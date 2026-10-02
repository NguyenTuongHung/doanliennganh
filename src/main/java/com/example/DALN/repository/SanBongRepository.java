package com.example.DALN.repository;

import com.example.DALN.model.SanBong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SanBongRepository extends JpaRepository<SanBong, Long> {

    List<SanBong> findByVenueId(Long venueId);
}