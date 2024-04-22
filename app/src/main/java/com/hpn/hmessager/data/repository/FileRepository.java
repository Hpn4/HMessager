package com.hpn.hmessager.data.repository;

import static com.hpn.hmessager.converter.DataConverter.intToByte;

import com.hpn.hmessager.domain.crypto.KeyUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import lombok.Getter;
import lombok.Setter;

public class FileRepository {

    // Constants for file name
    private static final String USER_FILE_NAME = "user.hm";
    private static final String PREF_FILE_NAME = "pref.hm";
    private static final String CONV_FILE_NAME = "conv";
    private static final String CONV_KEYS_NAME = "keys.hm";
    private static final String CONV_MSG_NAME = "msg.hm";
    private static final String CONV_META_NAME = "meta.hm";

    private static final String MSG_INFO = "msg";
    private static final String META_INFO = "msg meta";

    private final File parentDir;

    @Setter
    @Getter
    private byte[] rootKey;

    public FileRepository(File parentDir) {
        this.parentDir = parentDir;
    }

    public FileRepository(File parentDir, byte[] rootKey) {
        this.parentDir = parentDir;
        this.rootKey = rootKey;
    }

    // Conversation
    public File getConversationsFile() {
        return new File(parentDir, CONV_FILE_NAME);
    }

    public File getConversationFile(int convId) {
        File f = new File(getConversationsFile(), "" + convId);
        if (!f.exists()) f.mkdirs();

        return f;
    }

    public File getConversationKeysFile(int convId) {
        return new File(getConversationFile(convId), CONV_KEYS_NAME);
    }

    public File getConversationMessagesFile(int convId) {
        return new File(getConversationFile(convId), CONV_MSG_NAME);
    }

    public File getConversationMetaFile(int convId) {
        return new File(getConversationFile(convId), CONV_META_NAME);
    }

    public File getPrefFile() {
        return new File(parentDir, PREF_FILE_NAME);
    }

    public File getUserFile() {
        return new File(parentDir, USER_FILE_NAME);
    }

    public File getConversationMedia(int convId, String subDir) {
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
                if (!deleteDir(child)) return false;
        }

        return file.delete();
    }

    public byte[] deriveConvStorageKey(int convId) {
        return deriveKey(intToByte(convId), MSG_INFO);
    }

    public byte[] deriveConvMetaKey(int convId) {
        return deriveKey(intToByte(convId), META_INFO);
    }

    // Utils
    public byte[] deriveKey(byte[] salt, String info) {
        return KeyUtils.deriveRootKey(rootKey, salt, info.getBytes(StandardCharsets.UTF_8));
    }

    public void encryptAndSave(byte[] plainTest, File file, byte[] salt, String info) {
        byte[] rk = deriveKey(salt, info);

        encryptAndSave(plainTest, file, rk);
    }

    public void writeData(byte[] data, File file) {
        try {
            file.createNewFile();
            BufferedOutputStream bos = null;
            bos = new BufferedOutputStream(new FileOutputStream(file));

            bos.write(data);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] readAllData(File file) {
        try {
            return readAllData(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] readAllData(InputStream stream) {
        byte[] tmp;
        // Read
        try {
            BufferedInputStream bis = new BufferedInputStream(stream);

            tmp = new byte[bis.available()];

            bis.read(tmp);
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return tmp;
    }

    public boolean encryptAndSave(byte[] plainText, File file, byte[] rk) {
        try {
            byte[] ciphered = KeyUtils.encrypt(rk, plainText);

            writeData(ciphered, file);
        } catch (GeneralSecurityException e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    public byte[] readAndDecrypt(File file, byte[] salt, String info) {
        byte[] rk = deriveKey(salt, info);

        return readAndDecrypt(file, rk);
    }

    public byte[] readAndDecrypt(File file, byte[] rk) {
        byte[] tmp = readAllData(file);

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
