package com.tiddar.rafasync.domain;

import lombok.Data;

@Data
public class Peer {
    public int id;
    public String host;
    public int port;

    public String toUri() {
        return "http://" + host + ":" + port;
    }
}
