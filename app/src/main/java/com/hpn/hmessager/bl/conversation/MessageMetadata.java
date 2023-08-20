package com.hpn.hmessager.bl.conversation;

import com.hpn.hmessager.bl.user.AUser;

import java.util.Date;

public class MessageMetadata {

    public static final byte YOU = 0x01;
    public static final byte OTHER = 0x00;
    public static final byte SENT = 0x02;
    public static final byte RECEIVED = 0x04;
    public static final byte READ = 0x08;

    private boolean sent;

    private boolean received;

    private boolean read;

    private Date sentDate;

    private Date receivedDate;

    private Date readDate;

    private AUser user;

    public MessageMetadata() {
    }

    public AUser getUser() {
        return user;
    }

    public void setUser(AUser user) {
        this.user = user;
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
}
