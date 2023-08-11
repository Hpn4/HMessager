package com.hpn.hmessager.bl.cryptotry;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hpn.hmessager.bl.conversation.Conversation;
import com.hpn.hmessager.bl.crypto.KeyUtils;
import com.hpn.hmessager.bl.crypto.X3DH;
import com.hpn.hmessager.bl.io.PaquetManager;
import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.user.LocalUser;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Scanner;

public class HMCrypto {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void main(String[] args) {
        try {
            /*Conversation co = new Conversation(null, null);
            StorageManager storageManage = new StorageManager("test");
            SecureRandom random = new SecureRandom();
            storageManage.setup("salut");

           ConversationStorage convStorage = new ConversationStorage(co, new byte[0]);
            convStorage.open();
            convStorage.storeMessage(ConversationStorage.YOU, "Hello");
            convStorage.storeMessage(ConversationStorage.OTHER, "Hi");
            convStorage.storeMessage(ConversationStorage.YOU, "How are you?");
            convStorage.storeMessage(ConversationStorage.OTHER, "Fine");
            convStorage.close();

            ConversationStorage convStorage = new ConversationStorage(co, new byte[0]);
            convStorage.open();
            System.out.println(convStorage.readMessage());
            convStorage.backward(2);
            System.out.println(convStorage.readMessage());
            convStorage.forward(1);
            System.out.println(convStorage.readMessage());
            convStorage.close();
            System.exit(0);*/
            Scanner scanner = new Scanner(System.in);

            System.out.println("Welcome to HMessager!");
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();

            System.out.print("Enter your password: ");
            String password = scanner.nextLine();

            StorageManager storageManager = new StorageManager(null);
            storageManager.setup(password);

            LocalUser localUser;
            if (storageManager.isFirstLaunch()) {
                System.out.println("First launch");
                localUser = storageManager.createLocalUser();

                storageManager.storeLocalUser(localUser);
            } else {
                localUser = storageManager.loadLocalUser();

                while (localUser == null) {
                    System.out.print("Wrong one. Enter your password: ");
                    password = scanner.nextLine();
                    storageManager.setup(password);

                    localUser = storageManager.loadLocalUser();
                }
            }

            localUser.setName(username);

            // Print with bugInteger
            System.out.println("Identity public key: " + new BigInteger(localUser.getIdentityKeys().getRawPublicKey()).toString(16));
            System.out.println("Signing public key: " + new BigInteger(localUser.getSigningKeyPair().getRawPublicKey()).toString(16));

            Conversation conv = null;
            boolean c = false;
            while (true) {
                System.out.print(">>> ");
                String command = scanner.nextLine();

                if (command.equals("exit")) {
                    PaquetManager.close();

                    if (conv != null) storageManager.storeConversation(conv);
                    break;
                } else if (command.equals("f")) c = true;
                else if (command.equals("connect")) PaquetManager.connect(localUser);
                else if (command.equals("load")) {
                    conv = storageManager.loadConversation(localUser, 0);
                    System.out.println(conv);
                    conv.getRemoteUser().setName("other");
                }

                    // Create a conversation by genrating the QR code for the remote user
                else if (command.equals("create")) {
                    X3DH x3dh = new X3DH(localUser, KeyUtils.generateX25519KeyPair());

                    // Create qr code
                    String qrString = x3dh.generateQrCode(c);
                    System.out.print("QR code for remote user: " + qrString + "\n");

                    // Scan qr code
                    System.out.print("Enter the QR code for the remote user: ");
                    String qrCode = scanner.nextLine();

                    conv = x3dh.generateConversation(qrCode, storageManager);

                    System.out.println("Conversation ms: " + new BigInteger(conv.getRootKey()).toString(16));

                    System.out.print("Enter name for the remote user: ");
                    String name = scanner.nextLine();

                    conv.getRemoteUser().setName(name);
                } else if (conv != null && command.equals("previous"))
                    conv.seePreviousMessages(5);
                else if (conv != null)
                    conv.sendText(command);
            }

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
