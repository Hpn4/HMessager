package com.hpn.hmessager.domain.crypto;

import static com.hpn.hmessager.converter.DataConverter.intToByte;

import com.hpn.hmessager.converter.MessageConverter;
import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.data.model.Message;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class SendingRatchet extends Ratchet {

    private final MessageConverter messageConverter = new MessageConverter();

    private final MediaSender mediaSender;

    public SendingRatchet(Conversation conv) {
        super(conv);
        mediaSender = new MediaSender(this);
    }

    public MediaSender constructMediaSender(Message message) {
        System.out.println("[SendingRatchet]: Setup media sender");
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

            byte[] data = messageConverter.encode(message, true);

            return constructMessage(data, 0, 0, chainMessageK.getMessageKey());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected byte[] constructMessage(byte[] plain, int fragId, int fragTot, byte[] key) {
        byte[] sendingRatchetKey = conv.getDhKeys().getRawPublicKey();
        byte[] metadata = new byte[METADATA_SIZE];

        System.out.println("[SendingRatchet]: KEY: " + Arrays.toString(key));

        // Build metadata
        System.arraycopy(intToByte(conv.getDestConvId()), 0, metadata, 0, 4);
        System.arraycopy(intToByte(fragId), 0, metadata, 4, 4);
        System.arraycopy(intToByte(fragTot), 0, metadata, 8, 4);

        try {
            byte[] ciphered = KeyUtils.encrypt(key, plain);

            byte[] dstid = conv.getRemoteUser().getRawIdentityKey();
            byte[] hmac = KeyUtils.sign(ciphered, conv.getLocalUser().getSigningKeys().getPrivateKey());

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
