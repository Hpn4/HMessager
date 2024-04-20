package com.hpn.hmessager.data.model;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ConvMetadata {

    @Setter
    private String name;

    @Setter
    private String avatarUrl;

    @Setter
    private String lastMessage;

    @Setter
    private Date lastMessageDate;

    @Setter
    private int unreadCount;

    private final int convId;

    public ConvMetadata(int convId) {
        this.convId = convId;
    }

}
