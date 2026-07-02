package com.kaoshi.security;

import com.kaoshi.user.domain.UserAccount;
import com.kaoshi.user.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthUserService implements UserDetailsService {
    private final UserMapper userMapper;

    public AuthUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public AuthUser loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userMapper.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        List<String> roles = userMapper.findRoleCodes(user.getId());
        List<String> permissions = userMapper.findPermissionCodes(user.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roles.stream().map(role -> "ROLE_" + role).map(SimpleGrantedAuthority::new).forEach(authorities::add);
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
        return new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPasswordHash(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                roles,
                permissions,
                authorities
        );
    }
}

