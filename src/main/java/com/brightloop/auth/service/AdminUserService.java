package com.brightloop.auth.service;

import com.brightloop.auth.dao.UserDao;
import com.brightloop.auth.dto.CreateStaffRequest;
import com.brightloop.auth.dto.AuthDtos.ApiMessage;
import com.brightloop.auth.model.Role;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApiMessage createStaff(CreateStaffRequest request) {
        if (userDao.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        try {
            userDao.insert(request.fullName().trim(), request.email().trim().toLowerCase(),
                    passwordEncoder.encode(request.temporaryPassword()), Role.STAFF);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        return new ApiMessage("Staff account created successfully.");
    }
}
