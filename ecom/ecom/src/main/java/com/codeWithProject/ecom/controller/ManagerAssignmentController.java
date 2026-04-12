package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.dto.AssignManagerDTO;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/manager-assignment")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN_RH')")
public class ManagerAssignmentController {

    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;

    @PutMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignManagerToEmploye(@RequestBody AssignManagerDTO dto) {
        log.info("Assignation manager {} à employé {}", dto.getManagerId(), dto.getEmployeId());

        Employe employe = employeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));

        Manager manager = managerRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager", dto.getManagerId()));

        employe.setManager(manager);
        employeRepository.save(employe);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Manager assigné avec succès");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employes-sans-manager")
    public ResponseEntity<Map<String, Object>> getEmployesSansManager() {
        List<Employe> employes = employeRepository.findEmployesSansManager();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", employes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/managers")
    public ResponseEntity<Map<String, Object>> getAllManagers() {
        List<Manager> managers = managerRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", managers);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employes/{managerId}")
    public ResponseEntity<Map<String, Object>> getEmployesByManager(@PathVariable Long managerId) {
        List<Employe> employes = employeRepository.findByManagerId(managerId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", employes);
        return ResponseEntity.ok(response);
    }
}