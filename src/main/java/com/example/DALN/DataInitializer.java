package com.example.DALN;

import com.example.DALN.model.CoSoSan;
import com.example.DALN.model.SanBong;
import com.example.DALN.repository.CoSoSanRepository;
import com.example.DALN.repository.SanBongRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CoSoSanRepository coSoSanRepository;
    private final SanBongRepository sanBongRepository;

    public DataInitializer(
            CoSoSanRepository coSoSanRepository,
            SanBongRepository sanBongRepository) {

        this.coSoSanRepository = coSoSanRepository;
        this.sanBongRepository = sanBongRepository;
    }

    @Override
    public void run(String... args) {

        // Chỉ tạo dữ liệu nếu database đang trống
        if (coSoSanRepository.count() > 0) {
            return;
        }

        // =========================
        // CƠ SỞ 1
        // =========================

        CoSoSan haDong = new CoSoSan(
                "Sân bóng Hà Đông",
                "Hà Đông, Hà Nội",
                "0987654321"
        );

        coSoSanRepository.save(haDong);

        sanBongRepository.save(
                new SanBong(
                        "Sân 1",
                        "5 người",
                        300000.0,
                        haDong
                )
        );

        sanBongRepository.save(
                new SanBong(
                        "Sân 2",
                        "5 người",
                        300000.0,
                        haDong
                )
        );

        sanBongRepository.save(
                new SanBong(
                        "Sân 3",
                        "7 người",
                        400000.0,
                        haDong
                )
        );

        sanBongRepository.save(
                new SanBong(
                        "Sân 4",
                        "7 người",
                        400000.0,
                        haDong
                )
        );

        // =========================
        // CƠ SỞ 2
        // =========================

        CoSoSan cauGiay = new CoSoSan(
                "Sân bóng Cầu Giấy",
                "Cầu Giấy, Hà Nội",
                "0912345678"
        );

        coSoSanRepository.save(cauGiay);

        sanBongRepository.save(
                new SanBong(
                        "Sân 1",
                        "7 người",
                        400000.0,
                        cauGiay
                )
        );

        sanBongRepository.save(
                new SanBong(
                        "Sân 2",
                        "7 người",
                        450000.0,
                        cauGiay
                )
        );

        // =========================
        // CƠ SỞ 3
        // =========================

        CoSoSan thanhXuan = new CoSoSan(
                "Sân bóng Thanh Xuân",
                "Thanh Xuân, Hà Nội",
                "0909123456"
        );

        coSoSanRepository.save(thanhXuan);

        sanBongRepository.save(
                new SanBong(
                        "Sân 1",
                        "5 người",
                        300000.0,
                        thanhXuan
                )
        );

        sanBongRepository.save(
                new SanBong(
                        "Sân 2",
                        "7 người",
                        400000.0,
                        thanhXuan
                )
        );

        System.out.println("=================================");
        System.out.println("ĐÃ TẠO DỮ LIỆU SÂN BÓNG MẪU");
        System.out.println("=================================");
    }
}