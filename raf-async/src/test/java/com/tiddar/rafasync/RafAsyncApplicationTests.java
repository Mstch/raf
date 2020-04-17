package com.tiddar.rafasync;

import com.tiddar.rafasync.domain.Log;
import com.tiddar.rafasync.manager.LogRepo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RafAsyncApplicationTests {

	@Resource
	LogRepo logRepo;

	@Test
	void contextLoads() {
	}

	@Test
	void saveAndFlush() {
		logRepo.saveAndFlush(new Log());
	}
}