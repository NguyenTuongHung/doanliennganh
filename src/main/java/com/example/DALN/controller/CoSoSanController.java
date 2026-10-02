package com.example.DALN.controller;

import com.example.DALN.model.CoSoSan;
import com.example.DALN.service.CoSoSanService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/co-so-san")
public class CoSoSanController {

    private final CoSoSanService coSoSanService;

    public CoSoSanController(CoSoSanService coSoSanService) {
        this.coSoSanService = coSoSanService;
    }

    // Lấy tất cả cơ sở sân
    @GetMapping
    public List<CoSoSan> getAllCoSoSan() {
        return coSoSanService.getAllVenues();
    }

    // Lấy cơ sở sân theo ID
    @GetMapping("/{id}")
    public CoSoSan getCoSoSanById(@PathVariable Long id) {
        return coSoSanService.getVenueById(id);
    }

    // Thêm cơ sở sân
    @PostMapping
    public CoSoSan createCoSoSan(@RequestBody CoSoSan coSoSan) {
        return coSoSanService.saveVenue(coSoSan);
    }

    // Xóa cơ sở sân
    @DeleteMapping("/{id}")
    public String deleteCoSoSan(@PathVariable Long id) {
        coSoSanService.deleteVenue(id);
        return "Xóa cơ sở sân thành công";
    }
}