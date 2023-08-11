package com.hpn.hmessager.bl.crypto;

import org.bouncycastle.jcajce.interfaces.EdDSAPrivateKey;
import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SigningKeyPair implements CustomKeyPair {

    private final EdDSAPublicKey publicKey;

    private final EdDSAPrivateKey privateKey;

    public SigningKeyPair(Key pubKey, Key privKey) {
        this((PublicKey) pubKey, (PrivateKey) privKey);
    }

    public SigningKeyPair(PublicKey pubkey, PrivateKey privkey) {
        publicKey = (EdDSAPublicKey) pubkey;
        privateKey = (EdDSAPrivateKey) privkey;
    }

    public EdDSAPublicKey getPublicKey() {
        return publicKey;
    }

    public EdDSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public byte[] getRawPublicKey() {
        return publicKey.getPointEncoding();
    }

    public byte[] getRawPrivateKey() {
        return privateKey.getEncoded();
    }
}
