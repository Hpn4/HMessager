package com.hpn.hmessager.bl.crypto;

import org.bouncycastle.jcajce.interfaces.XDHPrivateKey;
import org.bouncycastle.jcajce.interfaces.XDHPublicKey;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

public class X25519KeyPair implements CustomKeyPair {

    private final XDHPublicKey publicKey;

    private final XDHPrivateKey privateKey;

    public X25519KeyPair(Key pubKey, Key privKey) {
        this((PublicKey) pubKey, (PrivateKey) privKey);
    }

    public X25519KeyPair(PublicKey pubKey, PrivateKey privKey) {
        publicKey = (XDHPublicKey) pubKey;
        privateKey = (XDHPrivateKey) privKey;
    }

    public XDHPublicKey getPublicKey() {
        return publicKey;
    }

    public XDHPrivateKey getPrivateKey() {
        return privateKey;
    }

    public byte[] getRawPublicKey() {
        return publicKey.getUEncoding();
    }

    public byte[] getRawPrivateKey() {
        return privateKey.getEncoded();
    }
}
