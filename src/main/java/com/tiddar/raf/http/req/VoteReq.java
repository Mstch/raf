package com.tiddar.raf.http.req;

import lombok.Data;

@Data
public class VoteReq {
    public Integer id;
    public Integer lastLogIndex;
}
