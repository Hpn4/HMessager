package com.hpn.hmessager;

import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.domain.crypto.KeyUtils;
import com.hpn.hmessager.domain.crypto.X3DH;
import com.hpn.hmessager.domain.entity.keypair.SigningKeyPair;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;

import org.junit.Assert;

import java.util.Arrays;

public class CryptoTest {

    @org.junit.Test
    public void X3DH() throws Exception {
        X25519KeyPair idA = KeyUtils.generateX25519KeyPair();
        X25519KeyPair ekA = KeyUtils.generateX25519KeyPair();
        SigningKeyPair skA = KeyUtils.generateSigningKeyPair();

        X25519KeyPair idB = KeyUtils.generateX25519KeyPair();
        X25519KeyPair ekB = KeyUtils.generateX25519KeyPair();
        SigningKeyPair skB = KeyUtils.generateSigningKeyPair();

        byte[] msA = KeyUtils.doX3DH(idA.getPrivateKey(), ekA.getPrivateKey(), idB.getRawPublicKey(), ekB.getRawPublicKey(), true);
        byte[] msB = KeyUtils.doX3DH(idB.getPrivateKey(), ekB.getPrivateKey(), idA.getRawPublicKey(), ekA.getRawPublicKey(), false);

        Assert.assertArrayEquals(msA, msB);
    }

    @org.junit.Test
    public void X3DHQR() throws Exception {
        X25519KeyPair idA = KeyUtils.generateX25519KeyPair();
        X25519KeyPair ekA = KeyUtils.generateX25519KeyPair();
        SigningKeyPair skA = KeyUtils.generateSigningKeyPair();
        LocalUser A = new LocalUser(idA, skA, 0);
        X3DH x3dhA = new X3DH(A, ekA);

        X25519KeyPair idB = KeyUtils.generateX25519KeyPair();
        X25519KeyPair ekB = KeyUtils.generateX25519KeyPair();
        SigningKeyPair skB = KeyUtils.generateSigningKeyPair();
        LocalUser B = new LocalUser(idB, skB, 0);
        X3DH x3dhB = new X3DH(B, ekB);

        String qrA = x3dhA.generateQrCode(true);
        String qrB = x3dhB.generateQrCode(false);

        Conversation convA = x3dhA.generateConversation(qrB, null);
        Conversation convB = x3dhB.generateConversation(qrA, null);

        byte[] msA = convA.getRootKey();
        byte[] msB = convB.getRootKey();

        System.out.println(Arrays.toString(msA));
        System.out.println(Arrays.toString(msB));

        Assert.assertArrayEquals(msA, msB);
    }
}
