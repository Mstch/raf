package com.tiddar.raf.http;

import com.tiddar.raf.domain.Log;
import com.tiddar.raf.domain.Node;
import com.tiddar.raf.domain.Peer;
import com.tiddar.raf.http.req.AppendReq;
import com.tiddar.raf.http.req.ApplyNotifyReq;
import com.tiddar.raf.http.req.HeartbeatReq;
import com.tiddar.raf.http.req.VoteReq;
import com.tiddar.raf.http.resp.AppendResp;
import com.tiddar.raf.http.resp.ApplyNotifyResp;
import com.tiddar.raf.http.resp.HeartbeatResp;
import com.tiddar.raf.http.resp.VoteResp;
import com.tiddar.raf.manager.LogRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Log4j2
@Component
public class Client {


    @Resource
    Node node;

    @Resource
    LogRepo repo;

     RestTemplate  template;

    public void voteRequest() {
        VoteReq req = new VoteReq();
        req.lastLogIndex = node.lastCommitLogIndex.get();
        req.id = node.id;
        int votes = 1;
        VoteResp voteResp = null;
        for (Map.Entry<Integer, Peer> peerEntry : node.peers.entrySet()) {
            Peer peer = peerEntry.getValue();
            long start = System.currentTimeMillis();
            try {
                log.debug("send vote to" + peer.toUri());
                log.debug(Thread.currentThread().getId());
                voteResp = template.postForObject(peer.toUri() + "/vote", req, VoteResp.class);
                if (voteResp != null && voteResp.granted) {
                    votes++;
                    if (votes > node.peers.size() / 2) {
                        node.becomeLeader();
                    }
                }
            } catch (RestClientException e) {
                log.error("spend" + (System.currentTimeMillis() - start) + " ms " + peer.toUri() + " connection / read error: " + e.getLocalizedMessage() + "\n" + voteResp);
            }
        }
    }

    public boolean heartbeatRequest() {
        HeartbeatReq heartBeatReq = new HeartbeatReq();
        heartBeatReq.id = node.id;
        heartBeatReq.index = node.lastCommitLogIndex.get();
        for (Map.Entry<Integer, Peer> peerEntry : node.peers.entrySet()) {
            Peer peer = peerEntry.getValue();
            long start = System.currentTimeMillis();
            try {
                HeartbeatResp heartBeatResp = template.postForObject(peer.toUri() + "/heartbeat", heartBeatReq, HeartbeatResp.class);
                if (heartBeatResp != null) {
                    if (heartBeatResp.index > node.lastCommitLogIndex.get()) {
                        node.becomeFollower(-1);
                        return false;
                    } else if (heartBeatResp.index < node.lastCommitLogIndex.get()) {
                        List<Log> logs = repo.findByIndexGreaterThan(heartBeatResp.index);
                        appendLogsRequestOnSingleNode(peer, logs.toArray(new Log[0]));
                    }
                }
            } catch (RestClientException e) {
                log.error("spend" + (System.currentTimeMillis() - start) + "ms" + peer.toUri() + "connection/read error:" + e.getLocalizedMessage());
                log.error(node);
            }
        }
        node.lastHeartbeat = System.currentTimeMillis();
        return true;
    }


    public boolean appendLogsRequest(Log[] logs) {
        int success = 1;
        for (Map.Entry<Integer, Peer> peerEntry : node.peers.entrySet()) {
            if (appendLogsRequestOnSingleNode(peerEntry.getValue(), logs)) {
                success++;
            }
        }
        return success > node.peers.size() / 2;
    }

    private boolean appendLogsRequestOnSingleNode(Peer peer, Log[] logs) {
        AppendReq appendReq = new AppendReq();
        appendReq.leaderId = node.id;
        appendReq.logs = logs;
        appendReq.leaderCommitIndex = node.lastCommitLogIndex.get();
        try {
            AppendResp appendResp = template.postForObject(peer.toUri() + "/append", appendReq, AppendResp.class);
            if (appendResp != null) {
                if (appendResp.index > node.lastCommitLogIndex.get()) {
                    node.becomeFollower(-1);
                    return false;
                } else if (appendResp.index < node.lastCommitLogIndex.get()) {
                    List<Log> logsInDB = repo.findByIndexGreaterThan(appendResp.index);
                    return appendLogsRequestOnSingleNode(peer, logsInDB.toArray(new Log[0]));
                } else return true;
            }
        } catch (RestClientException e) {
            log.error("node " + peer.toUri() + "connection/read error:" + e.getLocalizedMessage());
        }
        return false;
    }


    public void applyNotify(Integer index) {
        for (Map.Entry<Integer, Peer> peerEntry : node.peers.entrySet()) {
            applyNotifyOnSingleNode(peerEntry.getValue(), index);
        }
    }

    private void applyNotifyOnSingleNode(Peer peer, Integer index) {
        ApplyNotifyReq req = new ApplyNotifyReq();
        req.index = index;
        try {
            template.postForObject(peer.toUri() + "/apply", req, ApplyNotifyResp.class);
        } catch (RestClientException e) {
            log.error("node " + peer.toUri() + "connection/read error:" + e.getLocalizedMessage());
        }
    }

    public void startup() {
        template = new RestTemplateBuilder().defaultHeader("node", node.id.toString()).setReadTimeout(Duration.ofSeconds(5)).setConnectTimeout(Duration.ofSeconds(5)).build();
        Map<Integer, Peer> unConnectPeers = new HashMap<>(node.peers);
        while (true) {
            List<Integer> connectedId = new ArrayList<>();
            for (Map.Entry<Integer, Peer> peerEntry : unConnectPeers.entrySet()) {
                Peer peer = peerEntry.getValue();
                try {
                    template.getForObject(peer.toUri() + "/ping", String.class);
                    connectedId.add(peerEntry.getKey());
                    log.info("成功连接到节点" + peer.toUri());
                } catch (RestClientException ignore) {
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            unConnectPeers.keySet().removeAll(connectedId);
            if (unConnectPeers.isEmpty()) {
                return;
            }
        }
    }
}
