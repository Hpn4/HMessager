package com.hpn.hmessager.data.model.message;

public enum MediaType {

    GIF("image/gif", (byte) 0x01),

    IMAGE("image/", (byte) 0x02),

    AUDIO("audio/", (byte) 0x03),

    VIDEO("video/", (byte) 0x04),

    DOCUMENT("", (byte) 0x05);

    final String mimeType;

    final byte code;

    MediaType(String mimeType, byte b) {
        this.mimeType = mimeType;
        code = b;
    }

    public boolean isVisual() {
        return this == IMAGE || this == VIDEO;
    }

    public static MediaType fromMimeType(String mimeType) {
        if (mimeType == null) return DOCUMENT;

        for (MediaType type : values())
            if (mimeType.startsWith(type.mimeType))
                return type;

        return DOCUMENT;
    }

    public static MediaType fromCode(byte code) {
        for (MediaType type : values())
            if (type.code == code)
                return type;

        return DOCUMENT;
    }
}
