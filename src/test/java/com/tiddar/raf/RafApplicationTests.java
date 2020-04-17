package com.tiddar.raf;

import com.tiddar.raf.domain.Command;
import com.tiddar.raf.http.resp.CommandResp;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

class RafApplicationTests {

    RestTemplate template = new RestTemplate();

    String leaderHost = "http://localhost:8080";

    @Test
    void contextLoads() {
    }

    @Test
    void saveAndFlush() {
    }

    @Test
    void killLeader() {
        List<Integer> successes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            if (set(i, i)) {
                System.out.println(i + "写成功");
                successes.add(i);
            } else {
                System.err.println(i + "写错误");
            }
        }
        template.getForObject(leaderHost + "/kill", Void.class);
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Integer success : successes) {
            if (get(success) != success) throw new AssertionError();
        }

    }

    boolean set(int key, int value) {

        Command cmd = new Command();
        cmd.opt = "set";
        cmd.value = Integer.toString(key);
        cmd.key = Integer.toString(value);
        return cmd(cmd).success;
    }

    int get(int key) {
        Command cmd = new Command();
        cmd.opt = "get";
        cmd.key = Integer.toString(key);
        return Integer.parseInt(cmd(cmd).result);
    }

    CommandResp cmd(Command cmd) {
        try {
            CommandResp resp = template.postForObject(leaderHost + "/command", cmd, CommandResp.class);
            assert resp != null;
            if (resp.leader != null) {
                leaderHost = resp.leader;
                return template.postForObject(leaderHost + "/command", cmd, CommandResp.class);
            }
            return resp;
        } catch (RestClientException e) {
            e.printStackTrace();
            return new CommandResp("", "", false);
        }
    }
}
