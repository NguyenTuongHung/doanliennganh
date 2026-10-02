package com.example.DALN.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.DALN.security.DalnPrincipal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) { this.bookingService = bookingService; }

    @GetMapping("/sports")
    public List<BookingService.SportView> sports() { return bookingService.sports(); }

    @GetMapping("/venues/search")
    public List<BookingService.VenueView> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        if ((lat == null) != (lng == null) || (lat != null && (Math.abs(lat) > 90 || Math.abs(lng) > 180))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tọa độ không hợp lệ");
        }
        if (radiusKm <= 0 || radiusKm > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bán kính phải từ 0 đến 100 km");
        return bookingService.search(city, sportId, lat, lng, radiusKm);
    }

    @GetMapping("/venues/{venueId}/availability")
    public BookingService.AvailabilityView availability(@PathVariable long venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long sportId) {
        return bookingService.availability(venueId, date, sportId);
    }

    @GetMapping("/bookings/mine")
    public List<BookingService.BookingView> myBookings(@AuthenticationPrincipal DalnPrincipal customer) {
        return bookingService.myBookings(customer.getId());
    }

    @PostMapping("/bookings/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingService.HoldView hold(@RequestHeader(value = "Idempotency-Key", required = false) String key,
                                         @AuthenticationPrincipal DalnPrincipal customer,
                                         @Valid @RequestBody HoldRequest request) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu Idempotency-Key hợp lệ");
        }
        return bookingService.hold(key, customer.getId(), request);
    }

    public record HoldRequest(@NotNull @Positive Long venueId, @NotNull @Positive Long courtId,
                              @NotNull OffsetDateTime startsAt,
                              @NotNull OffsetDateTime endsAt) {}
}
