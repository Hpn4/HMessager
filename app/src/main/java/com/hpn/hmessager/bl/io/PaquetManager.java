package com.hpn.hmessager.bl.io;

import com.hpn.hmessager.bl.conversation.Conversations;
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

    private static WebSocket w;

    private static final OkHttpClient client = new OkHttpClient();

    public static void connect(LocalUser user) {
        Config c = user.getConfig();
        Request.Builder builder = new Request.Builder().url("ws://" + c.getHostString() + ":" + c.getPort());

        w = client.newWebSocket(builder.build(), new WebSocketListener() {
            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                System.out.println("onFailure: " + t.getMessage());
                t.printStackTrace();
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                super.onClosed(webSocket, code, reason);
                System.out.println("onClosed: " + reason);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                super.onMessage(webSocket, text);
                System.out.println("onMessage: " + text);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
                byte[] msg = bytes.toByteArray();

                int convId = Ratchet.getConvIdFromHeader(msg);

                Conversations.getConversation(convId).receiveMessage(msg);
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                super.onOpen(webSocket, response);

                send(user.getIdentityKeys().getRawPublicKey());
            }
        });
    }

    public static void close() {
        if (w == null) return;

        w.close(1000, "Bye");
        // client.dispatcher().executorService().shutdown();
    }

    public static void send(byte[] msg) {
        if (w == null) return;

        w.send(ByteString.of(msg));
    }
}
