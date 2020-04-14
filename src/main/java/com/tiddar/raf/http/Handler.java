package com.tiddar.raf.http;

import com.tiddar.raf.domain.*;
import com.tiddar.raf.http.req.AppendReq;
import com.tiddar.raf.http.req.ApplyNotifyReq;
import com.tiddar.raf.http.req.HeartbeatReq;
import com.tiddar.raf.http.req.VoteReq;
import com.tiddar.raf.http.resp.*;
import com.tiddar.raf.manager.LogRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("")
@Log4j2
public class Handler {


    @Resource
    Node node;

    @Resource
    LogRepo repo;

    @Resource
    Client client;

    @PostMapping("/vote")
    public VoteResp handleVote(@RequestBody VoteReq voteReq) {
        VoteResp resp = new VoteResp();
        if (voteReq.lastLogIndex >= node.lastCommitLogIndex.get()) {
            resp.granted = true;
            node.becomeFollower(-1);
        } else {
            resp.granted = false;
        }
        return resp;
    }

    @PostMapping("/heartbeat")
    public HeartbeatResp handleHeartbeat(@RequestBody HeartbeatReq heartbeatReq) {
        HeartbeatResp resp = new HeartbeatResp();
        if (heartbeatReq.index >= node.lastCommitLogIndex.get()) {
            node.becomeFollower(heartbeatReq.id);
        }
        resp.index = node.lastCommitLogIndex.get();
        return resp;
    }

    @GetMapping("/fsm")
    public Map<String, String> handleStatus() {
        return FSM.state;
    }

    @GetMapping("/ping")
    public String handlePing() {
        return "ok";
    }

    @PostMapping("/command")
    public CommandResp handleCommand(@RequestBody Command cmd) {
        if (node.rule != Rule.LEADER) {
            return new CommandResp(node.peers.get(node.leader.get()).toUri(), null, false);
        }
        if (cmd.opt.equals("get")) {
            return new CommandResp(null, cmd.apply(), true);
        }
        Log log = repo.saveAndFlush(cmd.toLog());
        int index = node.lastCommitLogIndex.incrementAndGet();
        if (client.appendLogsRequest(new Log[]{log})) {
            cmd.apply();
            client.applyNotify(index);
            return new CommandResp(null, cmd.apply(), true);

        } else {
            return new CommandResp(null, null, false);
        }
    }


    @PostMapping("/append")
    public AppendResp handleAppend(@RequestBody AppendReq req) {
        AppendResp resp = new AppendResp();
        if (req.leaderCommitIndex > node.lastCommitLogIndex.get()) {
            node.becomeFollower(req.leaderId);
        }
        resp.index = node.lastCommitLogIndex.get();
        if (req.logs[0].getIndex() == node.lastCommitLogIndex.get() + 1) {
            log.info("成功追加日志" + req.logs.length + "条");
            repo.saveAll(Arrays.asList(req.logs));
            node.lastCommitLogIndex.set(repo.findFirstByOrderByIndexDesc().getIndex());
        }
        return resp;
    }


    @PostMapping("/apply")
    public ApplyNotifyResp handleApply(@RequestBody ApplyNotifyReq req) {
        int lastApplied = node.lastAppliedLogIndex.get();
        if (req.index > lastApplied) {
            List<Log> logsNeedApply = repo.findByIndexGreaterThan(lastApplied);
            for (Log logNeedApply : logsNeedApply) {
                if (logNeedApply.getIndex() <= req.index) {
                    Command.fromString(logNeedApply.getCommand()).apply();
                }
            }
            log.info("成功应用日志:" + (req.index - lastApplied) + "条");
            node.lastAppliedLogIndex.set(req.index);
        }
        return new ApplyNotifyResp();
    }


}
