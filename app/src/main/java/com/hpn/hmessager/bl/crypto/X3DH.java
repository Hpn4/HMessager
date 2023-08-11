package com.hpn.hmessager.bl.crypto;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.io.ConversationStorage;
import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.user.LocalUser;
import com.hpn.hmessager.bl.user.User;
import com.hpn.hmessager.bl.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
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

    public X3DH(LocalUser user, X25519KeyPair ekey) {
        this.user = user;
        this.ekey = ekey;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String generateQrCode(boolean first) {
        try {
            int size = KeyUtils.DH_PUB_KEY_SIZE * 2 + KeyUtils.SIGN_PUB_KEY_SIZE + 5;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(size);

            bos.write(ekey.getRawPublicKey()); // pubEK
            bos.write(user.getIdentityKeys().getRawPublicKey()); // pubIK
            bos.write(user.getSigningKeyPair().getRawPublicKey()); // pubSK
            bos.write(StorageManager.intToByte(user.getNumberOfConversations())); // convID
            bos.write(first ? 1 : 0);

            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Conversation generateConversation(String qrcode, StorageManager storageManager) {
        try {
            byte[] raw = Base64.getDecoder().decode(qrcode);
            HByteArrayInputStream bis = new HByteArrayInputStream(raw);

            byte[] pubEKRemote = bis.readBytes(KeyUtils.DH_PUB_KEY_SIZE); // Public ephemeral key from remote user
            byte[] pubIKRemote = bis.readBytes(KeyUtils.DH_PUB_KEY_SIZE); // Public identity key from remote user

            // Read keys
            Key pubIKRemoteKey = KeyUtils.getPubKeyFromX25519(pubIKRemote, true);
            Key pubSKRemoteKey = StorageManager.readKey(bis, StorageManager.ED_PUB);

            int convId = bis.readInt(); // Conversation id
            boolean first = bis.read() == 1; // Is it the first message of the conversation?

            // Create the master secret
            Conversation conv = new Conversation(user, new User(pubIKRemoteKey, pubSKRemoteKey));
            byte[] ms = KeyUtils.doX3DH(user.getIdentityKeys().getPrivateKey(), ekey.getPrivateKey(), pubIKRemote, pubEKRemote, first);

            conv.createConversation(pubEKRemote, ekey, ms, convId);
            conv.setConversationStorage(new ConversationStorage(conv, storageManager));

            storageManager.storeConversation(conv);
            storageManager.storeLocalUser(user);

            return conv;
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
