import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormationService } from '../../../../core/services/formation.service';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'app-mes-formations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mes-formations.component.html',
  styleUrls: ['./mes-formations.component.css']
})
export class MesFormationsComponent implements OnInit {

formationsSuivies: any[] = [];
recommandations: any[] = [];
videos: any[] = [];
completedVideos: number[] = [];
recommandationsSkill: any[] = [];



  constructor(private formationService: FormationService,private sanitizer: DomSanitizer) {}





loadRecommandationsSkill() {
  this.formationService.getRecommendationsSkill()
    .subscribe(data => {
      this.recommandationsSkill = data;
    });
}
getYoutubeEmbedUrl(url: string) {

  if (!url) {
    console.log("URL undefined ❌");
    return null;
  }

  let videoId = '';

  if (url.includes('watch?v=')) {
    videoId = url.split('v=')[1];
  } else if (url.includes('youtu.be/')) {
    videoId = url.split('youtu.be/')[1];
  }

  if (!videoId) return null;

  return this.sanitizer.bypassSecurityTrustResourceUrl(
    'https://www.youtube.com/embed/' + videoId
  );
}

  ngOnInit(): void {
    this.loadMesFormations();
    this.loadRecommandations();
    this.loadRecommandationsSkill();
  }

  // ✅ Mes formations
  loadMesFormations() {
    this.formationService.getMyFormations()
      .subscribe(data => {
        this.formationsSuivies = data;
      });
  }

  // ✅ Recommandations IA
loadRecommandations() {
  this.formationService.getRecommendations()    .subscribe(data => {
      console.log("RECO:", data);
      this.recommandations = data;
    });
}




  getSafeUrl(url: string) {
  if (!url) return null;

  const videoId = url.split('v=')[1];
  if (!videoId) return null;

  const embed = 'https://www.youtube.com/embed/' + videoId;

  return this.sanitizer.bypassSecurityTrustResourceUrl(embed);
}

selectedFormationId: number | null = null;

voirFormation(formationId: number) {

  // 👉 SI déjà ouvert → fermer
  if (this.selectedFormationId === formationId) {
    this.selectedFormationId = null;
    return;
  }

  // 👉 sinon ouvrir
  this.selectedFormationId = formationId;

  // 🔹 charger vidéos
  this.formationService.getVideos(formationId)
    .subscribe(data => {
      this.videos = data;
    });

  // 🔹 vidéos complétées
  this.formationService.getCompletedVideos(formationId)
    .subscribe(ids => {
      this.completedVideos = ids;
    });
}

completeVideo(videoId: number) {

  this.formationService.completeVideo(videoId)
    .subscribe({
      next: () => {

        // 🔥 ajouter direct (sans refresh)
        this.completedVideos.push(videoId);

        // 🔥 MAJ progression
        this.loadMesFormations();

      },
      error: err => console.error(err)
    });
}

  // 🔥 INSCRIPTION CORRIGÉE
  inscrire(formation: any) {

    this.formationService.inscrireFormation(formation.id)
      .subscribe({
        next: () => {
          alert("Inscription réussie ✅");

          this.loadMesFormations();
          setTimeout(() => this.loadMesFormations(), 500);
          this.loadRecommandations();
        },
        error: (err) => {
          console.error(err);
          if (err.status === 200) {
             alert("Inscription réussie (warning) ✅");
             } else {
                alert("Erreur inscription ❌");
               }}
      });
  }
}
