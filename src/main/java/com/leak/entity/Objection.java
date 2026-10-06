package com.leak.entity;

import com.leak.model.ObjectionOutcome;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 住户异议及物业回复。 */
@Entity
@Table(name = "objection")
public class Objection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Report report;

    @Column(nullable = false, length = 1000)
    public String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ObjectionOutcome outcome;

    @Column(length = 1000)
    public String replyNote;

    public String repliedBy;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    public LocalDateTime repliedAt;

    @OneToMany(mappedBy = "objection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    public List<Photo> photos = new ArrayList<>();
}
