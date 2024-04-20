package com.hpn.hmessager.domain.entity.keypair;

import org.bouncycastle.jcajce.interfaces.EdDSAPrivateKey;
import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import lombok.Getter;

@Getter
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

    public byte[] getRawPublicKey() {
        return publicKey.getPointEncoding();
    }

    public byte[] getRawPrivateKey() {
        return privateKey.getEncoded();
    }
}
