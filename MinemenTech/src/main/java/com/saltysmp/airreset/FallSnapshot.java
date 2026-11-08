package com.saltysmp.airreset;

public record FallSnapshot(double fallStartX, double fallStartY, double fallStartZ, float fallDistance) {

    public static FallSnapshot empty() {
        return new FallSnapshot(0, 0, 0, 0);
    }

    public boolean isValid() {
        return fallStartY != 0 || fallDistance > 0;
    }
}
