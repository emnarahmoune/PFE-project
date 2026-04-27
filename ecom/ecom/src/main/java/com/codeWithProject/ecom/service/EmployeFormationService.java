package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.*;
import com.codeWithProject.ecom.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.codeWithProject.ecom.repository.EmployeFormationProgressRepository;
import com.codeWithProject.ecom.repository.EmployeFormationRepository;
import com.codeWithProject.ecom.repository.FormationVideoRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.FormationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class EmployeFormationService {

    private final EmployeRepository employeRepository;
    private final FormationRepository formationRepository;
    private final FormationVideoRepository videoRepository;
    private final EmployeFormationRepository efRepository;
    private final EmployeFormationProgressRepository progressRepository;

    public EmployeFormationService(
            EmployeRepository employeRepository,
            FormationRepository formationRepository,
            FormationVideoRepository videoRepository,
            EmployeFormationRepository efRepository,
            EmployeFormationProgressRepository progressRepository
    ) {
        this.employeRepository = employeRepository;
        this.formationRepository = formationRepository;
        this.videoRepository = videoRepository;
        this.efRepository = efRepository;
        this.progressRepository = progressRepository;
    }

    // ✅ 1. INSCRIPTION
public void inscrire(Long employeId, Long formationId) {

    Employe emp = employeRepository.findById(employeId)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    Formation formation = formationRepository.findById(formationId)
            .orElseThrow(() -> new RuntimeException("Formation introuvable"));

    boolean exists = efRepository
        .existsByEmployeIdAndFormationId(employeId, formationId);

    if (exists) {
        throw new RuntimeException("Déjà inscrit");
    }

    EmployeFormation ef = new EmployeFormation();
    ef.setEmploye(emp);
    ef.setFormation(formation);

   efRepository.save(ef);
}

    // ✅ 3. MARQUER VIDÉO COMME TERMINÉE
public void completeVideo(Long empId, Long videoId) {

    FormationVideo video = videoRepository.findById(videoId)
            .orElseThrow(() -> new RuntimeException("Vidéo introuvable"));

    if (video.getFormation() == null) {
        throw new RuntimeException("Formation NULL pour vidéo ❌");
    }

    Long formationId = video.getFormation().getId();

    Optional<EmployeFormation> optionalEf =
            efRepository.findByEmployeIdAndFormationId(empId, formationId);

    if (optionalEf.isEmpty()) {
        throw new RuntimeException("Inscription introuvable ❌");
    }

    EmployeFormation ef = optionalEf.get();

    long total = videoRepository.countByFormationId(formationId);

    long done = progressRepository
            .countByEmployeIdAndVideo_Formation_IdAndCompletedTrue(empId, formationId);

    boolean alreadyDone = progressRepository
            .existsByEmployeIdAndVideo(empId, video);

    if (!alreadyDone) {
        EmployeFormationProgress p = new EmployeFormationProgress();
        p.setEmployeId(empId);
        p.setVideo(video);
        p.setCompleted(true);

        progressRepository.save(p);

        done++;
    }

    int percent = total == 0 ? 0 : (int) ((done * 100) / total);

    ef.setProgression(percent);

    efRepository.save(ef);
}



    // ✅ 2. RÉCUPÉRER VIDÉOS
public List<FormationVideo> getVideos(Long formationId) {
    return videoRepository.findByFormation_IdOrderByOrdre(formationId);
}


    
 

    // ✅ 4. CALCUL PROGRESSION
private void updateProgression(Long empId, Long formationId) {

    // ✅ total = nombre de vidéos de la formation
    long total = videoRepository.countByFormationId(formationId);

    // ✅ done = vidéos complétées
    long done = progressRepository
            .countByEmployeIdAndVideo_Formation_IdAndCompletedTrue(empId, formationId);

    int percent = total == 0 ? 0 : (int) ((done * 100) / total);

    EmployeFormation ef = efRepository.findByEmployeId(empId)
            .stream()
            .filter(e -> e.getFormation().getId().equals(formationId))
            .findFirst()
            .orElseThrow();

    ef.setProgression(percent);
    efRepository.save(ef);
}
}