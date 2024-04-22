package com.hpn.hmessager.domain.service;

import com.hpn.hmessager.data.model.user.Config;
import com.hpn.hmessager.data.model.user.LocalUser;
import com.hpn.hmessager.domain.crypto.MediaSender;
import com.hpn.hmessager.domain.crypto.Ratchet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class NetworkService {

    private static final OkHttpClient client = new OkHttpClient();
    private static WebSocket w;

    /**
     * Starts a Websocket session with the server specified in the user config data.
     *
     * @param user The user to connect
     */
    public static void connect(LocalUser user) {
        Config c = user.getConfig();
        Request.Builder builder = new Request.Builder().url("ws://" + c.getHostString() + ":" + c.getPort());
        System.out.println("[NetworkService]: Connect to server: " + c.getHostString() + ":" + c.getPort());

        w = client.newWebSocket(builder.build(), new HWebSocketListener(user));
    }

    public static void close() {
        if (w == null) return;

        w.close(1000, "Bye");
        // client.dispatcher().executorService().shutdown();
    }

    public static void sendMedia(MediaSender sender) {
        if (w == null) return;

        w.send(ByteString.of(sender.getFirstFragment()));

        for (int i = 0; i < sender.getFragCount(); i++) {
            System.out.println("[NetworkService]: send media " + i + "/" + sender.getFragCount() + "fragments");
            w.send(ByteString.of(sender.getNextFragment()));
        }

        System.out.println("[NetworkService]: send media in " + sender.getFragCount() + " fragments");
        sender.clear();
    }

    public static void sendMessage(byte[] msg) {
        if (w == null) return;

        System.out.println("[NetworkService]: send message: " + msg.length + "b");
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

            System.out.println("[NetworkService]: failed: " + t);
           // PaquetManager.close(); TODO: uncomment this line
           // PaquetManager.connect(user);
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            System.out.println("[NetworkService]: Stream closed: " + reason);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
            super.onMessage(webSocket, bytes);

            // Gets the message from network
            byte[] msg = bytes.toByteArray();

            // Extract conv id from metadata in header
            int convId = Ratchet.getConvIdFromHeader(msg);

            System.out.println("[NetworkService]: received message: " + msg.length + "b -> " + convId);

            // Redirect to correct conversations
            if (!ConversationService.getConversation(convId).getReceivingRatchet().receiveMessage(msg))
                System.out.println("[NetworkService]: ERROR: failed to decode message");
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);

            System.out.println("[NetworkService]: Connected to server, send pub IK");
            webSocket.send(ByteString.of(user.getIdentityKeys().getRawPublicKey()));
        }
    }
}
