package com.hpn.hmessager.data.model.user;

import com.hpn.hmessager.domain.entity.keypair.SigningKeyPair;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;

import lombok.Getter;
import lombok.Setter;

@Getter
public class LocalUser extends AUser {

    private final X25519KeyPair identityKeys;

    private final SigningKeyPair signingKeys;

    private int numberOfConversations;

    @Setter
    private Config config;

    public LocalUser(X25519KeyPair identityKeys, SigningKeyPair signingKeys, int numberOfConversations) {
        this.identityKeys = identityKeys;
        this.signingKeys = signingKeys;
        this.numberOfConversations = numberOfConversations;
    }

    public void addConversation() {
        numberOfConversations++;
    }
}
