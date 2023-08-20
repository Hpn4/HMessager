package com.hpn.hmessager.bl.conversation;

public enum MessageType {

    TEXT((byte) 0x01),

    ONLY_EMOJI((byte) 0x02),

    MEDIA((byte) 0x04);

    final byte code;

    MessageType(byte b) {
        code = b;
    }

    public byte getCode() {
        return code;
    }

    public boolean isText() {
        return this == TEXT || this == ONLY_EMOJI;
    }

    public static MessageType fromCode(byte code) {
        for (MessageType type : values())
            if (type.code == code)
                return type;

        return TEXT;
    }
}
