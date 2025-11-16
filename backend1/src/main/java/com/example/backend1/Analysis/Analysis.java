package com.example.backend1.Analysis;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "social_analysis")
@Data
public class Analysis {

    @Id
    private String id;

    private String text;

    @Column(name = "created_at")
    private String createdAt;

    private String platform;

    private String faculty;

    private String sentiment;

    private String nsfw;

    private String politeness;

    @Column(name = "final_label")
    private String finalLabel;
}
