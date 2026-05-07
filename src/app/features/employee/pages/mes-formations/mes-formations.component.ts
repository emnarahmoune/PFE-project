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
  recommandationsSkill: any[] = [];

  videos: any[] = [];
  completedVideos: number[] = [];

  selectedFormationIndex: number | null = null;
  selectedFormationId: number | null = null;
  revoirMode: boolean = false;

  // Quand cette valeur contient l'id de la formation,
  // les vidéos doivent redevenir non terminées dans l'affichage.
  revoirModeFormationId: number | null = null;

  constructor(
    private formationService: FormationService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.loadMesFormations();
    this.loadRecommandations();
    this.loadRecommandationsSkill();
  }

  loadMesFormations() {
    this.formationService.getMyFormations()
      .subscribe(data => {
        this.formationsSuivies = data;
      });
  }

  loadRecommandations() {
    this.formationService.getRecommendations()
      .subscribe(data => {
        console.log("RECO:", data);
        this.recommandations = data;
      });
  }

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

      if (videoId.includes('&')) {
        videoId = videoId.split('&')[0];
      }

    } else if (url.includes('youtu.be/')) {
      videoId = url.split('youtu.be/')[1];

      if (videoId.includes('?')) {
        videoId = videoId.split('?')[0];
      }
    }

    if (!videoId) {
      return null;
    }

    return this.sanitizer.bypassSecurityTrustResourceUrl(
      'https://www.youtube.com/embed/' + videoId
    );
  }

 handleFormationButton(ef: any, index: number) {

  if (!ef.formation?.id) {
    return;
  }

  if (this.selectedFormationIndex === index) {
    this.selectedFormationIndex = null;
    this.selectedFormationId = null;
    this.videos = [];
    this.completedVideos = [];
    this.revoirMode = false;
    return;
  }

  if (ef.progression === 100) {
    this.revoirFormation(ef, index);
  } else {
    this.voirFormation(ef.formation.id, index);
  }
}

voirFormation(formationId: number, index: number) {

  this.revoirMode = false;

  this.selectedFormationIndex = index;
  this.selectedFormationId = formationId;

  this.videos = [];
  this.completedVideos = [];

  this.formationService.getVideos(formationId)
    .subscribe(videosData => {

      this.formationService.getCompletedVideos(formationId)
        .subscribe(ids => {

          this.completedVideos = ids.map((id: any) => Number(id));

          this.videos = videosData.map((v: any) => ({
            ...v,
            completedView: this.completedVideos.includes(Number(v.id))
          }));

        });

    });
}


revoirFormation(ef: any, index: number) {

  const formationId = ef.formation.id;

  this.formationService.resetFormationProgress(formationId)
    .subscribe({
      next: () => {

        ef.progression = 0;

        this.selectedFormationIndex = index;
        this.selectedFormationId = formationId;

        this.completedVideos = [];
        this.videos = [];

        this.formationService.getVideos(formationId)
          .subscribe(videosData => {

            this.videos = videosData.map((v: any) => ({
              ...v,
              completedView: false
            }));

            this.completedVideos = [];
          });

        alert("Formation réinitialisée ✅ Vous pouvez la revoir depuis le début");
      },
      error: (err) => {
        console.error("Erreur reset formation =", err);
        alert("Erreur lors de la réinitialisation ❌");
      }
    });
}
  isRevoirMode(): boolean {
  return String(this.selectedFormationId) === String(this.revoirModeFormationId);
}

isVideoCompleted(videoId: number): boolean {
  if (this.isRevoirMode()) {
    return false;
  }

  return this.completedVideos.map(id => String(id)).includes(String(videoId));
}

completeVideo(video: any, ef: any) {

  this.formationService.completeVideo(video.id)
    .subscribe({
      next: () => {

        // Marquer seulement cette vidéo
        video.completedView = true;

        if (!this.completedVideos.includes(Number(video.id))) {
          this.completedVideos.push(Number(video.id));
        }

        // Recalcul local de la progression
        const totalVideos = this.videos.length;
        const completedCount = this.videos.filter(v => v.completedView).length;

        if (totalVideos > 0) {
          ef.progression = Math.round((completedCount / totalVideos) * 100);
        }

        // Ne pas appeler loadMesFormations ici,
        // sinon le backend peut renvoyer directement 100%.
        // this.loadMesFormations();
      },
      error: (err) => {
        console.error(err);
      }
    });
}
  inscrire(formation: any) {

    this.formationService.inscrireFormation(formation.id)
      .subscribe({
        next: () => {
          alert("Inscription réussie ✅");

          this.recommandations = this.recommandations.filter(
            f => f.id !== formation.id
          );

          this.recommandationsSkill = this.recommandationsSkill.filter(
            f => f.id !== formation.id
          );

          this.loadMesFormations();
          this.loadRecommandations();
          this.loadRecommandationsSkill();
        },
        error: (err) => {
          console.error(err);

          if (err.status === 200) {
            alert("Inscription réussie ✅");

            this.recommandations = this.recommandations.filter(
              f => f.id !== formation.id
            );

            this.recommandationsSkill = this.recommandationsSkill.filter(
              f => f.id !== formation.id
            );

            this.loadMesFormations();
            this.loadRecommandations();
            this.loadRecommandationsSkill();
          } else {
            alert("Erreur inscription ❌");
          }
        }
      });
  }

  generateCertificate(formationId: number) {

    this.formationService.generateCertificate(formationId)
      .subscribe({
        next: (blob: Blob) => {

          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');

          a.href = url;
          a.download = 'certificat.pdf';
          a.click();

          window.URL.revokeObjectURL(url);
        },
        error: (err) => {
          console.error(err);
          alert("Erreur génération certificat ❌");
        }
      });
  }

}