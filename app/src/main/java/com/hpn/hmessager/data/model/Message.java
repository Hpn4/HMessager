package com.hpn.hmessager.data.model;

import com.hpn.hmessager.data.model.message.MediaAttachment;
import com.hpn.hmessager.data.model.message.MessageMetadata;
import com.hpn.hmessager.data.model.message.MessageType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Message {

    // Constants: Header length + charset encoding of string message + regex to find only emojis
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final Pattern emojiPattern = Pattern.compile("\\w+|\\s+|\\p{P}+");

    @Setter
    private int id;
    private MessageMetadata metadata;
    @Setter
    private MessageType type;

    @Setter
    private byte[] data;
    private String text;

    @Setter
    private MediaAttachment mediaAttachment;

    public Message() {
        metadata = new MessageMetadata();
    }

    public Message(MediaAttachment media, int id) {
        this();
        this.id = id;
        mediaAttachment = media;
        type = MessageType.MEDIA;
    }

    public Message(String data, int id) {
        this();
        this.id = id;
        this.data = data.getBytes(charset);

        type = isOnlyEmoji(data) ? MessageType.ONLY_EMOJI : MessageType.TEXT;
    }

    private boolean isOnlyEmoji(String text) {
        boolean emoji = !emojiPattern.matcher(text).find();

        return emoji && (text.length() / 2) < 6;
    }

    public String getText() {
        if (text == null) text = new String(data, charset);

        return text;
    }
}
