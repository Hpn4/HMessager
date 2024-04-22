package com.hpn.hmessager.data.model.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Config {

    private int port;

    private short[] host;

    public Config() {
        this(8765, new short[] {10, 0, 0, 1});
    }

    public String getHostString() {
        return (host[0] & 0xFF) + "." + (host[1] & 0xFF) + "." + (host[2] & 0xFF) + "." + (host[3] & 0xFF);
    }

    public void setHost(short a, short b, short c, short d) {
        this.host = new short[] {a, b, c, d};
    }
}
