package com.hpn.hmessager.converter;

import static com.hpn.hmessager.converter.DataConverter.byteToDate;
import static com.hpn.hmessager.converter.DataConverter.dateToByte;
import static com.hpn.hmessager.data.model.message.MessageMetadata.OTHER;
import static com.hpn.hmessager.data.model.message.MessageMetadata.READ;
import static com.hpn.hmessager.data.model.message.MessageMetadata.RECEIVED;
import static com.hpn.hmessager.data.model.message.MessageMetadata.SENT;
import static com.hpn.hmessager.data.model.message.MessageMetadata.YOU;

import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.data.model.Message;
import com.hpn.hmessager.data.model.message.MessageMetadata;
import com.hpn.hmessager.data.model.message.MessageType;
import com.hpn.hmessager.data.model.user.LocalUser;

public class MessageConverter extends Converter<Message> {

    public static final int MSG_HEADER_LENGTH = 2 + 8 * 3;

    @Override
    public byte[] encode(Message message, Object other) {
        boolean forNetwork = (Boolean) other;

        byte[] data = message.getData();
        if (message.getType() == MessageType.MEDIA) {
            System.out.println("[MessageConverter]: build media metadata");
            data = message.getMediaAttachment().encodeMetadata(forNetwork);
        }

        MessageMetadata metadata = message.getMetadata();
        byte[] msg = new byte[data.length + MSG_HEADER_LENGTH];

        // First byte
        msg[0] = metadata.getUser() instanceof LocalUser ? YOU : OTHER;
        msg[0] |= metadata.isSent() ? SENT : 0x00;
        msg[0] |= metadata.isReceived() ? RECEIVED : 0x00;
        msg[0] |= metadata.isRead() ? READ : 0x00;

        // Second byte
        msg[1] = message.getType().getCode();

        // Dates
        System.arraycopy(dateToByte(metadata.getSentDate()), 0, msg, 2, 8);
        System.arraycopy(dateToByte(metadata.getReceivedDate()), 0, msg, 10, 8);
        System.arraycopy(dateToByte(metadata.getReadDate()), 0, msg, 18, 8);

        // Data
        System.arraycopy(data, 0, msg, MSG_HEADER_LENGTH, data.length);

        return msg;
    }

    @Override
    public Message decode(byte[] data, Object other) {
        Conversation c = (Conversation) other;
        Message m = new Message();

        // Fetch metadata
        MessageMetadata metadata = m.getMetadata();
        byte r = data[0];

        metadata.setUser((r & YOU) == YOU ? c.getLocalUser() : c.getRemoteUser());
        metadata.setSent((r & SENT) == SENT);
        metadata.setReceived((r & RECEIVED) == RECEIVED);
        metadata.setRead((r & READ) == READ);

        // Get dates
        metadata.setSentDate(byteToDate(data, 2));
        metadata.setReceivedDate(byteToDate(data, 10));
        metadata.setReadDate(byteToDate(data, 18));

        // Get plain data and type
        byte[] plain = new byte[data.length - MSG_HEADER_LENGTH];
        System.arraycopy(data, MSG_HEADER_LENGTH, plain, 0, data.length - MSG_HEADER_LENGTH);

        m.setData(plain);
        m.setType(MessageType.fromCode(data[1]));

        return m;
    }
}
