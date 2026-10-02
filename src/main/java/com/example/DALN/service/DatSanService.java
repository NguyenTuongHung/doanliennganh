package com.example.DALN.service;

import com.example.DALN.model.DatSan;
import com.example.DALN.repository.DatSanRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DatSanService {

    private final DatSanRepository datSanRepository;

    public DatSanService(DatSanRepository datSanRepository) {
        this.datSanRepository = datSanRepository;
    }

    // Lấy tất cả đơn đặt sân
    public List<DatSan> getAllDatSan() {
        return datSanRepository.findAll();
    }

    // Lấy đơn đặt sân theo ID
    public DatSan getDatSanById(Long id) {
        return datSanRepository.findById(id).orElse(null);
    }

    // Lấy lịch đặt của một sân trong ngày
    public List<DatSan> getLichDatSan(
            Long sanBongId,
            LocalDate ngayDat) {

        return datSanRepository
                .findBySanBongIdAndNgayDat(sanBongId, ngayDat);
    }

    // Kiểm tra sân có bị trùng giờ không
    public boolean biTrungGio(
            Long sanBongId,
            LocalDate ngayDat,
            LocalTime gioBatDau,
            LocalTime gioKetThuc) {

        List<DatSan> danhSachDat =
                datSanRepository.findBySanBongIdAndNgayDat(
                        sanBongId,
                        ngayDat
                );

        for (DatSan datSan : danhSachDat) {

            // Nếu khoảng thời gian mới giao với khoảng thời gian cũ
            boolean trung =
                    gioBatDau.isBefore(datSan.getGioKetThuc())
                    && gioKetThuc.isAfter(datSan.getGioBatDau());

            if (trung) {
                return true;
            }
        }

        return false;
    }

    // Thêm đơn đặt sân
    public DatSan datSan(DatSan datSan) {

        boolean trung =
                biTrungGio(
                        datSan.getSanBong().getId(),
                        datSan.getNgayDat(),
                        datSan.getGioBatDau(),
                        datSan.getGioKetThuc()
                );

        if (trung) {
            throw new RuntimeException(
                    "Sân đã được đặt trong khoảng thời gian này!"
            );
        }

        return datSanRepository.save(datSan);
    }

    // Xóa đơn đặt sân
    public void xoaDatSan(Long id) {
        datSanRepository.deleteById(id);
    }
}