package com.example.DALN.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Creates the first administrator only when explicit credentials are supplied at startup. */
@Component
public class AdminAccountBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminAccountBootstrap.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminAccountBootstrap(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                                 @Value("${daln.admin.email:admin@daln.local}") String email,
                                 @Value("${daln.admin.password:}") String password,
                                 @Value("${daln.admin.name:DALN Administrator}") String fullName) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.isBlank()) {
            log.info("Admin bootstrap skipped; set DALN_ADMIN_PASSWORD to create the first admin account.");
            return;
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 12 ||
                password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("DALN_ADMIN_PASSWORD must be between 12 and 72 UTF-8 bytes.");
        }
        if (email.isBlank() || email.length() > 190 || !email.contains("@")) {
            throw new IllegalStateException("DALN_ADMIN_EMAIL must be a valid email address.");
        }

        jdbc.update("insert ignore into roles(code,name) values('SUPER_ADMIN','Quản trị sàn')");
        Integer existing = jdbc.queryForObject("select count(*) from users where email=?", Integer.class, email);
        if (existing != null && existing > 0) {
            Integer alreadyAdmin = jdbc.queryForObject("select count(*) from users u join user_roles ur on ur.user_id=u.id " +
                    "join roles r on r.id=ur.role_id where u.email=? and r.code='SUPER_ADMIN'", Integer.class, email);
            if (alreadyAdmin != null && alreadyAdmin > 0) {
                log.info("Configured admin account already exists: {}", email);
                return;
            }
            throw new IllegalStateException("The configured admin email already belongs to a non-admin account. Choose another DALN_ADMIN_EMAIL.");
        }

        try {
            jdbc.update("insert into users(email,password_hash,full_name,status) values(?,?,?,'ACTIVE')",
                    email, passwordEncoder.encode(password), fullName);
        } catch (DuplicateKeyException race) {
            throw new IllegalStateException("Could not bootstrap admin because the email is already registered.", race);
        }
        Long id = jdbc.queryForObject("select id from users where email=?", Long.class, email);
        jdbc.update("insert into user_roles(user_id,role_id) select ?,id from roles where code='SUPER_ADMIN'", id);
        log.info("Created DALN super-admin account: {}", email);
    }
}
