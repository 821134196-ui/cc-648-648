package com.leak.entity;

import com.leak.model.ReportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 住户报修单。一个物理点位可以被多户分别报修（关联处理、记录各自保留）；
 * 同一户在修复后再次报修同一位置时，recurrenceNo 递增并记为原问题复发。
 */
@Entity
@Table(name = "report")
public class Report extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public LeakSpot spot;

    /** 房号，如 2单元502 */
    @Column(nullable = false)
    public String room;

    /** 报修住户姓名 */
    @Column(nullable = false)
    public String residentName;

    public String contact;

    @Column(length = 1000)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ReportStatus status;

    /** 该住户在该点位的第几次报修，1 = 首次，2+ = 复发 */
    @Column(nullable = false)
    public int recurrenceNo;

    /** 复发报修指向本户上一张报修单 */
    @ManyToOne(fetch = FetchType.LAZY)
    public Report recurrenceOf;

    /** 是否为点位曾验证修复后再次漏水（原问题复发，重新打开点位） */
    @Column(nullable = false)
    public boolean reopenedAfterResolved;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    /** 进入“待雨后复查”的时间（最近一次施工完成时间点） */
    public LocalDateTime awaitingSince;
}
