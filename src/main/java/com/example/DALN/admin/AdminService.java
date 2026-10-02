package com.example.DALN.admin;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class AdminService {
    private final JdbcTemplate jdbc;

    public AdminService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Overview overview() {
        return new Overview(count("select count(*) from users"), count("select count(*) from venues"),
                count("select count(*) from venues where status='PENDING'"),
                count("select count(*) from bookings where status in ('HELD','PENDING_PAYMENT','CONFIRMED')"));
    }

    public List<VenueRow> pendingVenues() {
        return jdbc.query("select v.id,v.name,v.address,v.city,v.created_at,u.email owner_email,u.full_name owner_name " +
                        "from venues v join users u on u.id=v.owner_id where v.status='PENDING' order by v.created_at",
                (rs, row) -> new VenueRow(rs.getLong("id"), rs.getString("name"), rs.getString("address"),
                        rs.getString("city"), rs.getString("owner_name"), rs.getString("owner_email"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    @Transactional
    public VenueDecision decideVenue(long venueId, String status) {
        if (!"ACTIVE".equals(status) && !"REJECTED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được duyệt ACTIVE hoặc từ chối REJECTED");
        }
        int updated = jdbc.update("update venues set status=? where id=? and status='PENDING'", status, venueId);
        if (updated == 0) {
            Integer exists = jdbc.queryForObject("select count(*) from venues where id=?", Integer.class, venueId);
            if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cơ sở này đã được xử lý");
        }
        return new VenueDecision(venueId, status);
    }

    private long count(String sql) { return jdbc.queryForObject(sql, Long.class); }

    public record Overview(long users, long venues, long pendingVenues, long activeBookings) {}
    public record VenueRow(long id, String name, String address, String city, String ownerName,
                           String ownerEmail, Instant submittedAt) {}
    public record VenueDecision(long venueId, String status) {}
}
