package com.leak.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 模拟天气：一场降雨记录。是否“有效降雨”由降雨量与施工完成时间共同判定。 */
@Entity
@Table(name = "rain_record",
        uniqueConstraints = @UniqueConstraint(columnNames = "rainDate"))
public class RainRecord extends BaseEntity {

    @Column(nullable = false)
    public LocalDate rainDate;

    /** 降雨量（毫米） */
    @Column(nullable = false)
    public double mm;

    public String note;

    @Column(nullable = false)
    public LocalDateTime createdAt;
}
