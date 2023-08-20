package com.hpn.hmessager.bl.crypto;

import com.hpn.hmessager.bl.conversation.Message;

public class MediaSender {

    private byte[] key;

    private int fragId;

    private int fragTot;

    private Message message;

    private final SendingRatchet sendingRatchet;

    public MediaSender(SendingRatchet sendingRatchet) {
        this.sendingRatchet = sendingRatchet;
    }

    public void initSending(Message message, byte[] key) {
        this.message = message;
        this.key = key;
        fragId = 1;
        fragTot = (int) Math.ceil((double) message.getMediaAttachment().getSize() / Ratchet.MAX_MESSAGE_SIZE);
    }

    public byte[] getFirstFragment() {
        return sendingRatchet.constructMessage(message.constructByteArray(), 0, fragTot, key);
    }

    public byte[] getNextFragment() {
        int offset = (fragId - 1) * Ratchet.MAX_MESSAGE_SIZE;
        int size = Math.min(Ratchet.MAX_MESSAGE_SIZE, (int) message.getMediaAttachment().getSize() - offset);

        byte[] fragment = new byte[size];
        System.arraycopy(message.getMediaAttachment().getData(), offset, fragment, 0, size);

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
