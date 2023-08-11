package com.hpn.hmessager.bl.crypto;

import com.hpn.hmessager.bl.conversation.Conversation;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class ReceivingRatchet extends Ratchet {

    private static final byte[] RECEIVING_INFO = "receiving".getBytes();

    private static final byte[] SENDING_INFO = "sending".getBytes();

    public ReceivingRatchet(Conversation conversation) {
        super(conversation);
    }


    // Return wheter the message have been successfully received ad decrypted
    public byte[] receiveMessage(byte[] msg) {
        try {
            byte[] ciphertext = new byte[msg.length - HEADER_ROW_SIZE * 4 - METADATA_SIZE];

            System.arraycopy(msg, HEADER_ROW_SIZE * METADATA_ROW_I + METADATA_SIZE, ciphertext, 0, ciphertext.length);

            if (!checkSignature(msg, ciphertext)) {
                System.out.println("Signature check failed");
                return null;
            }

            byte[] pubRatchetKey = new byte[32];

            System.arraycopy(msg, HEADER_ROW_SIZE * RATCHET_KEY_ROW_I, pubRatchetKey, 0, HEADER_ROW_SIZE);

            if (!Arrays.equals(conv.getRatchetKey(), pubRatchetKey)) {
                byte[] tmp;

                /*
                 * RECEIVING RATCHET PART
                 */
                tmp = KeyUtils.diffieHellman(conv.getDHKeys().getPrivateKey(), pubRatchetKey, true); // DH shared secret
                tmp = KeyUtils.deriveRootKey(conv.getRootKey(), tmp, RECEIVING_INFO); // Root key for receiving ratchet

                chainKey = tmp.clone();

                /*
                 * SENDING RATCHET PART
                 */
                X25519KeyPair newDHKeys = KeyUtils.generateX25519KeyPair();

                tmp = KeyUtils.diffieHellman(newDHKeys.getPrivateKey(), pubRatchetKey, true); // DH shared secret
                tmp = KeyUtils.deriveRootKey(chainKey, tmp, SENDING_INFO); // Root key for sending ratchet

                conv.getSendingRatchet().setChainKey(tmp.clone());

                conv.updateRatchetKey(pubRatchetKey);
                conv.setDHKeys(newDHKeys);
                conv.setRootKey(tmp.clone());
            }

            // Derive the chain key of our ratchet to create next chain key and associated message key
            KeyUtils.ChainMessageK chainMessageK = KeyUtils.deriveCKandMK(chainKey);

            chainKey = chainMessageK.getChainKey(); // Update chain key

            return KeyUtils.decrypt(chainMessageK.getMessageKey(), ciphertext);
        } catch (GeneralSecurityException e) {
            // An error can occur when:
            // - If the signature/message is not encrypted with the right key.
            // - Error in diffieHellman function.
            // - If an algorithm is not supported/found.

            e.printStackTrace();
            return null;
        }
    }

    private boolean checkSignature(byte[] msg, byte[] cipher) throws GeneralSecurityException {
        byte[] mac = new byte[MAC_SIZE];

        System.arraycopy(msg, 0, mac, 0, MAC_SIZE);

        return KeyUtils.verify(cipher, mac, conv.getRemoteUser().getSigningKey());
    }
}
