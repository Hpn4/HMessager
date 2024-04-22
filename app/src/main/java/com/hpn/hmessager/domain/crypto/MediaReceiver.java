package com.hpn.hmessager.domain.crypto;

import com.hpn.hmessager.data.model.message.MediaAttachment;

import java.security.GeneralSecurityException;

import lombok.Getter;

public class MediaReceiver {

    private byte[] key;

    @Getter
    private byte[] firstFragment; // EG message

    @Getter
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

        long size = MediaAttachment.getSizeFromMeta(firstFragment);
        System.out.println("[MediaReceiver]: init receiving of " + size + "b in " + fragTot + "fragments");
        data = new byte[(int)size];
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

    public void clear() {
        key = null;
        firstFragment = null;
        data = null;
        offset = 0;
        fragTot = 0;
    }
}
