package com.example.DALN.repository;

import com.example.DALN.model.DatSan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DatSanRepository extends JpaRepository<DatSan, Long> {

    List<DatSan> findBySanBongIdAndNgayDat(
            Long sanBongId,
            LocalDate ngayDat
    );

    List<DatSan> findBySanBongIdAndNgayDatAndTrangThai(
            Long sanBongId,
            LocalDate ngayDat,
            String trangThai
    );
}