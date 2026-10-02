package com.example.DALN.auth;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Profile register(String email, String fullName, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        jdbc.update("insert ignore into roles(code,name) values('CUSTOMER','Khách hàng')");
        try {
            jdbc.update("insert into users(email,password_hash,full_name,status) values(?,?,?,'ACTIVE')",
                    normalizedEmail, passwordEncoder.encode(rawPassword), fullName.trim());
        } catch (DuplicateKeyException duplicateEmail) {
            throw new EmailAlreadyRegisteredException();
        }
        Long userId = jdbc.queryForObject("select id from users where email=?", Long.class, normalizedEmail);
        jdbc.update("insert into user_roles(user_id,role_id) select ?,id from roles where code='CUSTOMER'", userId);
        return profile(userId);
    }

    public Profile profile(long userId) {
        RowMapper<ProfileRow> profileMapper = (rs, row) -> new ProfileRow(
                rs.getLong("id"), rs.getString("email"), rs.getString("full_name"), rs.getString("phone"));
        List<ProfileRow> profiles = jdbc.query(
                "select id,email,full_name,phone from users where id=?", profileMapper, userId);
        if (profiles.isEmpty()) throw new IllegalArgumentException("Không tìm thấy tài khoản");
        ProfileRow profile = profiles.get(0);
        List<String> roles = jdbc.query("select r.code from roles r join user_roles ur on ur.role_id=r.id where ur.user_id=? order by r.code",
                (rs, row) -> rs.getString("code"), userId);
        return new Profile(profile.id(), profile.email(), profile.fullName(), profile.phone(), roles);
    }

    @Transactional
    public Profile updateProfile(long userId, String fullName, String phone) {
        String normalizedPhone = phone == null || phone.isBlank() ? null : phone.trim();
        jdbc.update("update users set full_name=?,phone=? where id=?", fullName.trim(), normalizedPhone, userId);
        return profile(userId);
    }

    public record Profile(long id, String email, String fullName, String phone, List<String> roles) {}
    private record ProfileRow(long id, String email, String fullName, String phone) {}

    public static class EmailAlreadyRegisteredException extends RuntimeException {}
}
