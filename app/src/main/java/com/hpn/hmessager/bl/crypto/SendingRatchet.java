package com.hpn.hmessager.bl.crypto;

import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.conversation.Message;
import com.hpn.hmessager.bl.io.StorageManager;

import java.security.GeneralSecurityException;

public class SendingRatchet extends Ratchet {

    private final MediaSender mediaSender;

    public SendingRatchet(Conversation conv) {
        super(conv);
        mediaSender = new MediaSender(this);
    }

    public MediaSender constructMediaSender(Message message) {
        mediaSender.clear();

        try {
            KeyUtils.ChainMessageK chainMessageK = KeyUtils.deriveCKandMK(chainKey);
            chainKey = chainMessageK.getChainKey();

            byte[] k = chainMessageK.getMessageKey();

            mediaSender.initSending(message, k);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }

        return mediaSender;
    }

    public byte[] constructMessage(Message message) {
        try {
            KeyUtils.ChainMessageK chainMessageK = KeyUtils.deriveCKandMK(chainKey);
            chainKey = chainMessageK.getChainKey();

            return constructMessage(message.constructByteArray(), 0, 0, chainMessageK.getMessageKey());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected byte[] constructMessage(byte[] plain, int fragId, int fragTot, byte[] key) {
        byte[] sendingRatchetKey = conv.getDHKeys().getRawPublicKey();
        byte[] metadata = new byte[METADATA_SIZE];

        // Build metadata
        System.arraycopy(StorageManager.intToByte(conv.getDestConvId()), 0, metadata, 0, 4);
        System.arraycopy(StorageManager.intToByte(fragId), 0, metadata, 4, 4);
        System.arraycopy(StorageManager.intToByte(fragTot), 0, metadata, 8, 4);

        try {
            byte[] ciphered = KeyUtils.encrypt(key, plain);

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

    /*
     public byte[] constructMessage(byte[] plain) {
         byte[] sendingRatchetKey = conv.getDHKeys().getRawPublicKey();
         byte[] metadata = new byte[METADATA_SIZE];

         System.arraycopy(StorageManager.intToByte(conv.getDestConvId()), 0, metadata, 0, 4);

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
    */
}
