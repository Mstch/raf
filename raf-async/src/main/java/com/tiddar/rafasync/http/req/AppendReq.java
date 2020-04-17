package com.tiddar.rafasync.http.req;

import com.tiddar.rafasync.domain.Log;

public class AppendReq {
    public Integer leaderId;
    public Log[] logs;
    public Integer leaderCommitIndex;
}
