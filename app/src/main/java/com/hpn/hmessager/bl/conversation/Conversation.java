package com.hpn.hmessager.bl.conversation;

import android.content.Context;
import android.net.Uri;

import androidx.compose.runtime.snapshots.SnapshotStateList;

import com.hpn.hmessager.bl.conversation.message.MediaAttachment;
import com.hpn.hmessager.bl.conversation.message.Message;
import com.hpn.hmessager.bl.conversation.message.MessageType;
import com.hpn.hmessager.bl.crypto.ReceivingRatchet;
import com.hpn.hmessager.bl.crypto.SendingRatchet;
import com.hpn.hmessager.bl.crypto.X25519KeyPair;
import com.hpn.hmessager.bl.io.ConversationStorage;
import com.hpn.hmessager.bl.io.PaquetManager;
import com.hpn.hmessager.bl.user.LocalUser;
import com.hpn.hmessager.bl.user.User;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class Conversation {

    private final LocalUser localUser;
    private final User remoteUser;
    private final SendingRatchet sendingRatchet;
    private final ReceivingRatchet receivingRatchet;
    private SnapshotStateList<Message> messages;
    private ConversationStorage storage;
    private X25519KeyPair dhKeys;

    private Context context;

    private int convId;

    private int destConvId;

    private byte[] ratchetKey; // From receiving message

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

        // Register the conversation
        localUser.addConversation();
        Conversations.addConversation(convId, this);
    }

    public void receiveMedia(byte[] metadata, byte[] media) {
        Message message = new Message(metadata, this, storage.getLastId());
        message.getMetadata().setReceivedDate(new Date());
        message.getMetadata().setUser(remoteUser);

        if (media != null) {
            MediaAttachment mediaAttachment = new MediaAttachment(message.getDataBytes(), media, storage, context);
            message.setMediaAttachment(mediaAttachment);
        }

        addToMessagesList(message);
        storage.storeMessage(message);
    }

    public void receiveMessage(byte[] msg) {
        receiveMedia(msg, null);
    }

    public void receiveMessageFromIO(byte[] msg) {
        receivingRatchet.receiveMessage(msg);
    }

    public void sendMedias(List<Uri> medias) {
        for (Uri media : medias)
            sendMessage(new Message(new MediaAttachment(media, context), storage.getLastId()));
    }

    public void sendText(String msg) {
        sendMessage(new Message(msg, storage.getLastId()));
    }

    private void sendMessage(Message message) {
        message.getMetadata().setSentDate(new Date());
        message.getMetadata().setUser(localUser);

        addToMessagesList(message);
        storage.storeMessage(message);

        if (message.getType() == MessageType.MEDIA)
            PaquetManager.send(sendingRatchet.constructMediaSender(message));
        else PaquetManager.send(sendingRatchet.constructMessage(message));
    }

    public boolean seePreviousMessages() {
        Message message = storage.readMessage(context);
        if (message != null) {
            insertStartToMessagesList(message);
            return true;
        }

        return false;
    }

    public void seePreviousMessages(int count) {
        for (int i = 0; i < count; i++) {
            Message message = storage.readMessage(context);
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

        if (messages == null) return metadata;

        Message last = !messages.isEmpty() ? messages.get(messages.size() - 1) : null;
        if (last != null) {
            if (last.getType().isText()) metadata.setLastMessage(last.getData());
            else metadata.setLastMessage("Media");

            metadata.setLastMessageDate(last.getMetadata().getSentDate());
        }

        return metadata;
    }

    public void setConversationName(String name) {
        remoteUser.setName(name);
    }

    public ConversationStorage getConversationStorage() {
        return storage;
    }

    public void setConversationStorage(ConversationStorage storage) {
        this.storage = storage;
    }

    public void closeConversationStorage() {
        storage.close();
    }

    public LocalUser getLocalUser() {
        return localUser;
    }

    public User getRemoteUser() {
        return remoteUser;
    }

    public SendingRatchet getSendingRatchet() {
        return sendingRatchet;
    }

    public ReceivingRatchet getReceivingRatchet() {
        return receivingRatchet;
    }

    public X25519KeyPair getDHKeys() {
        return dhKeys;
    }

    public void setDHKeys(X25519KeyPair dhKeys) {
        this.dhKeys = dhKeys;
    }

    public byte[] getRatchetKey() {
        return ratchetKey;
    }

    public void updateRatchetKey(byte[] ratchetKey) {
        this.ratchetKey = ratchetKey;
    }

    public byte[] getRootKey() {
        return rootKey;
    }

    public void setRootKey(byte[] rootKey) {
        this.rootKey = rootKey;
    }

    public int getConvId() {
        return convId;
    }

    public int getDestConvId() {
        return destConvId;
    }

    public void initConv(Context context, @NotNull SnapshotStateList<Message> msg) {
        this.context = context;
        messages = msg;
        PaquetManager.connect(localUser);

        seePreviousMessages(10);
    }

    public Context getContext() {
        return context;
    }
}
