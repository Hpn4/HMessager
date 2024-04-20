package com.hpn.hmessager.domain.crypto;

import com.hpn.hmessager.data.model.message.MediaAttachment;

import java.security.GeneralSecurityException;

public class MediaReceiver {

    private byte[] key;

    private byte[] firstFragment; // EG message

    private byte[] data; // Media data

    private int offset;

    private int fragTot;

    public MediaReceiver() {
    }

    public void initReceiving(byte[] key, byte[] firstFragment, int fragTot) {
        this.key = key;
        this.fragTot = fragTot;
        this.firstFragment = firstFragment;
        offset = 0;

        data = new byte[MediaAttachment.getSizeFromMeta(firstFragment)];
    }

    public void receiveFragment(byte[] fragment) {
        try {
            byte[] decrypted = KeyUtils.decrypt(key, fragment);

            System.arraycopy(decrypted, 0, data, offset, decrypted.length);
            offset += decrypted.length;
        } catch(GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public boolean isComplete(int fragId) {
        return fragId == fragTot;
    }

    public byte[] getMedia() {
        return data;
    }

    public byte[] getFirstFragment() {
        return firstFragment;
    }

    public void clear() {
        key = null;
        firstFragment = null;
        data = null;
        offset = 0;
        fragTot = 0;
    }
}
