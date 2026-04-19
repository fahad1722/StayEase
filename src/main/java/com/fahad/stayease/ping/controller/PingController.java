package com.fahad.stayease.ping.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("🏓 Ping received! Responding with pong...");
        return ResponseEntity.ok("pong");
    }
}
