package com.hpn.hmessager.bl.cryptotry;

import com.hpn.hmessager.bl.crypto.KeyUtils;
import com.hpn.hmessager.bl.crypto.SigningKeyPair;
import com.hpn.hmessager.bl.crypto.X25519KeyPair;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoTest {

    static long time;

    public static void main(String[] args) {
        try {
            X3DH();

            X25519Reconstruction(10);

            diffieHellmanRaw(100);
            diffieHellmanEncoded(100);

            Ed25519Reconstruction(100);
            EdDSA(256);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertEquals(Object o1, Object o2) {
        if (o1.getClass() != o2.getClass())
            System.err.println("Assertion failed: " + o1 + " != " + o2);
        else if(!(o1 instanceof Object[] && o2 instanceof Object[] && Arrays.equals((Object[])o1, (Object[])o2)))
            System.err.println("Assertion failed: " + o1 + " != " + o2);
        if(!o1.equals(o2))
            System.err.println("Assertion failed: " + o1 + " != " + o2);
    }

    public static void start() {
        time = System.currentTimeMillis();
    }

    public static void passed(String msg, int it) {
        System.out.println("[" + it + "/" + it + "] " + msg + " passed in " + (System.currentTimeMillis() - time) + "ms");
    }

    public static void X3DH() throws GeneralSecurityException {
        start();

        // Alice
        X25519KeyPair aliceIKey = KeyUtils.generateX25519KeyPair();
        X25519KeyPair aliceEKey = KeyUtils.generateX25519KeyPair();

        // Bob
        X25519KeyPair bobIKey = KeyUtils.generateX25519KeyPair();
        X25519KeyPair bobEKey = KeyUtils.generateX25519KeyPair();

        byte[] aMS = KeyUtils.doX3DH(aliceIKey.getPrivateKey(), aliceEKey.getPrivateKey(), bobIKey.getRawPublicKey(), bobEKey.getRawPublicKey(), true);

        byte[] bMS = KeyUtils.doX3DH(bobIKey.getPrivateKey(), bobEKey.getPrivateKey(), aliceIKey.getRawPublicKey(), aliceEKey.getRawPublicKey(), false);

        if(!Arrays.equals(aMS, bMS))
            System.err.println("X3DH failed: " + aMS + " != " + bMS);

        passed("X3DH", 1);
    }

    public static void EdDSA(int iteration) throws GeneralSecurityException {
        start();
        for (int i = 0; i < iteration; ++i) {
            SigningKeyPair keyPair = KeyUtils.generateSigningKeyPair();

            byte[] message = new SecureRandom().generateSeed(iteration);

            byte[] signature = KeyUtils.sign(message, keyPair.getPrivateKey());
            boolean verified = KeyUtils.verify(message, signature, keyPair.getPublicKey());

            if(!verified)
                System.err.println("EdDSA failed: " + message + " != " + signature);
        }
        passed("EdDSA", iteration);
    }

    public static void Ed25519Reconstruction(int iteration) throws GeneralSecurityException {
        start();
        for (int i = 0; i < iteration; ++i) {
            SigningKeyPair keyPair = KeyUtils.generateSigningKeyPair();

            byte[] rawPublicKey = keyPair.getRawPublicKey();
            byte[] encodedPublicKey = keyPair.getPublicKey().getEncoded();
            byte[] rawPrivateKey = keyPair.getRawPrivateKey();

            PublicKey publicKey = KeyUtils.getPubKeyFromEd25519(rawPublicKey, true);
            PublicKey publicKey2 = KeyUtils.getPubKeyFromEd25519(encodedPublicKey, false);
            PrivateKey privateKey = KeyUtils.getPrivKeyFromEd25519(rawPrivateKey);

            if (!publicKey.equals(keyPair.getPublicKey()))
                System.err.println("Ed25519 failed: " + publicKey + " != " + keyPair.getPublicKey());

            if (!publicKey2.equals(keyPair.getPublicKey()))
                System.err.println("Ed25519 failed: " + publicKey2 + " != " + keyPair.getPublicKey());

            if (!privateKey.equals(keyPair.getPrivateKey()))
                System.err.println("Ed25519 failed: " + privateKey + " != " + keyPair.getPrivateKey());
        }
        passed("Ed25519 reconstruction", iteration);
    }

    public static void X25519Reconstruction(int iteration) throws GeneralSecurityException {
        start();
        for (int i = 0; i < iteration; ++i) {
            X25519KeyPair keyPair = KeyUtils.generateX25519KeyPair();

            byte[] rawPublicKey = keyPair.getRawPublicKey();
            byte[] encodedPublicKey = keyPair.getPublicKey().getEncoded();
            byte[] rawPrivateKey = keyPair.getRawPrivateKey();

            PublicKey publicKey = KeyUtils.getPubKeyFromX25519(rawPublicKey, true);
            PublicKey publicKey2 = KeyUtils.getPubKeyFromX25519(encodedPublicKey, false);
            PrivateKey privateKey = KeyUtils.getPrivKeyFromX25519(rawPrivateKey);

            if (!publicKey.equals(keyPair.getPublicKey()))
                System.err.println("X25519 failed: " + publicKey + " != " + keyPair.getPublicKey());

            if (!publicKey2.equals(keyPair.getPublicKey()))
                System.err.println("X25519 failed: " + publicKey2 + " != " + keyPair.getPublicKey());

            if (!privateKey.equals(keyPair.getPrivateKey()))
                System.err.println("X25519 failed: " + privateKey + " != " + keyPair.getPrivateKey());
        }
        passed("X25519 reconstruction", iteration);
    }

    public static void diffieHellmanRaw(int iteration) throws GeneralSecurityException {
        start();
        for (int i = 0; i < iteration; i++) {
            X25519KeyPair alice = KeyUtils.generateX25519KeyPair();
            X25519KeyPair bob = KeyUtils.generateX25519KeyPair();

            byte[] aliceShared = KeyUtils.diffieHellman(alice.getPrivateKey(), bob.getRawPublicKey(), true);
            byte[] bobShared = KeyUtils.diffieHellman(bob.getPrivateKey(), alice.getRawPublicKey(), true);

            if(!Arrays.equals(aliceShared, bobShared))
                System.err.println("Diffie-Hellman failed: " + aliceShared + " != " + bobShared);
        }
        passed("Diffie-Hellman raw", iteration);
    }

    public static void diffieHellmanEncoded(int iteration) throws GeneralSecurityException {
        start();
        for (int i = 0; i < iteration; i++) {
            X25519KeyPair alice = KeyUtils.generateX25519KeyPair();
            X25519KeyPair bob = KeyUtils.generateX25519KeyPair();

            byte[] aliceShared = KeyUtils.diffieHellman(alice.getPrivateKey(), bob.getPublicKey().getEncoded(), false);
            byte[] bobShared = KeyUtils.diffieHellman(bob.getPrivateKey(), alice.getPublicKey().getEncoded(), false);

            if(!Arrays.equals(aliceShared, bobShared))
                System.err.println("Diffie-Hellman failed: " + aliceShared + " != " + bobShared);
        }
        passed("Diffie-Hellman encoded", iteration);
    }
}
