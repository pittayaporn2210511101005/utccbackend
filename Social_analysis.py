import pandas as pd
from sqlalchemy import create_engine
from openai import OpenAI

# ================================
# OLLAMA CLIENT
# ================================
client = OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama"
)

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
        return "neutral"


# ================================
# 1) Sentiment
# ================================
def detect_sentiment(text):
    prompt = f"""
    วิเคราะห์ sentiment ของข้อความต่อไปนี้:

    กฎ:
    - ถามเฉยๆ = neutral
    - ขอรีวิว = neutral
    - ข้อมูลทั่วไป = neutral
    - บ่น โวยวาย ไม่พอใจ = negative
    - ชื่นชม = positive

    ตอบ:
    - positive
    - neutral
    - negative

    ข้อความ: "{text}"
    """
    return ask_llm(prompt)


# ================================
# 2) NSFW / Toxic / Hate
# ================================
def detect_nsfw_llm(text):
    prompt = f"""
    วิเคราะห์ประเภทของข้อความต่อไปนี้:

    กฎ:
    - ถ้าเป็นคำถาม, ขอข้อมูล, ขอรีวิว ไม่มีคำหยาบ = normal
    - คำเชิงเพศหรือ 18+ = sexual หรือ pornographic
    - ด่า/หยาบคาย/ก้าวร้าว = abusive หรือ toxic
    - ดูถูก/เหยียด/ล้อเลียน = hate หรือ bully
    - ขู่ทำร้าย = threatening

    เลือกเพียง 1 label:
    sexual, pornographic, abusive, toxic, hate, bully,
    threatening, violent, normal

    ข้อความ: "{text}"
    """
    return ask_llm(prompt)


# ================================
# 3) Politeness
# ================================
def detect_politeness(text):
    prompt = f"""
    วิเคราะห์ระดับความสุภาพของข้อความนี้:

    กฎ:
    - คำถามทั่วไป, ขอรีวิว, หาเพื่อน, ชวนคุย = neutral
    - คำหยาบ ด่า ประชดแรง = impolite
    - มีครับ/ค่ะ/นะคะ/คะ/ครับ = polite
    - ไม่เข้าเกณฑ์ใดๆ = neutral

    ตอบ:
    polite
    neutral
    impolite

    ข้อความ: "{text}"
    """
    return ask_llm(prompt)


# ================================
# 4) FINAL LABEL
# ================================
def final_classification(text):
    s = detect_sentiment(text)
    n = detect_nsfw_llm(text)
    p = detect_politeness(text)

    # 18+
    if n in ["sexual", "pornographic"]:
        return "ล่อแหลม / 18+"

    # toxic
    if n in ["abusive", "toxic", "hate", "bully", "threatening", "violent"]:
        return "ด่า / ก้าวร้าว / เหยียด"

    # impolite
    if p == "impolite":
        return "หยาบคาย"

    # polite + positive
    if p == "polite" and s == "positive":
        return "สุภาพ-ชม"

    if s == "positive":
        return "ชม"

    if s == "negative":
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
# RUN ANALYSIS (with progress)
# ================================
print("⚙️ Running full AI analysis ...")

sentiments = []
nsfws = []
polites = []
finals = []

total = len(df)

for i, text in enumerate(df["text"], start=1):
    print(f"Analyzing {i}/{total}...")

    s = detect_sentiment(text)
    n = detect_nsfw_llm(text)
    p = detect_politeness(text)
    f = final_classification(text)

    sentiments.append(s)
    nsfws.append(n)
    polites.append(p)
    finals.append(f)

df["sentiment"] = sentiments
df["nsfw"] = nsfws
df["politeness"] = polites
df["final_label"] = finals


# ================================
# SAVE
# ================================
df.to_sql("social_analysis", con=engine, if_exists="replace", index=False)

print("🎉 DONE!")
print(f"💾 Saved {len(df)} rows into social_analysis")