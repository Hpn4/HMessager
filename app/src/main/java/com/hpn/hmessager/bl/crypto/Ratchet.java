package com.hpn.hmessager.bl.crypto;

import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.io.StorageManager;

public abstract class Ratchet {

    protected static final int MAX_MESSAGE_KEYS = 100;

    protected static final short HEADER_ROW_SIZE = 32;

    protected static final short RATCHET_KEY_ROW_I = 3;

    protected static final short METADATA_ROW_I = 4;

    protected static final short METADATA_SIZE = 12; // 4 byte for ConvId, 4 for N and 4 for NP

    protected static final int MAC_SIZE = 2 * HEADER_ROW_SIZE;

    protected byte[] chainKey;

    protected Conversation conv;

    public Ratchet(Conversation conversation) {
        this.conv = conversation;
    }

    public static int getConvIdFromHeader(byte[] msg) {
        return StorageManager.byteToInt(msg, HEADER_ROW_SIZE * METADATA_ROW_I);
    }

    public void setChainKey(byte[] chainKey) {
        this.chainKey = chainKey;
    }

    public byte[] getChainKey() {
        return chainKey;
    }

    // Write and read from file
}
