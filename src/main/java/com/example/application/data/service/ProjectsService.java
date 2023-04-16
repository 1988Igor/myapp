package com.example.application.data.service;

import com.example.application.data.entity.Projects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ProjectsService {

    private final ProjectsRepository repository;

    public ProjectsService(ProjectsRepository repository) {
        this.repository = repository;
    }

    public Optional<Projects> get(Long id) {
        return repository.findById(id);
    }

    public Projects update(Projects entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Projects> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Projects> list(Pageable pageable, Specification<Projects> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
