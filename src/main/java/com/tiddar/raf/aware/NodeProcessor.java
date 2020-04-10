package com.tiddar.raf.aware;


import com.tiddar.raf.domain.Node;
import com.tiddar.raf.http.Client;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


@Component
public class NodeProcessor  implements CommandLineRunner {
    @Resource
    Node node;

    @Resource
    Client client;

    @Override
    public void run(String... args)  {
        client.startup();
        node.startup();
    }
}
