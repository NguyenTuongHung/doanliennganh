package com.example.DALN.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
public class DatSan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sân được đặt
    @ManyToOne
    @JoinColumn(name = "court_id")
    private SanBong sanBong;

    // Ngày đặt sân
    private LocalDate ngayDat;

    // Giờ bắt đầu
    private LocalTime gioBatDau;

    // Giờ kết thúc
    private LocalTime gioKetThuc;

    // Tên người đặt
    private String tenNguoiDat;

    // Số điện thoại
    private String soDienThoai;

    // Tổng tiền
    private Double tongTien;

    // Trạng thái
    private String trangThai;

    public DatSan() {
    }

    public DatSan(
            SanBong sanBong,
            LocalDate ngayDat,
            LocalTime gioBatDau,
            LocalTime gioKetThuc,
            String tenNguoiDat,
            String soDienThoai,
            Double tongTien,
            String trangThai) {

        this.sanBong = sanBong;
        this.ngayDat = ngayDat;
        this.gioBatDau = gioBatDau;
        this.gioKetThuc = gioKetThuc;
        this.tenNguoiDat = tenNguoiDat;
        this.soDienThoai = soDienThoai;
        this.tongTien = tongTien;
        this.trangThai = trangThai;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SanBong getSanBong() {
        return sanBong;
    }

    public void setSanBong(SanBong sanBong) {
        this.sanBong = sanBong;
    }

    public LocalDate getNgayDat() {
        return ngayDat;
    }

    public void setNgayDat(LocalDate ngayDat) {
        this.ngayDat = ngayDat;
    }

    public LocalTime getGioBatDau() {
        return gioBatDau;
    }

    public void setGioBatDau(LocalTime gioBatDau) {
        this.gioBatDau = gioBatDau;
    }

    public LocalTime getGioKetThuc() {
        return gioKetThuc;
    }

    public void setGioKetThuc(LocalTime gioKetThuc) {
        this.gioKetThuc = gioKetThuc;
    }

    public String getTenNguoiDat() {
        return tenNguoiDat;
    }

    public void setTenNguoiDat(String tenNguoiDat) {
        this.tenNguoiDat = tenNguoiDat;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public Double getTongTien() {
        return tongTien;
    }

    public void setTongTien(Double tongTien) {
        this.tongTien = tongTien;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}