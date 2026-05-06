 package com.codeWithProject.ecom.controller;

 import com.codeWithProject.ecom.controller.dto.ApiResponse;
 import com.codeWithProject.ecom.entity.Employe;
 import com.codeWithProject.ecom.repository.DemandeCongeRepository;
 import com.codeWithProject.ecom.repository.EmployeRepository;
 import com.codeWithProject.ecom.service.WorkflowService;

 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;

 import org.springframework.http.ResponseEntity;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.security.core.annotation.AuthenticationPrincipal;
 import org.springframework.security.oauth2.jwt.Jwt;
 import org.springframework.web.bind.annotation.*;

 import java.util.*;

 @RestController
 @RequestMapping("/api/manager/workflow")  
 @RequiredArgsConstructor
 @Slf4j
 public class ManagerWorkflowController {

     private final WorkflowService workflowService;
     private final EmployeRepository employeRepository;
     private final DemandeCongeRepository demandeCongeRepository;

    //   ================= STATS WORKFLOW =================

     @GetMapping("/stats-workflow")
     @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
     public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@AuthenticationPrincipal Jwt jwt) {

         String email = extractEmail(jwt);

         Map<String, Object> stats = new HashMap<>();

         List<Employe> equipe = employeRepository.findByManagerEmail(email);
         stats.put("employes", equipe.size());

         List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
         stats.put("congesEnAttente", tasks.size());

         long approuve = demandeCongeRepository.countByStatut("APPROUVE");
         long refuse = demandeCongeRepository.countByStatut("REFUSE");

         stats.put("demandesApprouvees", approuve);
         stats.put("demandesRefusees", refuse);

         return ResponseEntity.ok(ApiResponse.success(stats, "Stats workflow"));
     }

    //   ================= CONGES WORKFLOW =================

    @GetMapping("/conges-workflow")
@PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConges(@AuthenticationPrincipal Jwt jwt) {

    String email = extractEmail(jwt);

    List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);

    enrichTasksWithEmployeePhotos(tasks);

    return ResponseEntity.ok(ApiResponse.success(tasks, "Congés workflow"));
}



private void enrichTasksWithEmployeePhotos(List<Map<String, Object>> tasks) {
    if (tasks == null || tasks.isEmpty()) {
        return;
    }

    for (Map<String, Object> task : tasks) {
        if (task == null) {
            continue;
        }

        Employe employe = findEmployeFromTask(task);

        if (employe == null) {
            task.put("photoUrl", null);
            task.put("employePhotoProfil", null);
            task.put("employePhotoUrl", null);
            continue;
        }

        String photo = employe.getPhotoUrl();

        task.put("photoUrl", photo);
        task.put("employePhotoProfil", photo);
        task.put("employePhotoUrl", photo);

        task.putIfAbsent("employeId", employe.getId());
        task.putIfAbsent("employeNom", employe.getNom());
        task.putIfAbsent("employePrenom", employe.getPrenom());
        task.putIfAbsent("employeEmail", employe.getEmail());
    }
}

private Employe findEmployeFromTask(Map<String, Object> task) {

    // ✅ 1) Le plus fiable : retrouver par demandeId
    Long demandeId = extractLong(task.get("demandeId"));

    if (demandeId == null) {
        demandeId = extractLong(task.get("demandeCongeId"));
    }

    if (demandeId == null) {
        demandeId = extractLong(task.get("congeId"));
    }

    if (demandeId == null) {
        demandeId = extractLong(task.get("idDemande"));
    }

    if (demandeId != null) {
        return demandeCongeRepository.findById(demandeId)
                .map(demande -> demande.getEmploye())
                .orElse(null);
    }

    // ✅ 2) Fallback par employeId
    Long employeId = extractLong(task.get("employeId"));

    if (employeId == null) {
        employeId = extractLong(task.get("employeeId"));
    }

    if (employeId == null) {
        employeId = extractLong(task.get("idEmploye"));
    }

    if (employeId != null) {
        return employeRepository.findById(employeId).orElse(null);
    }

    // ✅ 3) Fallback par email
    String email = extractString(task.get("employeEmail"));

    if (email == null || email.isBlank()) {
        email = extractString(task.get("employeeEmail"));
    }

    if (email == null || email.isBlank()) {
        email = extractString(task.get("email"));
    }

    if (email != null && !email.isBlank()) {
        return employeRepository.findByEmailIgnoreCase(email.trim())
                .orElse(null);
    }

    return null;
}

private Long extractLong(Object value) {
    if (value == null) {
        return null;
    }

    if (value instanceof Long longValue) {
        return longValue;
    }

    if (value instanceof Integer integerValue) {
        return integerValue.longValue();
    }

    if (value instanceof Number numberValue) {
        return numberValue.longValue();
    }

    try {
        return Long.parseLong(String.valueOf(value));
    } catch (Exception e) {
        return null;
    }
}

private String extractString(Object value) {
    if (value == null) {
        return null;
    }

    return String.valueOf(value);
}

    //   ================= HELPER =================

    private String extractEmail(Jwt jwt) {

     String email = jwt.getClaimAsString("email");

     if (email == null || email.isBlank()) {
         email = jwt.getClaimAsString("preferred_username");
     }

    if (email == null || email.isBlank()) {
         email = jwt.getSubject();
     }

     return email;
 }
 }