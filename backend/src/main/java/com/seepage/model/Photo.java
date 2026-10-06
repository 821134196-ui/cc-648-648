package com.seepage.model;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/** 照片：按工单 + 阶段（报修/施工/复查）归档，文件存本地磁盘 */
@Entity
public class Photo extends BaseEntity {
    @ManyToOne(optional = false)
    public RepairTicket ticket;

    @Enumerated(EnumType.STRING)
    public Phase phase;

    public String fileName;
    public String label;
    public LocalDateTime uploadedAt = LocalDateTime.now();
}
