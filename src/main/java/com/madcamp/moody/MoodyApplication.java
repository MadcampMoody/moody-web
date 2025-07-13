package com.madcamp.moody;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoodyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoodyApplication.class, args);
	}

}
