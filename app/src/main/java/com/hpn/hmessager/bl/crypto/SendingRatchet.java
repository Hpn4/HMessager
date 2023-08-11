package com.hpn.hmessager.bl.crypto;

import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.io.StorageManager;

import java.security.GeneralSecurityException;

public class SendingRatchet extends Ratchet {

    public SendingRatchet(Conversation conv) {
        super(conv);
    }

    public byte[] constructMessage(byte[] plain) {
        byte[] sendingRatchetKey = conv.getDHKeys().getRawPublicKey();
        byte[] metadata = new byte[METADATA_SIZE]; //TODO metadata

        System.arraycopy(StorageManager.intToByte(conv.getConvId()), 0, metadata, 0, 4);

        try {
            KeyUtils.ChainMessageK chainMessageK = KeyUtils.deriveCKandMK(chainKey);
            chainKey = chainMessageK.getChainKey();

            byte[] ciphered = KeyUtils.encrypt(chainMessageK.getMessageKey(), plain);

            byte[] dstid = conv.getRemoteUser().getRawIdentityKey();
            byte[] hmac = KeyUtils.sign(ciphered, conv.getLocalUser().getSigningKeyPair().getPrivateKey());

            byte[] msg = new byte[HEADER_ROW_SIZE * 4 + METADATA_SIZE + ciphered.length];

            // Header construction: MAC, destination id, sending ratchet key, metadata, ciphertext
            System.arraycopy(hmac, 0, msg, 0, MAC_SIZE);
            System.arraycopy(dstid, 0, msg, HEADER_ROW_SIZE * 2, HEADER_ROW_SIZE);

            //System.out.println("Dstid: " + Base64);

            System.arraycopy(sendingRatchetKey, 0, msg, HEADER_ROW_SIZE * RATCHET_KEY_ROW_I, HEADER_ROW_SIZE);
            System.arraycopy(metadata, 0, msg, HEADER_ROW_SIZE * METADATA_ROW_I, METADATA_SIZE);
            System.arraycopy(ciphered, 0, msg, HEADER_ROW_SIZE * METADATA_ROW_I + METADATA_SIZE, ciphered.length);

            return msg;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return null;
    }
}
