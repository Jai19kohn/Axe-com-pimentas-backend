package com.axecompimentas.backend.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/session")
    public ResponseEntity<Void> verifySession() {
        return ResponseEntity.noContent().build();
    }
}
