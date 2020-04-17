package com.tiddar.rafasync.http;

import com.tiddar.rafasync.domain.*;
import com.tiddar.rafasync.http.req.AppendReq;
import com.tiddar.rafasync.http.req.ApplyNotifyReq;
import com.tiddar.rafasync.http.req.HeartbeatReq;
import com.tiddar.rafasync.http.req.VoteReq;
import com.tiddar.rafasync.http.resp.*;
import com.tiddar.rafasync.manager.LogRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<VoteResp> handleVote(@RequestBody VoteReq voteReq) {
        return Mono.fromSupplier(() -> {
            VoteResp resp = new VoteResp();
            if (voteReq.lastLogIndex >= node.lastCommitLogIndex.get()) {
                resp.granted = true;
                node.becomeFollower(-1);
            } else {
                resp.granted = false;
            }
            return resp;
        });
    }

    @PostMapping("/heartbeat")
    public Mono<HeartbeatResp> handleHeartbeat(@RequestBody HeartbeatReq heartbeatReq) {
        return Mono.fromSupplier(() -> {
            HeartbeatResp resp = new HeartbeatResp();
            if (heartbeatReq.index >= node.lastCommitLogIndex.get()) {
                node.becomeFollower(heartbeatReq.id);
            }
            resp.index = node.lastCommitLogIndex.get();
            return resp;
        });
    }

    @GetMapping("/fsm")
    public Mono<Map<String, String>> handleStatus() {
        return Mono.just(FSM.state);
    }

    @GetMapping("/ping")
    public Mono<String> handlePing() {
        return Mono.just("pong");
    }

    @PostMapping("/command")
    public Mono<CommandResp> handleCommand(@RequestBody Command cmd) {
        return Mono.fromSupplier(() -> {
            if (node.rule != Rule.LEADER) {
                return new CommandResp(node.peers.get(node.leader.get()).toUri(), null, false);
            }
            if (cmd.opt.equals("get")) {
                if (node.readType == ReadType.LEASE) {
                    if (System.currentTimeMillis() - node.lastHeartbeat < 4000) {
                        return new CommandResp(null, cmd.apply(), true);
                    }
                }
                if (client.heartbeatRequest()) {
                    return new CommandResp(null, cmd.apply(), true);
                }
                return new CommandResp(null, null, false);
            }
            Log log = repo.save(cmd.toLog()).block();
            int index = node.lastCommitLogIndex.incrementAndGet();
            return client.appendLogsRequest(new Log[]{log}).map(success -> {
                if (success) {
                    String applyResult = cmd.apply();
                    client.applyNotify(index);
                    return new CommandResp(null, applyResult, true);
                } else {
                    return new CommandResp(null, null, false);
                }
            }).block();
        });
    }


    @PostMapping("/append")
    public AppendResp handleAppend(@RequestBody AppendReq req) {
        AppendResp resp = new AppendResp();
        resp.index = node.lastCommitLogIndex.get();
        if (req.leaderCommitIndex > node.lastCommitLogIndex.get()) {
            node.becomeFollower(req.leaderId);
        }
        if (req.logs[0].getIndex() == node.lastCommitLogIndex.get() + 1) {
            log.info("成功追加日志" + req.logs.length + "条");
            repo.saveAll(Arrays.asList(req.logs));
            repo.findFirstByOrderByIndexDesc().subscribe(lastLog -> node.lastCommitLogIndex.set(lastLog.getIndex()));
        }
        return resp;
    }


    @PostMapping("/apply")
    public void handleApply(@RequestBody ApplyNotifyReq req) {
        int lastApplied = node.lastAppliedLogIndex.get();
        if (req.index > lastApplied) {
            repo.findByIndexGreaterThan(lastApplied)
                    .doOnEach((logNeedApplySig) -> {
                        Log logNeedApply = logNeedApplySig.get();
                        if (logNeedApply.getIndex() <= req.index) {
                            Command.fromString(logNeedApply.getCommand()).apply();
                        }
                    }).doOnComplete(() -> {
                log.info("成功应用日志:" + (req.index - lastApplied) + "条");
                node.lastAppliedLogIndex.set(req.index);
            }).subscribe();
        }
    }

}
