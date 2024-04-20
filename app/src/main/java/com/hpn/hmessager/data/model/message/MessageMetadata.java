package com.hpn.hmessager.data.model.message;

import com.hpn.hmessager.data.model.user.AUser;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
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
}
