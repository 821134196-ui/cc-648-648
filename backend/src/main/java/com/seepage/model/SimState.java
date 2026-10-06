package com.seepage.model;


import jakarta.persistence.Entity;
import java.time.LocalDate;

/** 模拟时钟：单行表（id=1），天气模拟推进“当前日期”，施工/复查时间均取该日期 */
@Entity
public class SimState extends BaseEntity {
    public LocalDate currentDate;
}
