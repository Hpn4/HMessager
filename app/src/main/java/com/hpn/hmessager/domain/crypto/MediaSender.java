package com.hpn.hmessager.domain.crypto;

import com.hpn.hmessager.converter.MessageConverter;
import com.hpn.hmessager.data.model.Message;
import com.hpn.hmessager.data.model.message.MediaAttachment;

public class MediaSender {

    private final MessageConverter messageConverter = new MessageConverter();

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
        System.out.println("[MediaSender]: Init sending of: " + message);
        fragTot = (int) Math.ceil((double) message.getMediaAttachment().getSize() / Ratchet.MAX_MESSAGE_SIZE);

        mediaData = message.getMediaAttachment().readContent();
        System.out.println("[MediaSender]: Read all metadata: " + mediaData.length);
    }

    public byte[] getFirstFragment() {
        System.out.println("[MediaSender]: prepare first fragment");
        byte[] firstFragClear = messageConverter.encode(message, true);
        byte[] firstFrag = sendingRatchet.constructMessage(firstFragClear, 0, fragTot, key);

        System.out.println("[MediaSender]: init sending of " + message.getMediaAttachment().getSize() + "b in " + fragTot + "fragments");
        System.out.println("[MediaSender]: init sending of " + mediaData.length + "b in " + fragTot + "fragments");
        System.out.println("[MediaSender]: init sending of " + MediaAttachment.getSizeFromMeta(firstFragClear) + "b in " + fragTot + "fragments");

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
