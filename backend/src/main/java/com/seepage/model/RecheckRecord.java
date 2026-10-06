package com.seepage.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 复查记录：必须基于一次完工后的有效降雨，复查日期不得早于降雨日 */
@Entity
public class RecheckRecord extends BaseEntity {
    @ManyToOne(optional = false)
    public RepairTicket ticket;

    public String inspectorName;

    @Enumerated(EnumType.STRING)
    public RecheckResult result;

    @Column(length = 2000)
    public String content;

    /** 复查日期（模拟日期） */
    public LocalDate checkedAt;

    /** 本次复查依据的有效降雨 */
    @ManyToOne
    public RainEvent rainEvent;

    public LocalDateTime createdAt = LocalDateTime.now();
}
