package com.hpn.hmessager.bl.user;

import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey;
import org.bouncycastle.jcajce.interfaces.XDHPublicKey;

import java.security.Key;
import java.security.PublicKey;

public class User extends AUser {

    private final XDHPublicKey identityKey;

    private final EdDSAPublicKey signingKey;

    public User(Key idK, Key signK) {
        this((PublicKey) idK, (PublicKey) signK);
    }

    public User(PublicKey identityKey, PublicKey signingKey) {
        this.identityKey = (XDHPublicKey) identityKey;
        this.signingKey = (EdDSAPublicKey) signingKey;
    }

    public EdDSAPublicKey getSigningKey() {
        return signingKey;
    }

    public byte[] getRawIdentityKey() {
        return identityKey.getUEncoding();
    }

    public byte[] getRawSigningKey() {
        return signingKey.getPointEncoding();
    }

}
