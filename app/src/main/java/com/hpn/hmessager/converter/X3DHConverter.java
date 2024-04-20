package com.hpn.hmessager.converter;

import static com.hpn.hmessager.converter.DataConverter.intToByte;

import com.hpn.hmessager.data.model.X3DHData;
import com.hpn.hmessager.domain.crypto.KeyUtils;
import com.hpn.hmessager.domain.entity.keypair.SigningKeyPair;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;
import com.hpn.hmessager.domain.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * X3DHConverter class
 * This class is used to encode/decode bytes for conversation QR code
 * <p>
 * A QR code is a byte array with:
 * - 32 bytes for the public ephemeral key of the remote user (or us)
 * - 32 bytes for the public identity key of the remote user (or us)
 * - 32 bytes for the public signing key of the remote user (or us)
 * - 4 bytes for the conversation id of the remote user (or us)
 */
public class X3DHConverter extends Converter<X3DHData> {
    @Override
    public byte[] encode(X3DHData x3DHData) {
        int size = KeyUtils.DH_PUB_KEY_SIZE * 2 + KeyUtils.SIGN_PUB_KEY_SIZE + 5;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(size)) {
            bos.write(x3DHData.getPubEK()); // pubEK
            bos.write(x3DHData.getIK().getRawPublicKey()); // pubIK
            bos.write(x3DHData.getSK().getRawPublicKey()); // pubSK
            bos.write(intToByte(x3DHData.getConvId())); // convID
            bos.write(x3DHData.isFirst() ? 1 : 0);

            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public X3DHData decode(byte[] data, Object other) {
        X3DHData x3DHData = new X3DHData();

        // Read keys
        try (HByteArrayInputStream bis = new HByteArrayInputStream(data)) {
            byte[] pubEKRemote = bis.readBytes(KeyUtils.DH_PUB_KEY_SIZE); // Public ephemeral key from remote user
            byte[] pubIKRemote = bis.readBytes(KeyUtils.DH_PUB_KEY_SIZE); // Public identity key from remote user

            Key pubIKRemoteKey = KeyUtils.getPubKeyFromX25519(pubIKRemote, true);
            Key pubSKRemoteKey = readKey(bis, ED_PUB);

            // Set keys
            x3DHData.setPubEK(pubEKRemote);
            x3DHData.setIK(new X25519KeyPair(pubIKRemoteKey, null));
            x3DHData.setSK(new SigningKeyPair(pubSKRemoteKey, null));

            // Set additional data
            x3DHData.setConvId(bis.readInt());
            x3DHData.setFirst(bis.read() == 1); // Is it the first message of the conversation?
        } catch (GeneralSecurityException | IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return x3DHData;
    }
}
