package com.tiddar.raf.manager;

import com.tiddar.raf.domain.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepo extends JpaRepository<Log,Integer> {

    List<Log> findByIndexGreaterThan(Integer index);

        Log findFirstByOrderByIndexDesc();
}
