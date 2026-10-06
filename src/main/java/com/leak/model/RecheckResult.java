package com.leak.model;

/** 雨后复查结论 */
public enum RecheckResult {
    DRY("未见渗水"),
    LEAKING("仍有渗水");

    public final String label;

    RecheckResult(String label) {
        this.label = label;
    }
}
