package com.hpn.hmessager.data.model;

import static com.hpn.hmessager.converter.DataConverter.byteToDate;
import static com.hpn.hmessager.converter.DataConverter.dateToByte;
import static com.hpn.hmessager.data.model.message.MessageMetadata.OTHER;
import static com.hpn.hmessager.data.model.message.MessageMetadata.READ;
import static com.hpn.hmessager.data.model.message.MessageMetadata.RECEIVED;
import static com.hpn.hmessager.data.model.message.MessageMetadata.SENT;
import static com.hpn.hmessager.data.model.message.MessageMetadata.YOU;

import com.hpn.hmessager.data.model.message.MediaAttachment;
import com.hpn.hmessager.data.model.message.MessageMetadata;
import com.hpn.hmessager.data.model.message.MessageType;
import com.hpn.hmessager.data.model.user.LocalUser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

public class Message {

    // Constants: Header length + charset encoding of string message + regex to find only emojis
    private static final int MSG_HEADER_LENGTH = 2 + 8 * 3;
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final Pattern pattern = Pattern.compile("\\w+|\\s+|\\p{P}+");

    @Getter
    private final int id;
    @Setter
    private byte[] data;
    private MessageMetadata m;
    @Setter
    @Getter
    private MediaAttachment mediaAttachment;
    @Getter
    private MessageType type;

    private String tmpString;

    public Message(MediaAttachment media, int id) {
        m = new MessageMetadata();
        this.id = id;
        mediaAttachment = media;
        type = MessageType.MEDIA;
    }

    public Message(String data, int id) {
        m = new MessageMetadata();
        this.id = id;
        this.data = encodeToBytes(data);

        type = isOnlyEmoji(data) ? MessageType.ONLY_EMOJI : MessageType.TEXT;
    }

    public Message(byte[] deciphered, Conversation c, int id) {
        parseMessage(deciphered, c);
        this.id = id;
    }

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

        // Setup media attachment
        if (type == MessageType.MEDIA) mediaAttachment = new MediaAttachment(data, c.getContext());
    }

    /**
     * @param forNetwork true if the message is for the network, false if it is for the storage
     * @return the message as a byte array plus the metadata
     */
    public byte[] constructByteArray(boolean forNetwork) {
        if (type == MessageType.MEDIA) data = mediaAttachment.constructMetadata(forNetwork);

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
        return new String(bytes, charset);
    }

    private byte[] encodeToBytes(String string) {
        return string.getBytes(charset);
    }

    public String getData() {
        if (tmpString == null) tmpString = decodeToString(data);

        return tmpString;
    }

    public byte[] getDataBytes() {
        return data;
    }

    public MessageMetadata getMetadata() {
        return m;
    }
}
