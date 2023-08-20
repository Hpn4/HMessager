package com.hpn.hmessager.bl.conversation;

import android.content.Context;
import android.net.Uri;

import androidx.compose.runtime.snapshots.SnapshotStateList;

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
        Message message = new Message(metadata, this);
        message.getMetadata().setReceivedDate(new Date());
        message.getMetadata().setUser(remoteUser);

        if (media != null) {
            MediaAttachment mediaAttachment = new MediaAttachment(media);
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

    public void sendMedias(List<Uri> medias, Context context) {
        for (Uri media : medias)
            sendMessage(new Message(new MediaAttachment(media, context)));
    }

    public void sendText(String msg) {
        sendMessage(new Message(msg));
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
        Message message = storage.readMessage();
        if (message != null) {
            addToMessagesList(message, 0);
            return true;
        }

        return false;
    }

    public void seePreviousMessages(int count) {
        int loaded = 0;
        for (int i = 0; i < count; i++) {
            Message message = storage.readMessage();
            if (message != null) {
                addToMessagesList(message, 0);
                loaded++;
            }
        }

    }

    private void addToMessagesList(Message msg) {
        messages.add(msg);

        // To avoid consume all the RAM
        if (messages.size() > 200) messages.removeRange(0, 50);
    }

    private void addToMessagesList(Message msg, int offset) {
        // To avoid consume all the RAM
        if (messages.size() > 200) messages.removeRange(0, 50);

        messages.add(offset, msg);
    }

    public ConvMetadata constructMetadata(int convId) {
        ConvMetadata metadata = new ConvMetadata(convId);

        metadata.setName(remoteUser.getName() == null ? "Unknown" : remoteUser.getName());

        if (messages == null) return metadata;

        Message last = messages.size() > 0 ? messages.get(messages.size() - 1) : null;
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

    public void initConv(@NotNull SnapshotStateList<Message> msg) {
        messages = msg;
        PaquetManager.connect(localUser);

        seePreviousMessages(10);
    }
}
