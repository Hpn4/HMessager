package com.hpn.hmessager.domain.crypto;

import static com.hpn.hmessager.converter.DataConverter.byteToInt;

import com.hpn.hmessager.data.model.Conversation;

import lombok.Getter;
import lombok.Setter;


/**
 * Data format:
 *                                       32 bytes
 * /----------------------------------------------------------------------------------------\
 * |                                   MAC (2 * 32)                                         |
 * |                                                                                        |
 * |----------------------------------------------------------------------------------------|
 * |                                      ID Key                                            |
 * |----------------------------------------------------------------------------------------|
 * |                                      DH Key                                            |
 * |----------------------------------------------------------------------------------------|
 * |  ConvID (4)  |  fragID (4)  | fragCount (4)  |                                         |
 * |----------------------------------------------/                                         |
 * |                                       Data                                             |
 * |                                                                                        |
 * \----------------------------------------------------------------------------------------/
 * Two parts:
 * - Header (clear): MAC (64B) + ID Key (32B) + DH Key (32B) = 128B
 * - Message (encrypted): Metadata + Data
 *      - Metadata: ConvID (4B) + fragID (4B) + fragCount (4B)
 *      - Data
 * Where:
 * - MAC (64B) is generated by hashing Enc(Metadata + Data) and then sign it
 * - ID Key (32B): is the identifier of the user we want to send the message (used by the server)
 * - DH Key (32B): is the public DH key of the remote user
 * - ConvID (4B): is an identifier of the conversation (used to redirect to the correct conv)
 * - fragID (4B): is used for media sending. Media are split in multiple fragment.
 * - fragCount (4B): the total number of fragment in order to send the media.
 */
public abstract class Ratchet {

    protected static final int MAX_MESSAGE_SIZE = 512 * 1024; // 512 KB

    protected static final short HEADER_ROW_SIZE = 32;

    protected static final short RATCHET_KEY_ROW_I = 3;

    protected static final short METADATA_ROW_I = 4;

    protected static final short METADATA_SIZE = 12; // 4 byte for ConvId, 4 for fragId, 4 for fragCount

    protected static final int MAC_SIZE = 2 * HEADER_ROW_SIZE;

    @Getter
    @Setter
    protected byte[] chainKey;

    protected Conversation conv;

    public Ratchet(Conversation conversation) {
        this.conv = conversation;
    }

    public static int getConvIdFromHeader(byte[] msg) {
        return byteToInt(msg, HEADER_ROW_SIZE * METADATA_ROW_I);
    }

    // Write and read from file
}
