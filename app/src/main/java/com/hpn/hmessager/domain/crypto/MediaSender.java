package com.hpn.hmessager.domain.crypto;

import com.hpn.hmessager.data.model.Message;

public class MediaSender {

    private final SendingRatchet sendingRatchet;
    private byte[] key;
    private byte[] mediaData;

    private int fragId;
    private int fragTot;
    private Message message;

    public MediaSender(SendingRatchet sendingRatchet) {
        this.sendingRatchet = sendingRatchet;
    }

    public void initSending(Message message, byte[] key) {
        this.message = message;
        this.key = key;
        fragId = 0;
        fragTot = (int) Math.ceil((double) message.getMediaAttachment().getSize() / Ratchet.MAX_MESSAGE_SIZE);

        mediaData = message.getMediaAttachment().readContent();
    }

    public byte[] getFirstFragment() {
        byte[] tmpData = message.getDataBytes();
        byte[] firstFrag = sendingRatchet.constructMessage(message.constructByteArray(true), 0, fragTot, key);

        message.setData(tmpData);

        return firstFrag;
    }

    public byte[] getNextFragment() {
        int offset = fragId * Ratchet.MAX_MESSAGE_SIZE;
        int size = Math.min(Ratchet.MAX_MESSAGE_SIZE, mediaData.length - offset);

        byte[] fragment = new byte[size];
        System.arraycopy(mediaData, offset, fragment, 0, size);

        ++fragId;
        return sendingRatchet.constructMessage(fragment, fragId, fragTot, key);
    }

    public int getFragCount() {
        return fragTot;
    }

    public void clear() {
        message = null;
        key = null;
        fragId = 0;
        fragTot = 0;
    }

}
