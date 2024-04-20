package com.hpn.hmessager.converter;

import static com.hpn.hmessager.converter.DataConverter.intToByte;
import static com.hpn.hmessager.domain.crypto.KeyUtils.DH_PRIV_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.DH_PUB_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.SIGN_PRIV_KEY_SIZE;
import static com.hpn.hmessager.domain.crypto.KeyUtils.SIGN_PUB_KEY_SIZE;

import com.hpn.hmessager.data.model.user.Config;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.domain.crypto.KeyUtils;
import com.hpn.hmessager.domain.entity.keypair.CustomKeyPair;
import com.hpn.hmessager.domain.entity.keypair.SigningKeyPair;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;
import com.hpn.hmessager.domain.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

public class LocalUserConverter extends Converter<LocalUser> {

    public LocalUser createLocalUser() {
        try {
            X25519KeyPair idKey = KeyUtils.generateX25519KeyPair();
            SigningKeyPair signKey = KeyUtils.generateSigningKeyPair();

            LocalUser lo = new LocalUser(idKey, signKey, 0);

            lo.setConfig(new Config());

            return lo;
        } catch (GeneralSecurityException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public byte[] encode(LocalUser user) {
        // Create the plain data (4: number of conv, 4: port number, 4: server ip)
        int size = DH_PUB_KEY_SIZE + DH_PRIV_KEY_SIZE + SIGN_PUB_KEY_SIZE + SIGN_PRIV_KEY_SIZE + 4 + 4 + 4;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(size)){
            // IK
            CustomKeyPair keys = user.getIdentityKeys();

            bos.write(keys.getRawPublicKey());
            bos.write(keys.getRawPrivateKey());

            // ISK
            keys = user.getSigningKeys();

            bos.write(keys.getRawPublicKey());
            bos.write(keys.getRawPrivateKey());

            // Number of conversations
            bos.write(intToByte(user.getNumberOfConversations()));

            // Config
            bos.write(intToByte(user.getConfig().getPort()));
            bos.write(user.getConfig().getHost());

            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public LocalUser decode(byte[] plain, Object other) {
        if (plain == null) return null;

        try (HByteArrayInputStream bis = new HByteArrayInputStream(plain)) {
            Key pubIK = readKey(bis, DH_PUB);
            Key privIK = readKey(bis, DH_PRIV);
            X25519KeyPair idKey = new X25519KeyPair(pubIK, privIK);

            Key pubSK = readKey(bis, ED_PUB);
            Key privSK = readKey(bis, ED_PRIV);
            SigningKeyPair signKey = new SigningKeyPair(pubSK, privSK);

            int convCount = bis.readInt();

            Config config = new Config();
            config.setPort(bis.readInt());
            config.setHost(bis.readBytes(4));

            LocalUser lo = new LocalUser(idKey, signKey, convCount);
            lo.setConfig(config);

            return lo;
        } catch (GeneralSecurityException | IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
