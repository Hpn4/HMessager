package com.hpn.hmessager.bl.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class MediaReceiver {

    private byte[] key;

    private byte[] firstFragment; // EG message

    private ByteArrayOutputStream media;

    private int fragId;

    private int fragTot;

    public MediaReceiver() {

    }

    public void initReceiving(byte[] key, byte[] firstFragment, int fragTot) {
        this.key = key;
        fragId = 0;
        this.fragTot = fragTot;
        this.firstFragment = firstFragment;

        media = new ByteArrayOutputStream();
    }

    public void receiveFragment(byte[] fragment) {
        try {
            media.write(KeyUtils.decrypt(key, fragment));
        } catch(GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isComplete() {
        return fragId == fragTot;
    }

    public byte[] getMedia() {
        return media.toByteArray();
    }

    public byte[] getFirstFragment() {
        return firstFragment;
    }

    public void clear() {
        key = null;
        firstFragment = null;
        media = null;
        fragId = 0;
        fragTot = 0;
    }
}
