package com.leak.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 维修员的施工记录，挂在物理点位上（同一点位的关联报修共享一次施工）。 */
@Entity
@Table(name = "repair_record")
public class RepairRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public LeakSpot spot;

    @Column(nullable = false)
    public String workerName;

    @Column(length = 1000)
    public String summary;

    /** 施工完成时间 */
    @Column(nullable = false)
    public LocalDateTime happenedAt;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    @OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    public List<Photo> photos = new ArrayList<>();
}
