package com.example.DALN.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class DalnUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;

    public DalnUserDetailsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username.trim().toLowerCase(Locale.ROOT);
        List<AccountRow> accounts = jdbc.query(
                "select id,email,password_hash,status from users where email=?",
                (rs, row) -> new AccountRow(rs.getLong("id"), rs.getString("email"),
                        rs.getString("password_hash"), "ACTIVE".equals(rs.getString("status"))), email);
        if (accounts.isEmpty()) throw new UsernameNotFoundException("Email hoặc mật khẩu không đúng");

        AccountRow account = accounts.get(0);
        List<String> roles = jdbc.query(
                "select r.code from roles r join user_roles ur on ur.role_id=r.id where ur.user_id=?",
                (rs, row) -> rs.getString("code"), account.id);
        return new DalnPrincipal(account.id, account.email, account.passwordHash, account.enabled, roles);
    }

    private record AccountRow(long id, String email, String passwordHash, boolean enabled) {}
}
