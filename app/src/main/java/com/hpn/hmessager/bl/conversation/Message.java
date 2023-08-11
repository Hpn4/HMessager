package com.hpn.hmessager.bl.conversation;

import static com.hpn.hmessager.bl.io.StorageManager.byteToDate;
import static com.hpn.hmessager.bl.io.StorageManager.dateToByte;

import com.hpn.hmessager.bl.user.AUser;
import com.hpn.hmessager.bl.user.LocalUser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Message {

    private final Charset charset = StandardCharsets.UTF_8;

    // First metadata byte
    public static final byte YOU = 0x01;
    public static final byte OTHER = 0x00;
    public static final byte SENT = 0x02;
    public static final byte RECEIVED = 0x04;
    public static final byte READ = 0x08;
    // Second metadata byte
    public static final byte TEXT = 0x01;
    public static final byte IMAGE = 0x02;
    public static final byte AUDIO = 0x04;
    public static final byte VIDEO = 0x08;
    private static final int MSG_HEADER_LENGTH = 2 + 8 * 3;

    private byte[] data;

    private String tmpString;

    private MessageType type;

    private boolean sent;

    private boolean received;

    private boolean read;

    private Date sentDate;

    private Date receivedDate;

    private Date readDate;

    private AUser user;

    public Message(String data) {
        this.data = encodeToBytes(data);
    }

    public Message(byte[] data) {
        this.data = data;
    }

    public Message(byte[] deciphered, Conversation c) {
        parseMessage(deciphered, c);
    }

    private void parseMessage(byte[] deciphered, Conversation c) {
        byte r = deciphered[0];

        user = (r & YOU) == YOU ? c.getLocalUser() : c.getRemoteUser();
        sent = (r & SENT) == SENT;
        received = (r & RECEIVED) == RECEIVED;
        read = (r & READ) == READ;

        byte t = deciphered[1];

        if ((t & TEXT) == TEXT) {
            type = MessageType.TEXT;
        } else if ((t & IMAGE) == IMAGE) {
            type = MessageType.IMAGE;
        } else if ((t & AUDIO) == AUDIO) {
            type = MessageType.AUDIO;
        } else if ((t & VIDEO) == VIDEO) {
            type = MessageType.VIDEO;
        } else {
            type = MessageType.UNKNOWN;
        }

        // Get dates
        sentDate = byteToDate(deciphered, 2);
        receivedDate = byteToDate(deciphered, 10);
        readDate = byteToDate(deciphered, 18);

        // Get plain data
        data = new byte[deciphered.length - MSG_HEADER_LENGTH];
        System.arraycopy(deciphered, MSG_HEADER_LENGTH, data, 0, deciphered.length - MSG_HEADER_LENGTH);
    }

    public byte[] constructByteArray() {
        byte[] msg = new byte[data.length + MSG_HEADER_LENGTH];

        // First byte
        msg[0] = user instanceof LocalUser ? YOU : OTHER;
        msg[0] |= sent ? SENT : 0x00;
        msg[0] |= received ? RECEIVED : 0x00;
        msg[0] |= read ? READ : 0x00;

        // Second byte
        msg[1] = type.getCode();

        // Dates
        System.arraycopy(dateToByte(sentDate), 0, msg, 2, 8);
        System.arraycopy(dateToByte(receivedDate), 0, msg, 10, 8);
        System.arraycopy(dateToByte(readDate), 0, msg, 18, 8);

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

    public AUser getUser() {
        return user;
    }

    public void setUser(AUser user) {
        this.user = user;
    }

    public String getUserName() {
        return user.getName();
    }

    public byte[] getDataBytes() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Date getReadDate() {
        return readDate;
    }

    public void setReadDate(Date readDate) {
        this.readDate = readDate;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}
