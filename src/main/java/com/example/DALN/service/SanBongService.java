package com.example.DALN.service;

import com.example.DALN.model.SanBong;
import com.example.DALN.repository.SanBongRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SanBongService {

    private final SanBongRepository sanBongRepository;

    public SanBongService(SanBongRepository sanBongRepository) {
        this.sanBongRepository = sanBongRepository;
    }

    // Lấy tất cả sân bóng
    public List<SanBong> getAllCourts() {
        return sanBongRepository.findAll();
    }

    // Lấy sân bóng theo ID
    public SanBong getCourtById(Long id) {
        return sanBongRepository.findById(id).orElse(null);
    }

    // Lấy tất cả sân thuộc một cơ sở
    public List<SanBong> getCourtsByVenueId(Long venueId) {
        return sanBongRepository.findByVenueId(venueId);
    }

    // Thêm sân bóng
    public SanBong saveCourt(SanBong sanBong) {
        return sanBongRepository.save(sanBong);
    }

    // Xóa sân bóng
    public void deleteCourt(Long id) {
        sanBongRepository.deleteById(id);
    }
}