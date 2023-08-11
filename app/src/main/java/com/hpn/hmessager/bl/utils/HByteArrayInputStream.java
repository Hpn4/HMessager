package com.hpn.hmessager.bl.utils;

import com.hpn.hmessager.bl.io.StorageManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class HByteArrayInputStream extends ByteArrayInputStream {

    public HByteArrayInputStream(byte[] input) {
        super(input);
    }

    public byte[] readBytes(int n) {
        byte[] tmp = new byte[n];

        read(tmp, 0, n);

        return tmp;
    }

    public byte[] readRemainingBytes() {
        return readBytes(available());
    }

    public String readString(int length) {
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    public int readInt() {
        return StorageManager.byteToInt(readBytes(4), 0);
    }

    public Date readDate() {
        return StorageManager.byteToDate(readBytes(8), 0);
    }
}
