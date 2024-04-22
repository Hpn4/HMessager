package com.hpn.hmessager.converter;

import static com.hpn.hmessager.domain.crypto.KeyUtils.DH_PRIV_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.DH_PUB_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.SIGN_PRIV_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.SIGN_PUB_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.getPrivKeyFromEd25519;
import static com.hpn.hmessager.domain.crypto.KeyUtils.getPrivKeyFromX25519;
import static com.hpn.hmessager.domain.crypto.KeyUtils.getPubKeyFromEd25519;
import static com.hpn.hmessager.domain.crypto.KeyUtils.getPubKeyFromX25519;

import com.hpn.hmessager.domain.utils.HByteArrayInputStream;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

public abstract class Converter<T> {

    protected static final int DH_PRIV = 0;
    protected static final int DH_PUB = 1;
    protected static final int ED_PRIV = 2;
    protected static final int ED_PUB = 3;

    private static final int[] KEYS_SIZE = {DH_PRIV_KEY_SIZE, DH_PUB_KEY_SIZE, SIGN_PRIV_KEY_SIZE, SIGN_PUB_KEY_SIZE};

    protected Key readKey(HByteArrayInputStream bis, int code) throws IOException, GeneralSecurityException {
        byte[] tmp = bis.readBytes(KEYS_SIZE[code]);

        Key key = null;
        switch (code) {
            case 0:
                key = getPrivKeyFromX25519(tmp);
                break;
            case 1:
                key = getPubKeyFromX25519(tmp, true);
                break;
            case 2:
                key = getPrivKeyFromEd25519(tmp);
                break;
            case 3:
                key = getPubKeyFromEd25519(tmp, true);
                break;
        }

        return key;
    }

    public byte[] encode(T t) { return encode(t, null); }

    public abstract byte[] encode(T t, Object other);

    public T decode(byte[] data) { return decode(data, null); }

    public abstract T decode(byte[] data, Object other);
}
