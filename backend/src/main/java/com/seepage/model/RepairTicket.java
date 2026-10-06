package com.seepage.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/** 渗水报修工单：一户居民的一条记录，关联到渗水点位；复发工单通过 recurrenceOf 指向原工单 */
@Entity
public class RepairTicket extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public LeakPoint point;

    public String building;
    public String facade;
    public String room;
    public String residentName;
    public String phone;

    @Column(length = 2000)
    public String description;

    @Enumerated(EnumType.STRING)
    public TicketStatus status = TicketStatus.REPORTED;

    /** 复发报修时指向原工单 */
    @ManyToOne(fetch = FetchType.EAGER)
    public RepairTicket recurrenceOf;

    public LocalDateTime createdAt = LocalDateTime.now();
}
