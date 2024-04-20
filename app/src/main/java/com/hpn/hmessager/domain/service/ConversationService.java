package com.hpn.hmessager.domain.service;

import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.data.repository.StorageManager;

import java.util.HashMap;

public class ConversationService {

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
