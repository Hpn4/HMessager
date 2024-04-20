package com.hpn.hmessager.data.model.user;

import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey;
import org.bouncycastle.jcajce.interfaces.XDHPublicKey;

import java.security.Key;
import java.security.PublicKey;

import lombok.Getter;
import lombok.Setter;

public class User extends AUser {

    private final XDHPublicKey identityKey;

    @Getter
    private final EdDSAPublicKey signingKey;

    @Getter
    @Setter
    private String avatarUrl;

    public User(Key idK, Key signK) {
        this((PublicKey) idK, (PublicKey) signK);
    }

    public User(PublicKey identityKey, PublicKey signingKey) {
        this.identityKey = (XDHPublicKey) identityKey;
        this.signingKey = (EdDSAPublicKey) signingKey;
    }

    public byte[] getRawIdentityKey() {
        return identityKey.getUEncoding();
    }

    public byte[] getRawSigningKey() {
        return signingKey.getPointEncoding();
    }
}
