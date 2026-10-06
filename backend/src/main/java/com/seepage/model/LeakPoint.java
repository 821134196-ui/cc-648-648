package com.seepage.model;


import jakarta.persistence.Entity;
import java.time.LocalDateTime;

/** 渗水点位：楼栋立面上的一处物理渗水位置，可关联多户报修工单 */
@Entity
public class LeakPoint extends BaseEntity {
    public String building;
    public String facade;
    public String note;
    public LocalDateTime createdAt = LocalDateTime.now();
}
