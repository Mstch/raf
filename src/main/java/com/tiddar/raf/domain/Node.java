package com.tiddar.raf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiddar.raf.http.Client;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Data
@ConfigurationProperties(prefix = "node")
@Log4j2
public class Node {
    @Resource
    @JsonIgnore
    Client client;
    public Integer id;
    public Map<Integer, Peer> peers;
    public Rule rule = Rule.FOLLOWER;
    public ReadType readType = ReadType.READ_INDEX;
    public AtomicInteger leader = new AtomicInteger(-1);
    public AtomicInteger voteFor = new AtomicInteger(-1);
    public AtomicInteger lastCommitLogIndex = new AtomicInteger(0);
    public AtomicInteger lastAppliedLogIndex = new AtomicInteger(0);
    public Map<String, String> stateView = FSM.state;

    public Long lastHeartbeat = 0L;

    @JsonIgnore
    private RandomTimer timer = new RandomTimer();


    public void startup() {
        this.rule = Rule.FOLLOWER;
        this.voteFor.set(-1);
        this.timer.changeTo("election timeout", this::becomeCandidate, 5500, 4000);
    }

    public void becomeFollower(int leaderId) {
        if (this.rule != Rule.FOLLOWER) {
            log.info("node:" + id + "become follower");
        }
        this.rule = Rule.FOLLOWER;
        if (leaderId != -1) {
            this.leader.set(leaderId);
        }
        this.voteFor.set(-1);
        if (this.rule != Rule.FOLLOWER) {
            this.timer.changeTo("election timeout", this::becomeCandidate, 5500, 4000);
        } else {
            this.timer.reLoop();
        }
    }

    public void becomeCandidate() {
        log.info("node:" + id + "become candidate");
        this.rule = Rule.CANDIDATE;
        this.voteFor.set(this.id);
        client.voteRequest();
    }

    public void becomeLeader() {
        log.info("node:" + id + "become leader");
        this.rule = Rule.LEADER;
        this.leader.set(this.id);
        this.timer.changeTo("heartbeat timeout", client::heartbeatRequest, 550, 400);
    }
}

