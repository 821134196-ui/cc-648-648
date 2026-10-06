package com.seepage.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/** 住户对复查结论提出的异议，以及物业的回复 */
@Entity
public class Dispute extends BaseEntity {
    @ManyToOne(optional = false)
    public RepairTicket ticket;

    public String residentName;

    @Column(length = 2000)
    public String content;

    @Enumerated(EnumType.STRING)
    public DisputeStatus status = DisputeStatus.OPEN;

    @Column(length = 2000)
    public String reply;

    @Enumerated(EnumType.STRING)
    public Resolution resolution;

    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime repliedAt;
}
