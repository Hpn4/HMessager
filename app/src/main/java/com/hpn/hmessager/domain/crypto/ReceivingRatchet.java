package com.hpn.hmessager.domain.crypto;

import static com.hpn.hmessager.converter.DataConverter.byteToInt;

import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class ReceivingRatchet extends Ratchet {

    private static final byte[] RECEIVING_INFO = "receiving".getBytes();

    private static final byte[] SENDING_INFO = "sending".getBytes();

    private final MediaReceiver mediaReceiver;

    public ReceivingRatchet(Conversation conversation) {
        super(conversation);
        mediaReceiver = new MediaReceiver();
    }

    // Return wheter the message have been successfully received ad decrypted
    public boolean receiveMessage(byte[] msg) {
        try {
            // Check the message integrity
            byte[] ciphertext = new byte[msg.length - HEADER_ROW_SIZE * METADATA_ROW_I - METADATA_SIZE];
            System.arraycopy(msg, HEADER_ROW_SIZE * METADATA_ROW_I + METADATA_SIZE, ciphertext, 0, ciphertext.length);

            if (!checkSignature(msg, ciphertext)) {
                System.out.println("[ReceivingRatchet]: Signature check failed");
                return false;
            }

            // Extract metadata
            int offset = HEADER_ROW_SIZE * METADATA_ROW_I;
            int fragId = byteToInt(msg, offset + 4);
            int fragTot = byteToInt(msg, offset + 8);

            // Check if the message as multiple fragments
            if (fragTot > 0 && fragId > 0) {
                System.out.println("[ReceivingRatchet]: Media " + fragId + "/" + fragTot + " fragments");
                mediaReceiver.receiveFragment(ciphertext);

                // If all fragments have been received, send the assembled media to the conversation
                if (mediaReceiver.isComplete(fragId)) {
                    System.out.println("[ReceivingRatchet]: Media received in " + fragTot + " fragments");
                    conv.receiveMedia(mediaReceiver.getFirstFragment(), mediaReceiver.getData());

                    mediaReceiver.clear();
                }

                return true;
            }

            byte[] messageKey = updateKey(msg);
            System.out.println("[ReceivingRatchet]: KEY: " + Arrays.toString(messageKey));
            byte[] deciphered = KeyUtils.decrypt(messageKey, ciphertext);

            if (fragTot > 0 && fragId == 0)
                mediaReceiver.initReceiving(messageKey, deciphered, fragTot);
            else conv.receiveMessage(deciphered);

            return true;
        } catch (GeneralSecurityException e) {
            // An error can occur when:
            // - If the signature/message is not encrypted with the right key.
            // - Error in diffieHellman function.
            // - If an algorithm is not supported/found.
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update the ratchet key and the chain key and return a message key.
     *
     * @param msg The message received.
     * @return The message key.
     */
    private byte[] updateKey(byte[] msg) throws GeneralSecurityException {
        byte[] pubRatchetKey = new byte[32];
        System.arraycopy(msg, HEADER_ROW_SIZE * RATCHET_KEY_ROW_I, pubRatchetKey, 0, HEADER_ROW_SIZE);

        // If ratchet key different, do DH and update ratchet key
        if (!Arrays.equals(conv.getRatchetKey(), pubRatchetKey)) {
            byte[] tmp;

            /*
             * RECEIVING RATCHET PART
             */
            tmp = KeyUtils.diffieHellman(conv.getDhKeys().getPrivateKey(), pubRatchetKey, true); // DH shared secret
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
            conv.setDhKeys(newDHKeys);
            conv.setRootKey(tmp.clone());
        }

        // Derive the chain key of our ratchet to create next chain key and associated message key
        KeyUtils.ChainMessageK chainMessageK = KeyUtils.deriveCKandMK(chainKey);

        chainKey = chainMessageK.getChainKey(); // Update chain key

        return chainMessageK.getMessageKey();
    }

    private boolean checkSignature(byte[] msg, byte[] cipher) throws GeneralSecurityException {
        byte[] mac = new byte[MAC_SIZE];

        System.arraycopy(msg, 0, mac, 0, MAC_SIZE);

        return KeyUtils.verify(cipher, mac, conv.getRemoteUser().getSigningKey());
    }
}
