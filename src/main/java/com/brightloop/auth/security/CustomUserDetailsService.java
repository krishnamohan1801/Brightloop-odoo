package com.brightloop.auth.security;

import com.brightloop.auth.dao.UserDao;
import com.brightloop.auth.model.AppUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserDao userDao;
    public CustomUserDetailsService(UserDao userDao) { this.userDao = userDao; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userDao.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(user.email())
                .password(user.passwordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())))
                .disabled(!user.enabled())
                .build();
    }
}
