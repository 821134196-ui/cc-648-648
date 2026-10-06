package com.leak.model;

/**
 * 报修单状态。
 * “待复查”期间用雨后条件资格区分：等待有效降雨 / 已具备复查条件，
 * 未出现有效降雨时任务一直保持待复查，不能在施工当天直接验证通过。
 */
public enum ReportStatus {
    PENDING_REPAIR("待维修"),
    PENDING_RECHECK("待复查"),
    VERIFIED("已验证修复"),
    OBJECTIONED("异议处理中");

    public final String label;

    ReportStatus(String label) {
        this.label = label;
    }
}
