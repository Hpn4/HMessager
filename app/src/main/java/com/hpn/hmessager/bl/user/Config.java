package com.hpn.hmessager.bl.user;

public class Config {

    private int port;

    private byte[] host;

    public Config() {
        this(8765, new byte[] {10, 0, 1, 33});
    }

    public Config(int port, byte[] host) {
        this.port = port;
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public byte[] getHost() {
        return host;
    }

    public String getHostString() {
        return (host[0] & 0xFF) + "." + (host[1] & 0xFF) + "." + (host[2] & 0xFF) + "." + (host[3] & 0xFF);
    }

    public void setHost(byte[] host) {
        this.host = host;
    }

    public void setHost(byte a, byte b, byte c, byte d) {
        this.host = new byte[] {a, b, c, d};
    }
}
