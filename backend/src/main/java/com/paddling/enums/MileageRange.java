package com.paddling.enums;

public enum MileageRange {
    SHORT("短距离", "0-5km"),
    MEDIUM("中距离", "5-20km"),
    LONG("长距离", "20km以上");
    
    private final String label;
    private final String description;
    
    MileageRange(String label, String description) {
        this.label = label;
        this.description = description;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getDescription() {
        return description;
    }
}