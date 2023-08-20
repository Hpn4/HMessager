package com.hpn.hmessager.bl.io;

import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.conversation.MediaAttachment;
import com.hpn.hmessager.bl.conversation.Message;
import com.hpn.hmessager.bl.conversation.MessageType;
import com.hpn.hmessager.bl.crypto.KeyUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class ConversationStorageEM {

    private static final byte[] INFO = "msg chain key".getBytes(StandardCharsets.UTF_8);

    private static final byte[] MEDIA_INFO = "media chain key".getBytes(StandardCharsets.UTF_8);

    private final Conversation conv;

    private StorageManager sm;

    private final byte[] rootKey;

    private RandomAccessFile msgFile;

    private int messageIndex;

    private int messageCount;

    private int mediaCount;


    // Called only from StorageManager
    public ConversationStorageEM(Conversation conv, StorageManager sm) {
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

    public Message readMessage() {
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

        Message m = new Message(plain, conv);

        // If the message is a media, we read the media related to it (the message contains an id of the media)
        // and decrypt it
        if (m.getType() == MessageType.MEDIA) {
            byte[] mediaId = m.getDataBytes();
            byte[] mediaKey = KeyUtils.HKDF(mediaId, rootKey, MEDIA_INFO, 32);

            File file = sm.getConversationMedia(conv.getConvId(), StorageManager.byteToInt(mediaId, 0));
            MediaAttachment media = new MediaAttachment(sm.readAndDecrypt(file, mediaKey));

            m.setMediaAttachment(media);
        }

        return m;
    }

    public void storeMessage(Message msg) {
        if (open()) return;

        // If the message contain a media, we store it in a separate file
        // and we store the id of the media in the message
        if (msg.getType() == MessageType.MEDIA) {
            // Create the media file
            File file = sm.getConversationMedia(conv.getConvId(), mediaCount);
            byte[] key = KeyUtils.HKDF(StorageManager.intToByte(mediaCount), rootKey, MEDIA_INFO, 32);
            byte[] mediaData = msg.getMediaAttachment().constructForStorage();

            sm.encryptAndSave(mediaData, file, key);

            msg.setData(StorageManager.intToByte(mediaCount));

            ++mediaCount;
        }

        byte[] ciphertext;
        try {
            byte[] id = StorageManager.intToByte(messageCount);
            byte[] key = KeyUtils.HKDF(id, rootKey, INFO, 32);

            ciphertext = KeyUtils.encrypt(key, msg.constructByteArray());
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
            } else {
                messageCount = messageIndex = mediaCount = 0;
                writeInt(messageCount);
                writeInt(mediaCount);
            }

            msgFile.seek(msgFile.length());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error opening conversation messages file");
            return true;
        }

        return false;
    }

    private void storeCounter() throws IOException{
        long pointer = msgFile.getFilePointer();

        msgFile.seek(0);

        writeInt(messageCount);
        writeInt(mediaCount);

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
