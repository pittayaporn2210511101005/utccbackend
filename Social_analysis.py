import pandas as pd
from sqlalchemy import create_engine
from openai import OpenAI
import requests

# ================================
# OLLAMA CLIENT
# ================================
client = OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama"
)

# ================================
# LOAD CUSTOM KEYWORDS FROM BACKEND
# ================================
def load_custom_keywords():
    url = "http://localhost:8082/custom-keywords/all"
    data = requests.get(url).json()
    return {item["keyword"]: item["sentiment"] for item in data}

def apply_custom_dict(text, ai_sentiment, custom_dict):
    for word, sent in custom_dict.items():
        if word in text:
            return sent  # Override sentiment by user-configured label
    return ai_sentiment

print("🧠 Loading LLM ...")

# ================================
# HELPER: call LLM
# ================================
def ask_llm(prompt):
    try:
        res = client.chat.completions.create(
            model="llama3",
            messages=[{"role": "user", "content": prompt}],
            temperature=0
        )
        return res.choices[0].message.content.strip()
    except Exception as e:
        print("LLM ERROR:", e)
        return ""

# ================================
# Extract label from messy response
# ================================
def extract_label(text, choices, default):
    t = text.lower()
    for c in choices:
        if c.lower() in t:
            return c
    return default

# ================================
# 1) SENTIMENT
# ================================
def detect_sentiment(text):
    prompt = f"""
    วิเคราะห์ sentiment ของข้อความต่อไปนี้:
    ให้ตอบเป็นคำเดียว: positive, neutral, negative
    ข้อความ: "{text}"
    """
    raw = ask_llm(prompt)
    return extract_label(raw, ["positive", "neutral", "negative"], "neutral")

# ================================
# 2) NSFW
# ================================
def detect_nsfw_llm(text):
    prompt = f"""
    วิเคราะห์ประเภทข้อความนี้:

    เลือกเพียง 1 คำ:
    sexual, pornographic, abusive, toxic, hate,
    bully, threatening, violent, normal

    ข้อความ: "{text}"
    """
    raw = ask_llm(prompt)
    return extract_label(
        raw,
        ["sexual", "pornographic", "abusive", "toxic", "hate",
         "bully", "threatening", "violent", "normal"],
        "normal"
    )

# ================================
# 3) POLITENESS
# ================================
def detect_politeness(text):
    prompt = f"""
    วิเคราะห์ระดับความสุภาพ:

    ตอบ:
    polite
    neutral
    impolite

    ข้อความ: "{text}"
    """
    raw = ask_llm(prompt)
    return extract_label(raw, ["polite", "neutral", "impolite"], "neutral")

# ================================
# 4) FINAL LABEL
# ================================
def final_classification(sentiment, nsfw, politeness):
    if nsfw in ["sexual", "pornographic"]:
        return "ล่อแหลม / 18+"

    if nsfw in ["abusive", "toxic", "hate", "bully", "threatening", "violent"]:
        return "ด่า / ก้าวร้าว / เหยียด"

    if politeness == "impolite":
        return "หยาบคาย"

    if politeness == "polite" and sentiment == "positive":
        return "สุภาพ-ชม"

    if sentiment == "positive":
        return "ชม"

    if sentiment == "negative":
        return "บ่น / ตำหนิ"

    return "ปกติ"

# ================================
# DATABASE
# ================================
engine = create_engine(
    "mysql+pymysql://root:@localhost/backendutcc?charset=utf8mb4"
)

print("📥 Loading data from database ...")

df_tw = pd.read_sql("SELECT id, text, created_at FROM tweet", engine)
df_tw["platform"] = "twitter"

df_pt = pd.read_sql(
    "SELECT id, title AS text, post_time AS created_at FROM pantip_post", engine)
df_pt["platform"] = "pantip_post"

df_pc = pd.read_sql(
    "SELECT id, text, commented_at AS created_at FROM pantip_comment", engine)
df_pc["platform"] = "pantip_comment"

df = pd.concat([df_tw, df_pt, df_pc], ignore_index=True)
print(f"✅ รวมทั้งหมด {len(df)} ข้อความ")

# ================================
# FACULTY DETECTION
# ================================
faculty_keywords = {
    "บัญชี": ["บัญชี","การเงิน"],
    "การตลาด": ["การตลาด"],
    "นิเทศศาสตร์": ["นิเทศ", "สื่อสาร", "event"],
    "ท่องเที่ยว": ["ท่องเที่ยว", "ธุรกิจการบิน", "การบิน"],
    "บริหารธุรกิจ": ["บริหาร", "จัดการ", "ธุรกิจ","การจัดการ"],
    "เศรษฐศาสตร์": ["เศรษฐ"],
    "โลจิสติกส์": ["โลจิส"],
    "มนุษย์ศาสตร์": ["มนุษย์", "อิ้ง","มนุษยศาสตร์","เกาหลี","จีน","epic"],
    "ทุนมหาลัย": ["ทุน"],
    "กยส": ["กยศ", "กู้"],
    "วิทยาศาสตร์": ["วิทคอม", "เทคโนโลยี", "อาหาร"],
    "ศูนย์บริการ": ["ติดต่อ", "สำนัก", "บริการ", "ระบบ"],
}

def detect_faculty(text):
    t = text.lower()
    for f, keys in faculty_keywords.items():
        if any(k.lower() in t for k in keys):
            return f
    return "มหาวิทยาลัยโดยรวม"

df["faculty"] = df["text"].apply(detect_faculty)

# ================================
# LOAD custom_dict HERE (สำคัญมาก!!)
# ================================
custom_dict = load_custom_keywords()
print("📌 Loaded custom keywords:", custom_dict)

# ================================
# RUN ANALYSIS
# ================================
print("⚙️ Running AI analysis ...")

sentiments = []
nsfws = []
polites = []
finals = []

total = len(df)

for i, text in enumerate(df["text"], start=1):
    print(f"Analyzing {i}/{total}...")

    # AI sentiment
    ai_sent = detect_sentiment(text)

    # Override sentiment by custom keywords
    final_sent = apply_custom_dict(text, ai_sent, custom_dict)

    n = detect_nsfw_llm(text)
    p = detect_politeness(text)
    f = final_classification(final_sent, n, p)

    sentiments.append(final_sent)
    nsfws.append(n)
    polites.append(p)
    finals.append(f)

df["sentiment"] = sentiments
df["nsfw"] = nsfws
df["politeness"] = polites
df["final_label"] = finals

# ================================
# SAVE TO DB
# ================================
df.to_sql("social_analysis", con=engine, if_exists="replace", index=False)

print("🎉 DONE!")
print(f"💾 Saved {len(df)} rows into social_analysis")
