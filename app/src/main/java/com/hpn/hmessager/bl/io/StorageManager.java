package com.hpn.hmessager.bl.io;

import static com.hpn.hmessager.bl.crypto.KeyUtils.DH_PRIV_KEY_SIZE;
import static com.hpn.hmessager.bl.crypto.KeyUtils.DH_PUB_KEY_SIZE;
import static com.hpn.hmessager.bl.crypto.KeyUtils.SIGN_PRIV_KEY_SIZE;
import static com.hpn.hmessager.bl.crypto.KeyUtils.SIGN_PUB_KEY_SIZE;
import static com.hpn.hmessager.bl.crypto.KeyUtils.getPrivKeyFromEd25519;
import static com.hpn.hmessager.bl.crypto.KeyUtils.getPrivKeyFromX25519;
import static com.hpn.hmessager.bl.crypto.KeyUtils.getPubKeyFromEd25519;
import static com.hpn.hmessager.bl.crypto.KeyUtils.getPubKeyFromX25519;

import android.content.Context;
import android.content.Intent;

import com.hpn.hmessager.bl.conversation.ConvMetadata;
import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.conversation.Conversations;
import com.hpn.hmessager.bl.crypto.CustomKeyPair;
import com.hpn.hmessager.bl.crypto.KeyUtils;
import com.hpn.hmessager.bl.crypto.SigningKeyPair;
import com.hpn.hmessager.bl.crypto.X25519KeyPair;
import com.hpn.hmessager.bl.user.Config;
import com.hpn.hmessager.bl.user.LocalUser;
import com.hpn.hmessager.bl.user.User;
import com.hpn.hmessager.bl.utils.HByteArrayInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

public class StorageManager {

    // Constants for key size and type
    public static final int DH_PRIV = 0;
    public static final int DH_PUB = 1;
    public static final int ED_PRIV = 2;
    public static final int ED_PUB = 3;

    private static final int[] KEYS_SIZE = {DH_PRIV_KEY_SIZE, DH_PUB_KEY_SIZE, SIGN_PRIV_KEY_SIZE, SIGN_PUB_KEY_SIZE};


    // Constants for file name
    private static final String USER_FILE_NAME = "user.hm";
    private static final String CONV_KEYS_NAME = "keys.hm";
    private static final String CONV_MSG_NAME = "msg.hm";
    private static final String CONV_META_NAME = "meta.hm";

    private final File parentDir;
    private byte[] rootKey;

    public StorageManager(Context context) {
        parentDir = context.getFilesDir();
    }

    public StorageManager(Context context, Intent intent) {
        parentDir = context.getFilesDir();
        rootKey = intent.getByteArrayExtra("pass");
    }

    // Static utils
    public static Date byteToDate(byte[] buff, int offset) {
        return new Date(byteToLong(buff, offset));
    }

    public static byte[] dateToByte(Date d) {
        if (d == null)
            return new byte[8];

        return longToByte(d.getTime());
    }

    public static int byteToInt(byte[] buff, int offset) {
        return ByteBuffer.wrap(buff, offset, 4).getInt();
    }

    public static byte[] intToByte(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public static long byteToLong(byte[] buff, int offset) {
        return ByteBuffer.wrap(buff, offset, 8).getLong();
    }

    public static byte[] longToByte(long l) {
        return ByteBuffer.allocate(8).putLong(l).array();
    }

    public static Key readKey(HByteArrayInputStream bis, int code) throws IOException, GeneralSecurityException {
        byte[] tmp = bis.readBytes(KEYS_SIZE[code]);

        Key key = null;
        switch (code) {
            case 0:
                key = getPrivKeyFromX25519(tmp);
                break;
            case 1:
                key = getPubKeyFromX25519(tmp, true);
                break;
            case 2:
                key = getPrivKeyFromEd25519(tmp);
                break;
            case 3:
                key = getPubKeyFromEd25519(tmp, true);
                break;
        }

        return key;
    }

    public void saveRootKeyIntent(Intent intent) {
        intent.putExtra("pass", rootKey);
    }

    public void setup(String password) throws GeneralSecurityException {
        rootKey = KeyUtils.PBKDF(password, "app");
    }

    public boolean isFirstLaunch() {
        return !new File(parentDir, USER_FILE_NAME).exists();
    }

    public LocalUser createLocalUser() {
        try {
            X25519KeyPair idKey = KeyUtils.generateX25519KeyPair();
            SigningKeyPair signKey = KeyUtils.generateSigningKeyPair();

            LocalUser lo = new LocalUser(idKey, signKey, 0);

            lo.setConfig(new Config());

            return lo;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean storeLocalUser(LocalUser user) {
        if (rootKey == null) return false;

        // Create the plain data (4: number of conv, 4: port number, 4: server ip)
        int size = DH_PUB_KEY_SIZE + DH_PRIV_KEY_SIZE + SIGN_PUB_KEY_SIZE + SIGN_PRIV_KEY_SIZE + 4 + 4 + 4;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(size);

        try {
            // IK
            CustomKeyPair keys = user.getIdentityKeys();

            bos.write(keys.getRawPublicKey());
            bos.write(keys.getRawPrivateKey());

            // ISK
            keys = user.getSigningKeyPair();

            bos.write(keys.getRawPublicKey());
            bos.write(keys.getRawPrivateKey());

            // Number of conversations
            bos.write(intToByte(user.getNumberOfConversations()));

            // Config
            bos.write(intToByte(user.getConfig().getPort()));
            bos.write(user.getConfig().getHost());
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] plain = bos.toByteArray();

        return encryptAndSave(plain, new File(parentDir, USER_FILE_NAME), rootKey);
    }

    public LocalUser loadLocalUser() {
        if (rootKey == null) return null;

        byte[] plain = readAndDecrypt(new File(parentDir, USER_FILE_NAME), rootKey);

        if (plain == null) return null;

        try (HByteArrayInputStream bis = new HByteArrayInputStream(plain)) {
            Key pubIK = readKey(bis, DH_PUB);
            Key privIK = readKey(bis, DH_PRIV);
            X25519KeyPair idKey = new X25519KeyPair(pubIK, privIK);

            Key pubSK = readKey(bis, ED_PUB);
            Key privSK = readKey(bis, ED_PRIV);
            SigningKeyPair signKey = new SigningKeyPair(pubSK, privSK);

            int convCount = bis.readInt();

            Config config = new Config();
            config.setPort(bis.readInt());
            config.setHost(bis.readBytes(4));

            LocalUser lo = new LocalUser(idKey, signKey, convCount);
            lo.setConfig(config);

            return lo;
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Conversation
    private File getConversationFile(int convId) {
        File f = new File(parentDir, "conv" + System.getProperty("file.separator") + convId);
        if (!f.exists()) f.mkdirs();

        return f;
    }

    private File getConversationKeysFile(int convId) {
        return new File(getConversationFile(convId), CONV_KEYS_NAME);
    }

    protected File getConversationMessagesFile(int convId) {
        return new File(getConversationFile(convId), CONV_MSG_NAME);
    }

    protected File getConversationMetaFile(int convId) {
        return new File(getConversationFile(convId), CONV_META_NAME);
    }

    protected File getConversationMedia(int convId, String subDir) {
        File f = new File(getConversationFile(convId), "medias" + System.getProperty("file.separator") + subDir);

        if (!f.exists()) f.mkdirs();

        return f;
    }

    public boolean deleteConversation(int convId) {
        File convDir = getConversationFile(convId);

        if (!convDir.exists()) return false;

        return deleteDir(convDir);
    }

    private boolean deleteDir(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) return false;

            for (File child : children)
                if (!deleteDir(child))
                    return false;
        }

        return file.delete();
    }

    public void storeConversation(Conversation c) {
        // Are stored:
        // - IK, ISK (public identity key and signing key of remote user) 64
        // - K, DH (public ratchet key and + and - for DH ratchet) 32 + 32 + 83
        // - RK, SRK, RRK (root key, sending ratchet key, receiving ratchet key) : 32 * 3
        int size = DH_PUB_KEY_SIZE * 3 + SIGN_PUB_KEY_SIZE + DH_PRIV_KEY_SIZE + 32 * 3;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(size);

        try {
            // Write IK and ISK
            User u = c.getRemoteUser();

            bos.write(u.getRawIdentityKey());
            bos.write(u.getRawSigningKey());

            // Write K an DH
            X25519KeyPair dh = c.getDHKeys();

            bos.write(c.getRatchetKey());
            bos.write(dh.getRawPublicKey());
            bos.write(dh.getRawPrivateKey());

            // Write RK, SRK and RRK
            bos.write(c.getRootKey());
            bos.write(c.getSendingRatchet().getChainKey());
            bos.write(c.getReceivingRatchet().getChainKey());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int id = c.getConvId();
        byte[] plain = bos.toByteArray();

        c.closeConversationStorage();

        encryptAndSave(plain, getConversationKeysFile(id), StorageManager.intToByte(id), "conv");

        // Save metadata
        storeMetadata(id, c.constructMetadata(id));
    }

    public Conversation loadConversation(LocalUser locale, int i) {
        File keysDir = getConversationKeysFile(i);
        if (!keysDir.exists()) return null;

        byte[] plain = readAndDecrypt(keysDir, intToByte(i), "conv");

        HByteArrayInputStream bis = new HByteArrayInputStream(plain);
        Conversation conv;

        // Read IK and ISK
        try {
            Key ik = readKey(bis, DH_PUB);
            Key isk = readKey(bis, ED_PUB);

            User remote = new User(ik, isk);

            conv = new Conversation(locale, remote);

            // Read K (pub ratchet key) and DH (key pairs)
            conv.updateRatchetKey(bis.readBytes(DH_PUB_KEY_SIZE)); // K

            // DH
            Key dhPub = readKey(bis, DH_PUB);
            Key dhPriv = readKey(bis, DH_PRIV);

            conv.setDHKeys(new X25519KeyPair(dhPub, dhPriv));

            // Setup all ratchets (RK, RRK, SRK)
            conv.setRootKey(bis.readBytes(32)); // RK
            conv.getSendingRatchet().setChainKey(bis.readBytes(32)); // SRK
            conv.getReceivingRatchet().setChainKey(bis.readBytes(32)); // RRK
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }

        Conversations.addConversation(i, conv);
        conv.setConversationStorage(new ConversationStorage(conv, this));

        // Set metadata
        ConvMetadata meta = loadMetadata(i);
        if (meta != null)
            conv.setConversationName(meta.getName());

        return conv;
    }

    // Conversation metadata
    public ArrayList<ConvMetadata> getConversations() {
        ArrayList<ConvMetadata> convs = new ArrayList<>();
        File convDir = new File(parentDir, "conv");

        File[] files = convDir.listFiles();
        if (files == null) return convs;

        for (File file : files)
            if (file.isDirectory()) {
                int convId = Integer.parseInt(file.getName());
                ConvMetadata meta = loadMetadata(convId);
                if (meta == null) {
                    meta = new ConvMetadata(convId);
                    meta.setName("Error conv");
                }

                convs.add(meta);
            }

        return convs;
    }

    private ConvMetadata loadMetadata(int convId) {
        File file = getConversationMetaFile(convId);

        if (!file.exists())
            return null;

        byte[] plain = readAndDecrypt(file, deriveConvMetaKey(convId));
        ConvMetadata metadata = new ConvMetadata(convId);

        try (HByteArrayInputStream bis = new HByteArrayInputStream(plain)) {
            metadata.setName(bis.readString());
            metadata.setUnreadCount(bis.readInt());

            if (bis.available() > 0) {
                metadata.setLastMessage(bis.readString());
                metadata.setLastMessageDate(bis.readDate());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return metadata;
    }

    public void storeMetadata(int convId, ConvMetadata metadata) {
        File file = getConversationMetaFile(convId);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            // Conversation name
            byte[] tmp = metadata.getName().getBytes(StandardCharsets.UTF_8);
            bos.write(intToByte(tmp.length));
            bos.write(tmp);

            // Number of unread messages
            bos.write(intToByte(metadata.getUnreadCount()));

            if (metadata.getLastMessage() != null) {
                tmp = metadata.getLastMessage().getBytes(StandardCharsets.UTF_8);
                bos.write(intToByte(tmp.length));
                bos.write(tmp);

                bos.write(dateToByte(metadata.getLastMessageDate()));
            }

            encryptAndSave(bos.toByteArray(), file, deriveConvMetaKey(convId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] deriveConvStorageKey(int convId) {
        return deriveKey(intToByte(convId), "msg");
    }

    protected byte[] deriveConvMetaKey(int convId) {
        return deriveKey(intToByte(convId), "msg meta");
    }

    // Utils
    public byte[] deriveKey(byte[] salt, String info) {
        return KeyUtils.deriveRootKey(rootKey, salt, info.getBytes(StandardCharsets.UTF_8));
    }

    protected void encryptAndSave(byte[] plainTest, File file, byte[] salt, String info) {
        byte[] rk = deriveKey(salt, info);

        encryptAndSave(plainTest, file, rk);
    }

    protected boolean encryptAndSave(byte[] plainText, File file, byte[] rk) {
        try {
            byte[] ciphered = KeyUtils.encrypt(rk, plainText);

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            bos.write(ciphered);
            bos.flush();
            bos.close();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    protected byte[] readAndDecrypt(File file, byte[] salt, String info) {
        byte[] rk = deriveKey(salt, info);

        return readAndDecrypt(file, rk);
    }

    protected byte[] readAndDecrypt(File file, byte[] rk) {
        byte[] tmp;

        // Read
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

            tmp = new byte[bis.available()];

            bis.read(tmp);
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // Decrypt
        try {
            tmp = KeyUtils.decrypt(rk, tmp);
        } catch (GeneralSecurityException e) {
            System.err.println(e.getMessage());
            return null;
        }

        return tmp;
    }
}
