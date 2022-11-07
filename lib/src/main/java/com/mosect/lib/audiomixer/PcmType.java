package com.mosect.lib.audiomixer;

public enum PcmType {

    BIT8(1),
    BIT16(2);

    private final int code;

    PcmType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
