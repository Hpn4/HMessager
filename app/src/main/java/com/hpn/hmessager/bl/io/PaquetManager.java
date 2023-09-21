package com.hpn.hmessager.bl.io;

import com.hpn.hmessager.bl.conversation.Conversations;
import com.hpn.hmessager.bl.crypto.MediaSender;
import com.hpn.hmessager.bl.crypto.Ratchet;
import com.hpn.hmessager.bl.user.Config;
import com.hpn.hmessager.bl.user.LocalUser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class PaquetManager {

    private static final OkHttpClient client = new OkHttpClient();
    private static WebSocket w;

    public static void connect(LocalUser user) {
        Config c = user.getConfig();
        Request.Builder builder = new Request.Builder().url("ws://" + c.getHostString() + ":" + c.getPort());

        w = client.newWebSocket(builder.build(), new HWebSocketListener(user));
    }

    public static void close() {
        if (w == null) return;

        w.close(1000, "Bye");
        // client.dispatcher().executorService().shutdown();
    }

    public static void send(MediaSender sender) {
        if (w == null) return;

        w.send(ByteString.of(sender.getFirstFragment()));

        for (int i = 0; i < sender.getFragCount(); i++)
            w.send(ByteString.of(sender.getNextFragment()));

        System.out.println("[ PaquetManager ] Media sent in " + sender.getFragCount() + " fragments");
        sender.clear();
    }

    public static void send(byte[] msg) {
        if (w == null) return;

        w.send(ByteString.of(msg));
    }

    private static class HWebSocketListener extends WebSocketListener {

        private final LocalUser user;

        public HWebSocketListener(LocalUser user) {
            super();
            this.user = user;
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            System.out.println("onFailure: " + t.getMessage());
            t.printStackTrace();

           // PaquetManager.close(); TODO: uncomment this line
           // PaquetManager.connect(user);
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            System.out.println("onClosed: " + reason);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
            super.onMessage(webSocket, bytes);
            byte[] msg = bytes.toByteArray();

            int convId = Ratchet.getConvIdFromHeader(msg);

            Conversations.getConversation(convId).receiveMessageFromIO(msg);
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);

            webSocket.send(ByteString.of(user.getIdentityKeys().getRawPublicKey()));
        }
    }
}
