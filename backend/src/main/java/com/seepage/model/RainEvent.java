package com.seepage.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDate;

/** 本地模拟的降雨事件；雨量达到阈值即视为“有效降雨”，触发可复查提醒 */
@Entity
public class RainEvent extends BaseEntity {
    @Column(unique = true)
    public LocalDate date;

    public double rainfallMm;

    public String note;
}
