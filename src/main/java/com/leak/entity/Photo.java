package com.leak.entity;

import com.leak.model.PhotoStage;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 本地存储的照片元数据，文件保存在 leak.storage.photo-dir 目录。 */
@Entity
@Table(name = "photo")
public class Photo extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PhotoStage stage;

    /** 磁盘文件名（UUID + 扩展名） */
    @Column(nullable = false)
    public String fileName;

    @Column(nullable = false)
    public String contentType;

    public String caption;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    public Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    public RepairRecord repair;

    @ManyToOne(fetch = FetchType.LAZY)
    public RecheckRecord recheck;

    @ManyToOne(fetch = FetchType.LAZY)
    public Objection objection;
}
