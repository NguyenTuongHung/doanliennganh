package com.example.DALN.booking;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpiryJob {
    private final JdbcTemplate jdbc;

    public HoldExpiryJob(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Scheduled(fixedDelay = 60_000)
    public void expireUnpaidHolds() {
        jdbc.update("update bookings set status='EXPIRED' where status in ('HELD','PENDING_PAYMENT') " +
                "and hold_expires_at <= UTC_TIMESTAMP(6)");
    }
}
