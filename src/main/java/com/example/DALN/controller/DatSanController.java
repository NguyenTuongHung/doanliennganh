package com.example.DALN.controller;

import com.example.DALN.model.DatSan;
import com.example.DALN.service.DatSanService;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dat-san")
public class DatSanController {

    private final DatSanService datSanService;

    public DatSanController(DatSanService datSanService) {
        this.datSanService = datSanService;
    }

    // Lấy tất cả đơn đặt sân
    @GetMapping
    public List<DatSan> getAllDatSan() {
        return datSanService.getAllDatSan();
    }

    // Lấy đơn theo ID
    @GetMapping("/{id}")
    public DatSan getDatSanById(@PathVariable Long id) {
        return datSanService.getDatSanById(id);
    }

    // Xem lịch đặt sân theo ngày
    @GetMapping("/lich")
    public List<DatSan> getLichDatSan(
            @RequestParam Long sanBongId,
            @RequestParam LocalDate ngayDat) {

        return datSanService.getLichDatSan(
                sanBongId,
                ngayDat
        );
    }

    // Đặt sân
    @PostMapping
    public DatSan datSan(@RequestBody DatSan datSan) {
        return datSanService.datSan(datSan);
    }

    // Xóa đơn đặt sân
    @DeleteMapping("/{id}")
    public String xoaDatSan(@PathVariable Long id) {

        datSanService.xoaDatSan(id);

        return "Xóa đơn đặt sân thành công";
    }
}