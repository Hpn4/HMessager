package com.hpn.hmessager.data.model;

import com.hpn.hmessager.domain.entity.keypair.SigningKeyPair;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@Getter
@Setter
@With
@NoArgsConstructor
@AllArgsConstructor
public class X3DHData {

    byte[] pubEK;

    X25519KeyPair IK;

    SigningKeyPair SK;

    int convId;

    boolean first;
}
