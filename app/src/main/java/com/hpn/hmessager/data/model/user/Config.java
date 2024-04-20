package com.hpn.hmessager.data.model.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Config {

    private int port;

    private byte[] host;

    public Config() {
        this(8765, new byte[] {10, 0, 1, 33});
    }

    public String getHostString() {
        return (host[0] & 0xFF) + "." + (host[1] & 0xFF) + "." + (host[2] & 0xFF) + "." + (host[3] & 0xFF);
    }

    public void setHost(byte a, byte b, byte c, byte d) {
        this.host = new byte[] {a, b, c, d};
    }
}
