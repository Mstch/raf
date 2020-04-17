package com.tiddar.raf.http.req;

import com.tiddar.raf.domain.Log;
import lombok.Data;


@Data
public class AppendReq {
    public Integer leaderId;
    public Log[] logs;
    public Integer leaderCommitIndex;
}
