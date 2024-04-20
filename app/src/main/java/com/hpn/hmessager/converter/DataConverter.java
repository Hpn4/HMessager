package com.hpn.hmessager.converter;

import java.nio.ByteBuffer;
import java.util.Date;

public class DataConverter {

    // Static utils
    public static Date byteToDate(byte[] buff, int offset) {
        return new Date(byteToLong(buff, offset));
    }

    public static byte[] dateToByte(Date d) {
        if (d == null) return new byte[8];

        return longToByte(d.getTime());
    }

    public static int byteToInt(byte[] buff, int offset) {
        return ByteBuffer.wrap(buff, offset, 4).getInt();
    }

    public static byte[] intToByte(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public static long byteToLong(byte[] buff, int offset) {
        return ByteBuffer.wrap(buff, offset, 8).getLong();
    }

    public static byte[] longToByte(long l) {
        return ByteBuffer.allocate(8).putLong(l).array();
    }
}
