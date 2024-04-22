package com.hpn.hmessager.data.model;

import android.content.Context;
import android.net.Uri;

import androidx.compose.runtime.snapshots.SnapshotStateList;

import com.hpn.hmessager.converter.MessageConverter;
import com.hpn.hmessager.data.model.message.MediaAttachment;
import com.hpn.hmessager.data.model.message.MessageType;
import com.hpn.hmessager.domain.crypto.ReceivingRatchet;
import com.hpn.hmessager.domain.crypto.SendingRatchet;
import com.hpn.hmessager.domain.entity.keypair.X25519KeyPair;
import com.hpn.hmessager.data.repository.ConversationStorage;
import com.hpn.hmessager.domain.service.NetworkService;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.data.model.user.User;
import com.hpn.hmessager.domain.service.ConversationService;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Conversation {

    private static final MessageConverter messageConverter = new MessageConverter();

    private final LocalUser localUser;
    private final User remoteUser;
    private final SendingRatchet sendingRatchet;
    private final ReceivingRatchet receivingRatchet;
    private SnapshotStateList<Message> messages;
    @Setter
    private ConversationStorage conversationStorage;
    @Setter
    private X25519KeyPair dhKeys;

    private Context context;

    private int convId;
    private int destConvId;

    private byte[] ratchetKey; // From receiving message

    @Setter
    private byte[] rootKey; // Root key for generating ratchet chain keys

    public Conversation(LocalUser localUser, User remoteUser) {
        sendingRatchet = new SendingRatchet(this);
        receivingRatchet = new ReceivingRatchet(this);

        this.localUser = localUser;
        this.remoteUser = remoteUser;
    }

    public void createConversation(byte[] ratchetKey, X25519KeyPair dhKeys, byte[] ms, int destConvId) {
        this.destConvId = destConvId;
        this.ratchetKey = ratchetKey;
        this.dhKeys = dhKeys;
        convId = localUser.getNumberOfConversations();
        this.rootKey = ms;

        sendingRatchet.setChainKey(rootKey);
        receivingRatchet.setChainKey(rootKey);
        System.out.println("REMOVE THIS: MS: " + Arrays.toString(rootKey));

        // Register the conversation
        localUser.addConversation();
        ConversationService.addConversation(convId, this);
    }

    public void receiveMedia(byte[] metadata, byte[] media) {
        Message message = messageConverter.decode(metadata, this);
        message.setId(conversationStorage.getLastId());
        message.getMetadata().setReceivedDate(new Date());
        message.getMetadata().setUser(remoteUser);

        System.out.println("[Conversation]: receiveMedia: message created with metadata");

        if (media != null) {
            MediaAttachment mediaAttachment = MediaAttachment.fromNetwork(message.getData(), media, conversationStorage, context);
            message.setMediaAttachment(mediaAttachment);
            System.out.println("[Conversation]: receiveMedia: create media attachment and save to disk");
        }

        addToMessagesList(message);
        conversationStorage.storeMessage(message);
    }

    public void receiveMessage(byte[] msg) {
        receiveMedia(msg, null);
    }

    public void sendMedias(List<Uri> medias) {
        for (Uri media : medias) {
            System.out.println("[Conversation] sendMedias: " + media);
            Message message = new Message(MediaAttachment.fromUri(media, context), conversationStorage.getLastId());
            System.out.println("[Conversation] sendMedia: " + message.getType());
            sendMessage(message);
        }
    }

    public void sendText(String msg) {
        sendMessage(new Message(msg, conversationStorage.getLastId()));
    }

    private void sendMessage(Message message) {
        message.getMetadata().setSentDate(new Date());
        message.getMetadata().setUser(localUser);

        addToMessagesList(message);
        System.out.println("[Conversation] sendMessage: before storing");
        conversationStorage.storeMessage(message);
        System.out.println("[Conversation] sendMessage: after storing");

        if (message.getType() == MessageType.MEDIA) {
            System.out.println("[Conversation] Sending media");
            NetworkService.sendMedia(sendingRatchet.constructMediaSender(message));
        }
        else NetworkService.sendMessage(sendingRatchet.constructMessage(message));
    }

    public boolean seePreviousMessages() {
        Message message = conversationStorage.readMessage(context);
        if (message != null) {
            insertStartToMessagesList(message);
            return true;
        }

        return false;
    }

    public void seePreviousMessages(int count) {
        for (int i = 0; i < count; i++) {
            Message message = conversationStorage.readMessage(context);
            if (message != null) insertStartToMessagesList(message);
        }
    }

    private void addToMessagesList(Message msg) {
        messages.add(msg);

        // To avoid consume all the RAM
        if (messages.size() > 200) messages.removeRange(0, 50);
    }

    private void insertStartToMessagesList(Message msg) {
        int size = messages.size();
        // To avoid consume all the RAM
        if (size > 200) messages.removeRange(size - 50, size - 1);

        messages.add(0, msg);
    }

    public ConvMetadata constructMetadata(int convId) {
        ConvMetadata metadata = new ConvMetadata(convId);

        metadata.setName(remoteUser.getName() == null ? "Unknown" : remoteUser.getName());
        metadata.setAvatarUrl(remoteUser.getAvatarUrl());

        if (messages == null) return metadata;

        Message last = !messages.isEmpty() ? messages.get(messages.size() - 1) : null;
        if (last != null) {
            if (last.getType().isText()) metadata.setLastMessage(last.getText());
            else metadata.setLastMessage("Media");

            metadata.setLastMessageDate(last.getMetadata().getSentDate());
        }

        return metadata;
    }

    public void setConversationName(String name) {
        remoteUser.setName(name);
    }

    public void closeConversationStorage() {
        conversationStorage.close();
    }

    public void updateRatchetKey(byte[] ratchetKey) {
        this.ratchetKey = ratchetKey;
    }

    public void initConv(Context context, @NotNull SnapshotStateList<Message> msg) {
        this.context = context;
        messages = msg;
        NetworkService.connect(localUser);

        seePreviousMessages(10);
    }

}
