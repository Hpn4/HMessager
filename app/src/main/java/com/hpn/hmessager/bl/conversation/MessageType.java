package com.hpn.hmessager.bl.conversation;

public enum MessageType {

    TEXT((byte) 0x01),
    IMAGE((byte) 0x02),
    AUDIO((byte) 0x04),
    VIDEO((byte) 0x08),

    UNKNOWN((byte) 0x00);

    byte code;

    MessageType(byte b) {
        code = b;
    }

    public byte getCode() {
        return code;
    }
}
