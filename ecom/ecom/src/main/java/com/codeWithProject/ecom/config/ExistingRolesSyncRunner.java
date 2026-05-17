package com.codeWithProject.ecom.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

@Slf4j
public class ExistingRolesSyncRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("ExistingRolesSyncRunner désactivé pour éviter le timeout Render.");
    }
}