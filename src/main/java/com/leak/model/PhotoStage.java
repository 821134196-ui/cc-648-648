package com.leak.model;

/** 照片所属阶段，详情页按 报修→施工→复查 分组排列 */
public enum PhotoStage {
    REPORT("报修"),
    REPAIR("施工"),
    RECHECK("复查"),
    OBJECTION("异议");

    public final String label;

    PhotoStage(String label) {
        this.label = label;
    }
}
