package com.example.application.data.service;

import com.example.application.data.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectsRepository extends JpaRepository<Projects, Long>, JpaSpecificationExecutor<Projects> {

}
