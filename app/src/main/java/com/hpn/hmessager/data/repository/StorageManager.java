package com.hpn.hmessager.data.repository;

import static com.hpn.hmessager.converter.DataConverter.intToByte;

import android.content.Context;
import android.content.Intent;

import com.hpn.hmessager.converter.ConvMetadataConverter;
import com.hpn.hmessager.converter.ConversationConverter;
import com.hpn.hmessager.converter.LocalUserConverter;
import com.hpn.hmessager.converter.PreferenceConverter;
import com.hpn.hmessager.data.model.ConvMetadata;
import com.hpn.hmessager.data.model.Conversation;
import com.hpn.hmessager.data.model.Preference;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.domain.crypto.KeyUtils;
import com.hpn.hmessager.domain.service.ConversationService;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import lombok.Getter;

public class StorageManager {

    private static final String INFO = "conv";
    private static final String RK_INFO = "pass";

    @Getter
    private final FileRepository io;

    private final LocalUserConverter localUserConverter = new LocalUserConverter();
    private final PreferenceConverter preferenceConverter = new PreferenceConverter();
    private final ConvMetadataConverter convMetadataConverter = new ConvMetadataConverter();
    private final ConversationConverter conversationConverter = new ConversationConverter();

    public StorageManager(Context context) {
        io = new FileRepository(context.getFilesDir());
    }

    public StorageManager(Context context, Intent intent) {
        io = new FileRepository(context.getFilesDir(), intent.getByteArrayExtra(RK_INFO));
    }

    public void saveRootKeyIntent(Intent intent) {
        intent.putExtra(RK_INFO, io.getRootKey());
    }

    public void setup(String password) throws GeneralSecurityException {
        io.setRootKey(KeyUtils.PBKDF(password, RK_INFO));
    }

    public boolean isFirstLaunch() {
        return !io.getUserFile().exists();
    }

    public LocalUser createLocalUser() {
        return localUserConverter.createLocalUser();
    }

    public boolean storeLocalUser(LocalUser user) {
        if (io.getRootKey() == null) return false;

        byte[] plain = localUserConverter.encode(user);
        if (plain == null) return false;

        return io.encryptAndSave(plain, io.getUserFile(), io.getRootKey());
    }

    public LocalUser loadLocalUser() {
        if (io.getRootKey() == null) return null;

        byte[] plain = io.readAndDecrypt(io.getUserFile(), io.getRootKey());

        return localUserConverter.decode(plain);
    }

    public boolean deleteConversation(int convId) {
        return io.deleteConversation(convId);
    }

    // Conversation metadata
    public ArrayList<ConvMetadata> getConversations() {
        ArrayList<ConvMetadata> convs = new ArrayList<>();

        File[] files = io.getConversationsFile().listFiles();
        if (files == null) return convs;

        for (File file : files)
            if (file.isDirectory()) {
                int convId = Integer.parseInt(file.getName());
                ConvMetadata meta = loadMetadata(convId);
                if (meta == null) {
                    meta = new ConvMetadata(convId);
                    meta.setName("Error conv");
                }

                convs.add(meta);
                System.out.println("[StorageManager]: Convs: Name: " + meta.getName() + ", ID: " + file.getName());
            }

        return convs;
    }

    public void storeConversation(Conversation c) {
        int id = c.getConvId();
        byte[] plain = conversationConverter.encode(c);

        c.closeConversationStorage();

        io.encryptAndSave(plain, io.getConversationKeysFile(id), intToByte(id), INFO);

        // Save metadata
        storeMetadata(id, c.constructMetadata(id));
    }

    public Conversation loadConversation(LocalUser locale, int i) {
        File keysDir = io.getConversationKeysFile(i);
        if (!keysDir.exists()) return null;

        byte[] plain = io.readAndDecrypt(keysDir, intToByte(i), INFO);
        Conversation conv = conversationConverter.decode(plain, locale);
        if (conv == null)
            return null;

        ConversationService.addConversation(i, conv);
        conv.setConversationStorage(new ConversationStorage(conv, this));

        // Set metadata
        ConvMetadata meta = loadMetadata(i);
        if (meta != null) {
            conv.setConversationName(meta.getName());
            conv.getRemoteUser().setAvatarUrl(meta.getAvatarUrl());
        }

        return conv;
    }

    private ConvMetadata loadMetadata(int convId) {
        File file = io.getConversationMetaFile(convId);

        if (!file.exists()) return null;

        byte[] plain = io.readAndDecrypt(file, io.deriveConvMetaKey(convId));

        return convMetadataConverter.decode(plain, convId);
    }

    public void storeMetadata(int convId, ConvMetadata metadata) {
        File file = io.getConversationMetaFile(convId);

        io.encryptAndSave(convMetadataConverter.encode(metadata), file, io.deriveConvMetaKey(convId));
    }

    public Preference loadPreference() {
        File file = io.getPrefFile();

        if (!file.exists()) return null;

        return preferenceConverter.decode(io.readAllData(file));
    }

    public void storePreference(Preference pref) {
        io.writeData(preferenceConverter.encode(pref), io.getPrefFile());
    }
}
