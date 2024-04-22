package com.hpn.hmessager.converter;

import static com.hpn.hmessager.domain.crypto.KeyUtils.DH_PRIV_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.DH_PUB_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.SIGN_PUB_KEY_SIZE;

import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.data.model.user.User;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;
import com.hpn.hmessager.domain.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

public class ConversationConverter extends Converter<Conversation> {

    @Override
    public byte[] encode(Conversation c, Object other) {
        // Are stored:
        // - IK, ISK (public identity key and signing key of remote user) 64
        // - K, DH (public ratchet key and + and - for DH ratchet) 32 + 32 + 83
        // - RK, SRK, RRK (root key, sending ratchet key, receiving ratchet key) : 32 * 3
        int size = DH_PUB_KEY_SIZE * 3 + SIGN_PUB_KEY_SIZE + DH_PRIV_KEY_SIZE + 32 * 3;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(size);

        try {
            // Write IK and ISK
            User u = c.getRemoteUser();

            bos.write(u.getRawIdentityKey());
            bos.write(u.getRawSigningKey());

            // Write K an DH
            X25519KeyPair dh = c.getDhKeys();

            bos.write(c.getRatchetKey());
            bos.write(dh.getRawPublicKey());
            bos.write(dh.getRawPrivateKey());

            // Write RK, SRK and RRK
            bos.write(c.getRootKey());
            bos.write(c.getSendingRatchet().getChainKey());
            bos.write(c.getReceivingRatchet().getChainKey());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return bos.toByteArray();
    }

    @Override
    public Conversation decode(byte[] data, Object user) {
        Conversation conv;

        // Read IK and ISK
        try (HByteArrayInputStream bis = new HByteArrayInputStream(data)) {
            Key ik = readKey(bis, DH_PUB);
            Key isk = readKey(bis, ED_PUB);

            User remote = new User(ik, isk);
            LocalUser locale = (LocalUser) user;

            conv = new Conversation(locale, remote);

            // Read K (pub ratchet key) and DH (key pairs)
            conv.updateRatchetKey(bis.readBytes(DH_PUB_KEY_SIZE)); // K

            // DH
            Key dhPub = readKey(bis, DH_PUB);
            Key dhPriv = readKey(bis, DH_PRIV);

            conv.setDhKeys(new X25519KeyPair(dhPub, dhPriv));

            // Setup all ratchets (RK, RRK, SRK)
            conv.setRootKey(bis.readBytes(32)); // RK
            conv.getSendingRatchet().setChainKey(bis.readBytes(32)); // SRK
            conv.getReceivingRatchet().setChainKey(bis.readBytes(32)); // RRK
        } catch (GeneralSecurityException | IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return conv;
    }
}
