package com.hpn.hmessager.bl.conversation;

import java.util.Date;

public class ConvMetadata {

    private String name;

    private String avatarUrl;

    private String lastMessage;

    private Date lastMessageDate;

    private final int convId;

    public ConvMetadata(int convId) {
        this.convId = convId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(Date lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public int getConvId() {
        return convId;
    }
}
