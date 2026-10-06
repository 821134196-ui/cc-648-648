package com.leak.entity;

import com.leak.model.RecheckResult;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 雨后复查记录，挂在住户报修单上（复查人员回到该住户原位置检查）。 */
@Entity
@Table(name = "recheck_record")
public class RecheckRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Report report;

    @Column(nullable = false)
    public String inspectorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RecheckResult result;

    @Column(length = 1000)
    public String conclusion;

    /** 本次复查依据的有效降雨日期 */
    public LocalDate qualifyingRainDate;

    /** 有效降雨量（毫米） */
    public double qualifyingRainMm;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    @OneToMany(mappedBy = "recheck", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    public List<Photo> photos = new ArrayList<>();
}
