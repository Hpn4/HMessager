package com.hpn.hmessager.bl.user;

import com.hpn.hmessager.bl.crypto.SigningKeyPair;
import com.hpn.hmessager.bl.crypto.X25519KeyPair;

public class LocalUser extends AUser {

    private X25519KeyPair identityKeys;

    private SigningKeyPair signingKey;

    private int numberOfConversations;

    private Config config;

    public LocalUser(X25519KeyPair identityKeys, SigningKeyPair signingKey, int numberOfConversations) {
        this.identityKeys = identityKeys;
        this.signingKey = signingKey;
        this.numberOfConversations = numberOfConversations;
    }

    public SigningKeyPair getSigningKeyPair() {
        return signingKey;
    }

    public X25519KeyPair getIdentityKeys() {
        return identityKeys;
    }

    public int getNumberOfConversations() {
        return numberOfConversations;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void addConversation() {
        numberOfConversations++;
    }
}
