package com.seepage.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 施工记录：完工日期取模拟当前日期，是复查资格判断的起点 */
@Entity
public class ConstructionRecord extends BaseEntity {
    @ManyToOne(optional = false)
    public RepairTicket ticket;

    public String workerName;

    @Column(length = 2000)
    public String content;

    /** 完工日期（模拟日期） */
    public LocalDate completedAt;

    public LocalDateTime createdAt = LocalDateTime.now();
}
