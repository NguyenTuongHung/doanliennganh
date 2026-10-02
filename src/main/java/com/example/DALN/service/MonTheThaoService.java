package com.example.DALN.service;

import com.example.DALN.model.MonTheThao;
import com.example.DALN.repository.MonTheThaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonTheThaoService {

    private final MonTheThaoRepository sportRepository;

    public MonTheThaoService(MonTheThaoRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    public List<MonTheThao> getAllSports() {
        return sportRepository.findAll();
    }

    public MonTheThao getSportById(Long id) {
        return sportRepository.findById(id).orElse(null);
    }

    public MonTheThao saveSport(MonTheThao sport) {
        return sportRepository.save(sport);
    }

    public void deleteSport(Long id) {
        sportRepository.deleteById(id);
    }
}