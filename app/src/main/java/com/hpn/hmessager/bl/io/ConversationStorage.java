package com.hpn.hmessager.bl.io;

import android.content.Context;

import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.conversation.message.MediaAttachment;
import com.hpn.hmessager.bl.conversation.message.MediaType;
import com.hpn.hmessager.bl.conversation.message.Message;
import com.hpn.hmessager.bl.conversation.message.MessageType;
import com.hpn.hmessager.bl.crypto.KeyUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class ConversationStorage {

    private static final byte[] INFO = "msg chain key".getBytes(StandardCharsets.UTF_8);

    private final Conversation conv;

    private final StorageManager sm;

    private final byte[] rootKey;

    private RandomAccessFile msgFile;

    private int messageIndex;

    private int messageCount;

    private int mediaCount;

    private int documentCount;

    private int audioCount;


    // Called only from StorageManager
    public ConversationStorage(Conversation conv, StorageManager sm) {
        rootKey = sm.deriveConvStorageKey(conv.getConvId());
        this.conv = conv;
        this.sm = sm;
    }

    /**
     * Move i messages backward (go back in the message history, ancient message)
     *
     * @param i The number of message to skip
     */
    public void backward(int i) {
        try {
            while (i > 0 && msgFile.getFilePointer() > 8) { // 4 for messageCount, 4 for mediaCount
                msgFile.seek(msgFile.getFilePointer() - 4);
                int msgSize = readInt();

                msgFile.seek(msgFile.getFilePointer() - msgSize - 8); // 8: 4 + 4 for message size
                --i;
                --messageIndex;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forward(int i) {
        try {
            while (i >= 0 && msgFile.getFilePointer() < msgFile.length()) {
                int msgSize = readInt();

                msgFile.seek(msgFile.getFilePointer() + msgSize + 4);

                --i;
                ++messageIndex;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLastId() {
        return messageCount;
    }

    public File getMediaFile(String name, MediaType type) {
        String typeStr = type == MediaType.AUDIO ? "audio" : type == MediaType.DOCUMENT ? "document" : "media";
        File parent = sm.getConversationMedia(conv.getConvId(), typeStr);

        File file = new File(parent, name);
        if (!file.exists()) return file;

        String nameWithoutExt = name.substring(0, name.lastIndexOf('.'));
        String ext = name.substring(name.lastIndexOf('.'));

        do {
            if (type == MediaType.AUDIO) {
                name = nameWithoutExt + "_" + audioCount + ext;
                ++audioCount;
            } else if (type == MediaType.DOCUMENT) {
                name = nameWithoutExt + "_" + documentCount + ext;
                ++documentCount;
            } else {
                name = nameWithoutExt + "_" + mediaCount + ext;
                ++mediaCount;
            }
            file = new File(parent, name);
        } while (file.exists());

        return file;
    }

    public File getMediaFile(MediaType type) {
        String typeStr, name;

        if (type == MediaType.AUDIO) {
            name = "audio_" + audioCount;
            ++audioCount;
            typeStr = "audio";
        } else if (type == MediaType.DOCUMENT) {
            name = "document_" + documentCount;
            ++documentCount;
            typeStr = "document";
        } else {
            name = "media_" + mediaCount + ".jpg";
            ++mediaCount;
            typeStr = "media";
        }

        return new File(sm.getConversationMedia(conv.getConvId(), typeStr), name);
    }

    public Message readMessage(Context context) {
        if (open() || messageIndex <= 0) return null;

        byte[] msg;
        try {
            // Read the size of the message
            msgFile.seek(msgFile.getFilePointer() - 4);

            int msgSize = readInt();

            // Read the message
            msgFile.seek(msgFile.getFilePointer() - msgSize - 4);
            msg = read(msgSize);

            msgFile.seek(msgFile.getFilePointer() - msgSize - 4);
        } catch (IOException e) {
            System.err.println("Error while reading message");
            e.printStackTrace();
            return null;
        }

        // Generate the key
        byte[] id = StorageManager.intToByte(messageIndex - 1);
        byte[] key = KeyUtils.HKDF(id, rootKey, INFO, 32);

        // Decrypt the message
        byte[] plain;
        try {
            plain = KeyUtils.decrypt(key, msg);

            --messageIndex;
        } catch (GeneralSecurityException e) {
            System.err.println("Error while decrypting message");
            --messageIndex;
            return null;
        }

        Message m = new Message(plain, conv, messageIndex + 1);

        // If the message is a media, we read the media related to it (the message contains an id of the media)
        if (m.getType() == MessageType.MEDIA) {
            MediaAttachment media = new MediaAttachment(m.getDataBytes(), context);

            m.setMediaAttachment(media);
        }

        return m;
    }

    public void storeMessage(Message msg) {
        if (open()) return;

        byte[] ciphertext;
        try {
            byte[] id = StorageManager.intToByte(messageCount);
            byte[] key = KeyUtils.HKDF(id, rootKey, INFO, 32);

            ciphertext = KeyUtils.encrypt(key, msg.constructByteArray(false));
        } catch (GeneralSecurityException e) {
            System.err.println("Error while encrypting message");
            return;
        }

        // Write the message
        try {
            long pos = msgFile.getFilePointer();

            msgFile.seek(msgFile.length());

            writeInt(ciphertext.length);
            msgFile.write(ciphertext);
            writeInt(ciphertext.length);

            msgFile.seek(pos);

            ++messageCount;

            storeCounter();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while writing message");
        }
    }

    /*
     * ************************************************** *
     * *************** Open/Close methods *************** *
     * ************************************************** *
     */
    public boolean open() {
        if (msgFile != null) return false;

        File file = sm.getConversationMessagesFile(conv.getConvId());
        try {
            msgFile = new RandomAccessFile(file, "rw");
            msgFile.seek(0);

            if (msgFile.length() > 0) {
                messageCount = messageIndex = readInt();
                mediaCount = readInt();
                documentCount = readInt();
                audioCount = readInt();
            } else {
                messageCount = messageIndex = mediaCount = documentCount = audioCount = 0;
                writeInt(messageCount);
                writeInt(mediaCount);
                writeInt(documentCount);
                writeInt(audioCount);
            }

            msgFile.seek(msgFile.length());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error opening conversation messages file");
            return true;
        }

        return false;
    }

    private void storeCounter() throws IOException {
        long pointer = msgFile.getFilePointer();

        msgFile.seek(0);

        writeInt(messageCount);
        writeInt(mediaCount);
        writeInt(documentCount);
        writeInt(audioCount);

        msgFile.seek(pointer);
    }

    public void close() {
        if (msgFile == null) return;

        // Write the message count and close the file
        try {
            storeCounter();
            msgFile.close();
        } catch (IOException e) {
            System.err.println("Error while closing");
            e.printStackTrace();
        }
    }

    /*
     * *********************************************** *
     * *************** Utility methods *************** *
     * *********************************************** *
     */
    private void writeInt(int i) throws IOException {
        msgFile.write(StorageManager.intToByte(i));
    }

    private int readInt() throws IOException {
        byte[] tmp = new byte[4];

        msgFile.readFully(tmp);

        return StorageManager.byteToInt(tmp, 0);
    }

    private byte[] read(int length) throws IOException {
        byte[] tmp = new byte[length];

        msgFile.readFully(tmp);

        return tmp;
    }
}
