package com.admaal.resolvo.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.admaal.resolvo")
@EntityScan("com.admaal.resolvo.infrastructure")
@EnableJpaRepositories("com.admaal.resolvo.infrastructure")
public class ResolvoApplication {

  public static void main(String[] args) {
    SpringApplication.run(ResolvoApplication.class, args);
  }
}
