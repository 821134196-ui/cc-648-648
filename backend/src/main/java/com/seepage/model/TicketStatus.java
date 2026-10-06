package com.seepage.model;

/** 工单状态机：待受理 → 维修中 → 待复查 → 复查通过；复查不通过回到维修中；住户异议进入异议处理中 */
public enum TicketStatus {
    REPORTED,        // 待受理
    IN_REPAIR,       // 维修中
    PENDING_RECHECK, // 待复查（等待有效降雨）
    VERIFIED,        // 复查通过
    DISPUTED         // 异议处理中
}
