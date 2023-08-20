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

        int offset = 0;
        while (offset < n)
            offset += read(tmp, offset, n - offset);

        return tmp;
    }

    public byte[] readRemainingBytes() {
        return readBytes(available());
    }

    public String readString(int length) {
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    public String readString() {
        return readString(readInt());
    }

    public byte readByte() {
        return readBytes(1)[0];
    }

    public int readInt() {
        return StorageManager.byteToInt(readBytes(4), 0);
    }

    public long readLong() {
        return StorageManager.byteToLong(readBytes(8), 0);
    }

    public Date readDate() {
        return StorageManager.byteToDate(readBytes(8), 0);
    }
}
