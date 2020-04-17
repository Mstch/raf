package com.tiddar.rafasync.manager;

import com.tiddar.rafasync.domain.Log;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface LogRepo extends ReactiveCrudRepository<Log, Integer> {
    Flux<Log> findByIndexGreaterThan(Integer index);

    Mono<Log> findFirstByOrderByIndexDesc();
}
