package com.tiddar.raf.http.resp;

public class CommandResp {
    public String leader;
    public String result;
    public Boolean success;

    public CommandResp(String leader, String result, Boolean success) {
        this.leader = leader;
        this.result = result;
        this.success = success;
    }
}
