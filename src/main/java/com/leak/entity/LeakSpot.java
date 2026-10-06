package com.leak.entity;

import com.leak.model.SpotStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 物理渗水点位：同一栋楼、同一立面、同一位置的渗水归为一个点位。
 * 多户报修同一位置时关联到同一个 LeakSpot，但各自的 Report 独立保留。
 */
@Entity
@Table(name = "leak_spot")
public class LeakSpot extends BaseEntity {

    /** 楼栋，如 3栋 */
    @Column(nullable = false)
    public String building;

    /** 立面，如 东立面 */
    @Column(nullable = false)
    public String facade;

    /** 点位位置描述，如 主卧窗上角 */
    @Column(nullable = false)
    public String location;

    /** building + facade + 归一化 location 组成的匹配键，用于自动关联同点位 */
    @Column(nullable = false, unique = true)
    public String spotKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SpotStatus status;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    public List<Report> reports = new ArrayList<>();

    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL)
    @OrderBy("happenedAt ASC")
    public List<RepairRecord> repairs = new ArrayList<>();
}
