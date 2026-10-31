package com.brightloop.auth.controller;

import com.brightloop.auth.dao.UserDao;
import com.brightloop.auth.model.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccessController {
    private final UserDao userDao;
    public AccessController(UserDao userDao) { this.userDao = userDao; }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        AppUser user = userDao.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Map.of("id", user.id(), "fullName", user.fullName(),
                "email", user.email(), "role", user.role().name());
    }

    @GetMapping("/admin/access-check")
    public Map<String, String> admin() { return Map.of("message", "ADMIN access granted"); }

    @GetMapping("/staff/access-check")
    public Map<String, String> staff() { return Map.of("message", "STAFF or ADMIN access granted"); }

    @GetMapping("/customer/access-check")
    public Map<String, String> customer() { return Map.of("message", "CUSTOMER access granted"); }
}
