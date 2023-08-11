package com.hpn.hmessager.bl.conversation;

import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.user.LocalUser;

import java.util.HashMap;

public class Conversations {

    private static final HashMap<Integer, Conversation> conversations = new HashMap<>();

    public static Conversation getConversation(int convId) {
        return conversations.get(convId);
    }

    public static Conversation getOrLoadConversation(int convId, StorageManager storage, LocalUser user) {
        Conversation conv = conversations.get(convId);

        if(conv == null)
            conv = storage.loadConversation(user, convId);

        return conv;
    }

    public static void addConversation(int convId, Conversation conversation) {
        conversations.put(convId, conversation);
    }

}
