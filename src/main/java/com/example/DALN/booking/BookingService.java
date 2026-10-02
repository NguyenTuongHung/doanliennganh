package com.example.DALN.booking;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
public class BookingService {
    private final JdbcTemplate jdbc;
    public BookingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<SportView> sports() {
        return jdbc.query("select id, code, name from sports_categories where active=true order by name",
                (rs, n) -> new SportView(rs.getLong("id"), rs.getString("code"), rs.getString("name")));
    }

    public List<VenueView> search(String city, Long sportId, Double lat, Double lng, double radiusKm) {
        StringBuilder sql = new StringBuilder("select v.id,v.name,v.address,v.city,v.location, " +
                "(select count(*) from courts c where c.venue_id=v.id and c.active=true and c.bookable=true " +
                "and not exists(select 1 from courts ch where ch.parent_id=c.id and ch.active=true)) court_count " +
                "from venues v where v.status='ACTIVE'");
        List<Object> args = new ArrayList<>();
        if (city != null && !city.isBlank()) { sql.append(" and v.city like ?"); args.add("%" + city.trim() + "%"); }
        if (sportId != null) { sql.append(" and exists(select 1 from venue_sports vs where vs.venue_id=v.id and vs.sport_id=?)"); args.add(sportId); }
        if (lat != null) {
            double latDelta = radiusKm / 110.574;
            double cosine = Math.max(0.01, Math.cos(Math.toRadians(lat)));
            double lngDelta = radiusKm / (111.320 * cosine);
            if (lngDelta < 180 && lng - lngDelta >= -180 && lng + lngDelta <= 180) {
                double south = Math.max(-90, lat - latDelta), north = Math.min(90, lat + latDelta);
                double west = lng - lngDelta, east = lng + lngDelta;
                String envelope = String.format(Locale.ROOT,
                        "POLYGON((%.8f %.8f,%.8f %.8f,%.8f %.8f,%.8f %.8f,%.8f %.8f))",
                        west,south,east,south,east,north,west,north,west,south);
                sql.append(" and MBRContains(ST_GeomFromText(?,4326,'axis-order=long-lat'),v.location)");
                args.add(envelope);
            }
            sql.append(" and ST_Distance_Sphere(v.location,ST_GeomFromText(?,4326,'axis-order=long-lat')) <= ? " +
                    "order by ST_Distance_Sphere(v.location,ST_GeomFromText(?,4326,'axis-order=long-lat')) limit 100");
            String origin = String.format(Locale.ROOT, "POINT(%.8f %.8f)", lng, lat);
            args.add(origin); args.add(radiusKm * 1000); args.add(origin);
        } else sql.append(" order by v.name limit 100");
        return jdbc.query(sql.toString(), (rs, n) -> new VenueView(rs.getLong("id"), rs.getString("name"),
                rs.getString("address"), rs.getString("city"), rs.getInt("court_count")), args.toArray());
    }

    public AvailabilityView availability(long venueId, LocalDate date, Long sportId) {
        VenueHours hours = jdbc.query("select timezone,opening_time,closing_time from venues where id=? and status='ACTIVE'",
                rs -> rs.next() ? new VenueHours(rs.getString(1), rs.getTime(2).toLocalTime(), rs.getTime(3).toLocalTime()) : null, venueId);
        if (hours == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở");
        List<CourtView> courts = jdbc.query("select c.id,c.name,s.code sport_code from courts c join sports_categories s on s.id=c.sport_id " +
                "where c.venue_id=? and c.active=true and c.bookable=true and (? is null or c.sport_id=?) " +
                "and not exists(select 1 from courts ch where ch.parent_id=c.id and ch.active=true) order by c.id",
                (rs, n) -> new CourtView(rs.getLong("id"), rs.getString("name"), rs.getString("sport_code"), new ArrayList<>()),
                venueId, sportId, sportId);
        ZoneId zone = ZoneId.of(hours.timezone);
        LocalDateTime open = date.atTime(hours.opening), close = date.atTime(hours.closing);
        if (!close.isAfter(open)) close = close.plusDays(1);
        for (CourtView court : courts) {
            for (LocalDateTime start = open; !start.plusHours(1).isAfter(close); start = start.plusHours(1)) {
                LocalDateTime slotStart = start;
                LocalDateTime end = slotStart.plusHours(1);
                LocalDateTime dbStart = slotStart.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                LocalDateTime dbEnd = end.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                List<SlotView> overlap = jdbc.query("select b.status,b.hold_expires_at,bd.unit_price from booking_details bd " +
                                "join bookings b on b.id=bd.booking_id where bd.court_id=? and bd.starts_at < ? and bd.ends_at > ? " +
                                "and (b.status='CONFIRMED' or (b.status in ('HELD','PENDING_PAYMENT') and b.hold_expires_at > UTC_TIMESTAMP(6))) limit 1",
                        (rs, n) -> new SlotView(slotStart.atZone(zone).toOffsetDateTime(), end.atZone(zone).toOffsetDateTime(),
                                rs.getString("status").equals("CONFIRMED") ? "BOOKED" : "HELD", rs.getBigDecimal("unit_price")),
                        court.courtId, Timestamp.valueOf(dbEnd), Timestamp.valueOf(dbStart));
                if (!overlap.isEmpty()) { court.slots.add(overlap.get(0)); continue; }
                BigDecimal price = priceFor(venueId, sportIdFor(court.courtId), court.courtId, date, slotStart.toLocalTime());
                String status = slotStart.atZone(zone).toInstant().isBefore(Instant.now()) ? "PAST" : "AVAILABLE";
                court.slots.add(new SlotView(slotStart.atZone(zone).toOffsetDateTime(), end.atZone(zone).toOffsetDateTime(), status, price));
            }
        }
        return new AvailabilityView(venueId, date, hours.timezone, 60, courts);
    }

    private long sportIdFor(long courtId) {
        return jdbc.queryForObject("select sport_id from courts where id=?", Long.class, courtId);
    }

    private BigDecimal priceFor(long venueId, long sportId, long courtId, LocalDate date, LocalTime time) {
        List<BigDecimal> prices = jdbc.query("select price_per_slot from pricing_rules where venue_id=? and sport_id=? and active=true " +
                "and (court_id is null or court_id=?) and (day_of_week is null or day_of_week=?) " +
                "and start_time<=? and end_time>? and (valid_from is null or valid_from<=?) and (valid_until is null or valid_until>=?) " +
                "order by (court_id is not null) desc,priority desc limit 1",
                (rs, n) -> rs.getBigDecimal(1), venueId, sportId, courtId, date.getDayOfWeek().getValue(), time, time, date, date);
        return prices.isEmpty() ? BigDecimal.ZERO : prices.get(0);
    }

    public List<BookingView> myBookings(long customerId) {
        return jdbc.query("select b.id,b.booking_code,b.status,b.total_amount,b.deposit_amount,b.hold_expires_at, " +
                        "v.name venue_name,c.name court_name,bd.starts_at,bd.ends_at " +
                        "from bookings b join booking_details bd on bd.booking_id=b.id " +
                        "join venues v on v.id=bd.venue_id join courts c on c.id=bd.court_id " +
                        "where b.customer_id=? order by b.created_at desc,bd.starts_at desc",
                (rs, row) -> new BookingView(rs.getLong("id"), rs.getString("booking_code"), rs.getString("status"),
                        rs.getString("venue_name"), rs.getString("court_name"),
                        rs.getTimestamp("starts_at").toInstant().atOffset(ZoneOffset.UTC),
                        rs.getTimestamp("ends_at").toInstant().atOffset(ZoneOffset.UTC),
                        rs.getBigDecimal("total_amount"), rs.getBigDecimal("deposit_amount"),
                        rs.getTimestamp("hold_expires_at") == null ? null : rs.getTimestamp("hold_expires_at").toInstant()), customerId);
    }

    @Transactional
    public HoldView hold(String key, long customerId, BookingController.HoldRequest request) {
        Integer eligible = jdbc.queryForObject("select count(*) from courts c join venues v on v.id=c.venue_id " +
                "where c.id=? and c.venue_id=? and c.active=true and c.bookable=true and v.status='ACTIVE' " +
                "and not exists(select 1 from courts ch where ch.parent_id=c.id and ch.active=true)",
                Integer.class, request.courtId(), request.venueId());
        if (eligible == null || eligible != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sân không khả dụng");
        List<Long> ids = jdbc.query("select distinct related.descendant_id from court_closure selected " +
                        "join court_closure related on related.ancestor_id=selected.ancestor_id where selected.descendant_id=? order by 1",
                (rs, n) -> rs.getLong(1), request.courtId());
        if (ids.isEmpty()) ids = List.of(request.courtId());
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Long> locked = jdbc.query("select id from courts where id in (" + placeholders + ") and venue_id=? order by id for update",
                (rs, n) -> rs.getLong(1), combine(ids, request.venueId()));
        if (locked.size() != ids.size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy sân thuộc cơ sở này");
        List<HoldView> previous = jdbc.query("select id,booking_code,status,hold_expires_at,total_amount,deposit_amount,currency from bookings where customer_id=? and idempotency_key=?",
                (rs, n) -> new HoldView(rs.getLong("id"),rs.getString("booking_code"),rs.getString("status"),
                        rs.getTimestamp("hold_expires_at").toInstant(),rs.getBigDecimal("total_amount"),rs.getBigDecimal("deposit_amount"),rs.getString("currency")), customerId, key);
        if (!previous.isEmpty()) return previous.get(0);
        VenueHours venueHours = jdbc.query("select timezone,opening_time,closing_time from venues where id=?",
                rs -> rs.next() ? new VenueHours(rs.getString(1),rs.getTime(2).toLocalTime(),rs.getTime(3).toLocalTime()) : null, request.venueId());
        if (venueHours == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở");
        ZoneId venueZone = ZoneId.of(venueHours.timezone);
        ZonedDateTime localStart = request.startsAt().atZoneSameInstant(venueZone), localEnd = request.endsAt().atZoneSameInstant(venueZone);
        LocalDateTime start = localStart.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime end = localEnd.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (!end.isAfter(start) || Duration.between(start,end).toMinutes() != 60 || request.startsAt().toInstant().isBefore(Instant.now().minusSeconds(60)))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Khung giờ không hợp lệ (MVP hỗ trợ đúng 60 phút)");
        LocalDateTime opening = localStart.toLocalDate().atTime(venueHours.opening);
        LocalDateTime closing = localStart.toLocalDate().atTime(venueHours.closing);
        if (!closing.isAfter(opening)) closing = closing.plusDays(1);
        long offsetMinutes = Duration.between(opening, localStart.toLocalDateTime()).toMinutes();
        if (offsetMinutes < 0 || localEnd.toLocalDateTime().isAfter(closing) || offsetMinutes % 60 != 0 ||
                localStart.getSecond() != 0 || localStart.getNano() != 0)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Khung giờ nằm ngoài lịch mở cửa");
        List<Long> conflicts = jdbc.query("select bd.court_id from booking_details bd join bookings b on b.id=bd.booking_id " +
                "where bd.court_id in ("+placeholders+") and bd.starts_at < ? and bd.ends_at > ? " +
                "and (b.status='CONFIRMED' or (b.status in ('HELD','PENDING_PAYMENT') and b.hold_expires_at>UTC_TIMESTAMP(6))) limit 1",
                (rs,n)->rs.getLong(1), combine(ids, Timestamp.valueOf(end), Timestamp.valueOf(start)));
        if (!conflicts.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ vừa được người khác giữ");
        Long sportId = jdbc.queryForObject("select sport_id from courts where id=?", Long.class, request.courtId());
        BigDecimal price = priceFor(request.venueId(), sportId, request.courtId(), localStart.toLocalDate(), localStart.toLocalTime());
        if (price.signum() <= 0) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Sân chưa được cấu hình giá cho khung giờ này");
        Instant expiry = Instant.now().plus(Duration.ofMinutes(10));
        String code = "DALN-" + UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        Long bookingId;
        try {
            jdbc.update("insert into bookings(booking_code,customer_id,status,currency,total_amount,deposit_amount,hold_expires_at,idempotency_key) values(?,?,'HELD','VND',?,?,?,?)",
                    code,customerId,price,price.multiply(new BigDecimal("0.30")),Timestamp.from(expiry),key);
            bookingId = jdbc.queryForObject("select id from bookings where booking_code=?",Long.class,code);
        } catch (DuplicateKeyException duplicate) {
            return jdbc.queryForObject("select id,booking_code,status,hold_expires_at,total_amount,deposit_amount,currency from bookings where customer_id=? and idempotency_key=?",
                    (rs,n)->new HoldView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getTimestamp(4).toInstant(),rs.getBigDecimal(5),rs.getBigDecimal(6),rs.getString(7)),customerId,key);
        }
        jdbc.update("insert into booking_details(booking_id,venue_id,court_id,sport_id,starts_at,ends_at,unit_price) values(?,?,?,?,?,?,?)",
                bookingId,request.venueId(),request.courtId(),sportId,Timestamp.valueOf(start),Timestamp.valueOf(end),price);
        return new HoldView(bookingId,code,"HELD",expiry,price,price.multiply(new BigDecimal("0.30")),"VND");
    }

    private static Object[] combine(List<Long> ids, Object... tail) {
        Object[] result = new Object[ids.size()+tail.length]; int i=0;
        for (Long id:ids) result[i++]=id; for (Object value:tail) result[i++]=value; return result;
    }

    private record VenueHours(String timezone, LocalTime opening, LocalTime closing) {}
    public record SportView(long id,String code,String name) {}
    public record VenueView(long id,String name,String address,String city,int courtCount) {}
    public record AvailabilityView(long venueId,LocalDate date,String timezone,int slotMinutes,List<CourtView> courts) {}
    public record SlotView(OffsetDateTime startsAt,OffsetDateTime endsAt,String status,BigDecimal price) {}
    public static class CourtView {
        public final long courtId; public final String name; public final String sport; public final List<SlotView> slots;
        CourtView(long courtId,String name,String sport,List<SlotView> slots){this.courtId=courtId;this.name=name;this.sport=sport;this.slots=slots;}
    }
    public record HoldView(long bookingId,String bookingCode,String status,Instant holdExpiresAt,BigDecimal totalAmount,BigDecimal depositAmount,String currency) {}
    public record BookingView(long bookingId, String bookingCode, String status, String venueName, String courtName,
                              OffsetDateTime startsAt, OffsetDateTime endsAt, BigDecimal totalAmount,
                              BigDecimal depositAmount, Instant holdExpiresAt) {}
}
