package com.example.DALN.controller;

import com.example.DALN.model.SanBong;
import com.example.DALN.service.SanBongService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/san-bong")
public class SanBongController {

    private final SanBongService sanBongService;

    public SanBongController(SanBongService sanBongService) {
        this.sanBongService = sanBongService;
    }

    // Lấy tất cả sân bóng
    @GetMapping
    public List<SanBong> getAllSanBong() {
        return sanBongService.getAllCourts();
    }

    // Lấy sân bóng theo ID
    @GetMapping("/{id}")
    public SanBong getSanBongById(@PathVariable Long id) {
        return sanBongService.getCourtById(id);
    }

    // Thêm sân bóng
    @PostMapping
    public SanBong createSanBong(@RequestBody SanBong sanBong) {
        return sanBongService.saveCourt(sanBong);
    }

    // Xóa sân bóng
    @DeleteMapping("/{id}")
    public String deleteSanBong(@PathVariable Long id) {
        sanBongService.deleteCourt(id);
        return "Xóa sân bóng thành công";
    }

    @GetMapping("/co-so/{venueId}")
public List<SanBong> getSanBongByVenueId(@PathVariable Long venueId) {
    return sanBongService.getCourtsByVenueId(venueId);
}
}