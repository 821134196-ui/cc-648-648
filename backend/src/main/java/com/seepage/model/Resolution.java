package com.seepage.model;

/** 物业对异议的处理结论 */
public enum Resolution {
    KEEP_VERIFIED,  // 维持复查通过结论
    REOPEN_REPAIR,  // 重新维修
    RECHECK_AGAIN   // 安排再次复查
}
