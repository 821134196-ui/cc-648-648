package com.leak.model;

/** 点位整体状态（同一物理渗水点可能被多户关联报修、多次复发） */
public enum SpotStatus {
    OPEN("待处理"),
    IN_PROGRESS("处理中"),
    RESOLVED("已解决");

    public final String label;

    SpotStatus(String label) {
        this.label = label;
    }
}
