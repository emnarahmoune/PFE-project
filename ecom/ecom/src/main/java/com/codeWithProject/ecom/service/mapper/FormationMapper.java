package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.entity.FormationSupport;
import com.codeWithProject.ecom.entity.FormationVideo;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import com.codeWithProject.ecom.service.dto.FormationSupportDTO;
import com.codeWithProject.ecom.service.dto.FormationVideoDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour l'entité Formation
 */
@Component
public class FormationMapper {

    public FormationDTO toDto(Formation entity) {
        if (entity == null) {
            return null;
        }

        FormationDTO dto = new FormationDTO();

        dto.setId(entity.getId());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        dto.setDomaine(entity.getDomaine());
        dto.setDureeHeures(entity.getDureeHeures());
        dto.setActif(entity.getActif());
        dto.setDateCreation(entity.getDateCreation());

        /*
         * IMPORTANT :
         * On ne met PAS pdfPath ici parce que Formation n'a pas getPdfPath().
         * Les PDF sont maintenant dans entity.getSupports().
         */

        // Nombre participants : on met 0 ici.
        // Le vrai nombre est déjà calculé dans FormationServiceImpl.getAll()
        // ou via getParticipants().
        dto.setNombreParticipants(0);

        if (dto.getParticipantIds() == null) {
            dto.setParticipantIds(new ArrayList<>());
        }

        // ===== VIDÉOS =====
        if (entity.getVideos() != null) {
            List<FormationVideoDTO> videos = entity.getVideos()
                    .stream()
                    .sorted(Comparator.comparingInt(FormationVideo::getOrdre))
                    .map(video -> FormationVideoDTO.builder()
                            .id(video.getId())
                            .titre(video.getTitre())
                            .urlYoutube(video.getUrlYoutube())
                            .ordre(video.getOrdre())
                            .build()
                    )
                    .collect(Collectors.toList());

            dto.setVideos(videos);
        } else {
            dto.setVideos(new ArrayList<>());
        }

        // ===== SUPPORTS PDF =====
        if (entity.getSupports() != null) {
            List<FormationSupportDTO> supports = entity.getSupports()
                    .stream()
                    .sorted(Comparator.comparingInt(FormationSupport::getOrdre))
                    .map(support -> FormationSupportDTO.builder()
                            .id(support.getId())
                            .titre(support.getTitre())
                            .fichierUrl(support.getFichierUrl())
                            .ordre(support.getOrdre())
                            .build()
                    )
                    .collect(Collectors.toList());

            dto.setSupports(supports);
        } else {
            dto.setSupports(new ArrayList<>());
        }

        // ===== RÉSUMÉ =====
        int duree = entity.getDureeHeures() != null ? entity.getDureeHeures() : 0;
        int participants = dto.getNombreParticipants();

        dto.setResume(String.format(
                "%s (%dh) - %d participants",
                entity.getTitre(),
                duree,
                participants
        ));

        return dto;
    }

    public Formation toEntity(FormationDTO dto) {
        if (dto == null) {
            return null;
        }

        Formation formation = Formation.builder()
                .id(dto.getId())
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .domaine(dto.getDomaine())
                .dureeHeures(dto.getDureeHeures())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();

        /*
         * IMPORTANT :
         * On ne met PAS .pdfPath(...)
         * parce que FormationBuilder n'a pas de méthode pdfPath().
         */

        // ===== VIDÉOS =====
        List<FormationVideo> videos = new ArrayList<>();

        if (dto.getVideos() != null) {
            for (int i = 0; i < dto.getVideos().size(); i++) {
                FormationVideoDTO videoDTO = dto.getVideos().get(i);

                FormationVideo video = FormationVideo.builder()
                        .id(videoDTO.getId())
                        .titre(videoDTO.getTitre())
                        .urlYoutube(videoDTO.getUrlYoutube())
                        .ordre(videoDTO.getOrdre() != null ? videoDTO.getOrdre() : i + 1)
                        .formation(formation)
                        .build();

                videos.add(video);
            }
        }

        formation.setVideos(new java.util.HashSet<>(videos));

        // ===== SUPPORTS PDF =====
        List<FormationSupport> supports = new ArrayList<>();

        if (dto.getSupports() != null) {
            for (int i = 0; i < dto.getSupports().size(); i++) {
                FormationSupportDTO supportDTO = dto.getSupports().get(i);

                FormationSupport support = FormationSupport.builder()
                        .id(supportDTO.getId())
                        .titre(supportDTO.getTitre())
                        .fichierUrl(supportDTO.getFichierUrl())
                        .ordre(supportDTO.getOrdre() != null ? supportDTO.getOrdre() : i + 1)
                        .formation(formation)
                        .build();

                supports.add(support);
            }
        }

        formation.setSupports(new java.util.HashSet<>(supports));

        return formation;
    }
}