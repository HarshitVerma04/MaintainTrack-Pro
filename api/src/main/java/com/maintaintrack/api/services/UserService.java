package com.maintaintrack.api.services;

import com.maintaintrack.api.models.AppUser;
import com.maintaintrack.api.repositories.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final AppUserRepository repo;

    public UserService(AppUserRepository repo) {
        this.repo = repo;
    }

    public List<AppUser> getAll() {
        return repo.findAll();
    }

    public Optional<AppUser> getById(Long id) {
        return repo.findById(id);
    }

    public Optional<AppUser> updateRole(Long id, String role) {
        return repo.findById(id).map(user -> {
            user.setRole(role.toUpperCase());
            return repo.save(user);
        });
    }

    public boolean delete(Long id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            return true;
        }
        return false;
    }
}