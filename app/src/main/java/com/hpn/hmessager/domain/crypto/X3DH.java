package com.hpn.hmessager.domain.crypto;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hpn.hmessager.converter.X3DHConverter;
import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.data.model.X3DHData;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.data.model.user.User;
import com.hpn.hmessager.data.repository.ConversationStorage;
import com.hpn.hmessager.data.repository.StorageManager;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;

import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * X3DH class
 * This class is used to generate the QR code for a conversation and to generate a conversation from a qrcode.
 * <p>
 * A QR code is a byte array with:
 * - 32 bytes for the public ephemeral key of the remote user (or us)
 * - 32 bytes for the public identity key of the remote user (or us)
 * - 32 bytes for the public signing key of the remote user (or us)
 * - 4 bytes for the conversation id of the remote user (or us)
 */
public class X3DH {

    private final LocalUser user;

    private final X25519KeyPair ekey;

    private final X3DHConverter x3DHConverter = new X3DHConverter();

    public X3DH(LocalUser user, X25519KeyPair ekey) {
        this.user = user;
        this.ekey = ekey;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String generateQrCode(boolean first) {
        X3DHData data = new X3DHData()
                .withPubEK(ekey.getRawPublicKey())
                .withSK(user.getSigningKeys())
                .withIK(user.getIdentityKeys())
                .withConvId(user.getNumberOfConversations())
                .withFirst(first);

        return Base64.getEncoder().encodeToString(x3DHConverter.encode(data));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Conversation generateConversation(String qrcode, StorageManager storageManager) {
        try {
            byte[] raw = Base64.getDecoder().decode(qrcode);

            X3DHData remote = x3DHConverter.decode(raw);

            // Setup user data
            User remoteUser = new User(remote.getIK().getPublicKey(), remote.getSK().getPublicKey());
            Conversation conv = new Conversation(user, remoteUser);

            // Create the master secret
            byte[] ms = KeyUtils.doX3DH(user.getIdentityKeys().getPrivateKey(), ekey.getPrivateKey(), remote.getIK().getRawPublicKey(), remote.getPubEK(), remote.isFirst());

            conv.createConversation(remote.getPubEK(), ekey, ms, remote.getConvId());

            if (storageManager != null)
            {
                conv.setConversationStorage(new ConversationStorage(conv, storageManager));
                storageManager.storeConversation(conv);
                storageManager.storeLocalUser(user);
            }

            return conv;
        } catch (GeneralSecurityException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }
}
