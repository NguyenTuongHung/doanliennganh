package com.example.DALN.service;

import com.example.DALN.model.CoSoSan;
import com.example.DALN.repository.CoSoSanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoSoSanService {

    private final CoSoSanRepository venueRepository;

    public CoSoSanService(CoSoSanRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public List<CoSoSan> getAllVenues() {
        return venueRepository.findAll();
    }

    public CoSoSan getVenueById(Long id) {
        return venueRepository.findById(id).orElse(null);
    }

    public CoSoSan saveVenue(CoSoSan venue) {
        return venueRepository.save(venue);
    }

    public void deleteVenue(Long id) {
        venueRepository.deleteById(id);
    }
}