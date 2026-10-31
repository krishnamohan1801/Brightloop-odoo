package com.brightloop.auth.service;

import com.brightloop.auth.dao.UserDao;
import com.brightloop.auth.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserDao userDao;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap-admin.name:}") private String name;
    @Value("${app.bootstrap-admin.email:}") private String email;
    @Value("${app.bootstrap-admin.password:}") private String password;

    public AdminBootstrap(UserDao userDao, PasswordEncoder encoder) {
        this.userDao = userDao;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;
        if (!userDao.existsByEmail(email)) {
            userDao.insert(name == null || name.isBlank() ? "Brightloop Admin" : name,
                    email.trim().toLowerCase(), encoder.encode(password), Role.ADMIN);
            System.out.println("Initial ADMIN account created from environment configuration.");
        }
    }
}
