package com.madcamp.moody;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class MoodyApplicationTests {

	@Test
	void contextLoads() {
	}

}
