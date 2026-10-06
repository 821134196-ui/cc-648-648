package com.leak.model;

/** 物业对住户异议的处理结果 */
public enum ObjectionOutcome {
    PENDING("待物业回复"),
    MAINTAIN("维持原结论"),
    REOPEN("重新安排处理");

    public final String label;

    ObjectionOutcome(String label) {
        this.label = label;
    }
}
