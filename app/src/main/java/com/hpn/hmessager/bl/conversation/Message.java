package com.hpn.hmessager.bl.conversation;

import static com.hpn.hmessager.bl.conversation.MessageMetadata.*;
import static com.hpn.hmessager.bl.io.StorageManager.byteToDate;
import static com.hpn.hmessager.bl.io.StorageManager.dateToByte;

import com.hpn.hmessager.bl.user.LocalUser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Message {

    private final Charset charset = StandardCharsets.UTF_8;

    // First metadata byte

    private static final int MSG_HEADER_LENGTH = 2 + 8 * 3;

    // Message metadata
    private MessageMetadata m;

    // Message data
    private byte[] data;

    private MediaAttachment mediaAttachment;

    private String tmpString;

    private MessageType type;

    public Message(MediaAttachment media) {
        m = new MessageMetadata();
        mediaAttachment = media;
        type = MessageType.MEDIA;
    }

    public Message(String data) {
        m = new MessageMetadata();
        this.data = encodeToBytes(data);

        if (isOnlyEmoji(data))
            type = MessageType.ONLY_EMOJI;
        else
            type = MessageType.TEXT;
    }

    public Message(byte[] deciphered, Conversation c) {
        parseMessage(deciphered, c);
    }

    Pattern pattern = Pattern.compile("\\w+|\\s+|\\p{P}+");

    private boolean isOnlyEmoji(String text) {
        boolean emoji = !pattern.matcher(text).find();

        return emoji && (text.length() / 2) < 6;
    }

    private void parseMessage(byte[] deciphered, Conversation c) {
        m = new MessageMetadata();
        byte r = deciphered[0];

        m.setUser((r & YOU) == YOU ? c.getLocalUser() : c.getRemoteUser());
        m.setSent((r & SENT) == SENT);
        m.setReceived((r & RECEIVED) == RECEIVED);
        m.setRead((r & READ) == READ);

        byte t = deciphered[1];

        // Get dates
        m.setSentDate(byteToDate(deciphered, 2));
        m.setReceivedDate(byteToDate(deciphered, 10));
        m.setReadDate(byteToDate(deciphered, 18));

        // Get plain data
        data = new byte[deciphered.length - MSG_HEADER_LENGTH];
        System.arraycopy(deciphered, MSG_HEADER_LENGTH, data, 0, deciphered.length - MSG_HEADER_LENGTH);

        type = MessageType.fromCode(t);
    }

    public byte[] constructByteArray() {
        byte[] msg = new byte[data.length + MSG_HEADER_LENGTH];

        // First byte
        msg[0] = m.getUser() instanceof LocalUser ? YOU : OTHER;
        msg[0] |= m.isSent() ? SENT : 0x00;
        msg[0] |= m.isReceived() ? RECEIVED : 0x00;
        msg[0] |= m.isRead() ? READ : 0x00;

        // Second byte
        msg[1] = type.getCode();

        // Dates
        System.arraycopy(dateToByte(m.getSentDate()), 0, msg, 2, 8);
        System.arraycopy(dateToByte(m.getReceivedDate()), 0, msg, 10, 8);
        System.arraycopy(dateToByte(m.getReadDate()), 0, msg, 18, 8);

        // Data
        System.arraycopy(data, 0, msg, MSG_HEADER_LENGTH, data.length);

        return msg;
    }

    private String decodeToString(byte[] bytes) {
        //return new String(Base64.decode(bytes, offset, length, Base64.DEFAULT), charset);
        return new String(bytes, charset);
    }

    private byte[] encodeToBytes(String string) {
        //return Base64.encode(string.getBytes(charset), Base64.DEFAULT);
        return string.getBytes(charset);
    }

    public String getData() {
        if (tmpString == null)
            tmpString = decodeToString(data);

        return tmpString;
    }

    public byte[] getDataBytes() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public MediaAttachment getMediaAttachment() {
        return mediaAttachment;
    }

    public void setMediaAttachment(MediaAttachment mediaAttachment) {
        this.mediaAttachment = mediaAttachment;
    }

    public MessageMetadata getMetadata() {
        return m;
    }
}
