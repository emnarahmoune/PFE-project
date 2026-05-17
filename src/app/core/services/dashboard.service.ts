import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, forkJoin, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardStats {
  employesActifs: number;
  totalEmployes: number;
  turnover: number;
  absenteisme: number;
  demandesConge: number;
  formationsEnCours: number;
  scoresRisque: number;
  masseSalariale: number;
  salaireMoyen: number;
  parDepartement: { [key: string]: number };
  parStatut: { [key: string]: number };
}

export interface EmployeRecent {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  poste: string;
  dateEmbauche: string;
  statut: string;
  departement: string;
  salaire?: number;
}

export interface Alerte {
  type: 'danger' | 'warning' | 'info' | 'success';
  message: string;
  time: string;
  lien?: string;
  id?: number;
}

export interface Competence {
  id: number;
  nom: string;
  description?: string;
  categorie: string;
  pourcentage: number;
  count: number;
  niveauMoyen?: number;
}

export interface TurnoverData {
  labels: string[];
  data: number[];
}

export interface PerformanceData {
  employeId: number;
  employeNom: string;
  score: number;
  date: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {
    console.log('📊 DashboardService initialisé avec API:', this.apiUrl);
  }

  /**
   * Récupère toutes les statistiques du dashboard depuis l'API
   */
  getDashboardStats(): Observable<DashboardStats> {
    return forkJoin({
      employes: this.http.get<any>(`${this.apiUrl}/employes/stats/tableau-bord`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur stats employés, utilisation valeurs par défaut', err);
          return of({ data: { employesActifs: 0, totalEmployes: 0, masseSalariale: 0, salaireMoyen: 0 } });
        })
      ),
      turnover: this.http.get<any>(`${this.apiUrl}/indicateurs/type/TURNOVER`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur turnover, utilisation valeurs par défaut', err);
          return of({ data: [{ valeur: 0 }] });
        })
      ),
      absenteisme: this.http.get<any>(`${this.apiUrl}/indicateurs/type/ABSENTEISME`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur absentéisme, utilisation valeurs par défaut', err);
          return of({ data: [{ valeur: 0 }] });
        })
      ),
      conges: this.http.get<any>(`${this.apiUrl}/conges/stats/statut`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur stats congés, utilisation valeurs par défaut', err);
          return of({ data: { EN_ATTENTE: 0 } });
        })
      ),
      formations: this.http.get<any>(`${this.apiUrl}/formations/stats/tableau-bord`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur stats formations, utilisation valeurs par défaut', err);
          return of({ data: { totalParticipants: 0 } });
        })
      ),
      scores: this.http.get<any>(`${this.apiUrl}/scores-turnover/stats/repartition-risques`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur scores risque, utilisation valeurs par défaut', err);
          return of({ data: { risqueEleve: 0, risqueCritique: 0 } });
        })
      ),
      departements: this.http.get<any>(`${this.apiUrl}/employes/stats/departement`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur stats départements, utilisation valeurs par défaut', err);
          return of({ data: [] });
        })
      ),
      statuts: this.http.get<any>(`${this.apiUrl}/employes/stats/statut`).pipe(
        catchError(err => {
          console.warn('⚠️ Erreur stats statuts, utilisation valeurs par défaut', err);
          return of({ data: {} });
        })
      )
    }).pipe(
      map((data: any) => {
        const employesStats = data.employes.data || {};
        const turnoverData = Array.isArray(data.turnover.data) ? data.turnover.data[0] : { valeur: 0 };
        const absenteismeData = Array.isArray(data.absenteisme.data) ? data.absenteisme.data[0] : { valeur: 0 };
        const congesData = data.conges.data || { EN_ATTENTE: 0 };
        const formationsData = data.formations.data || { totalParticipants: 0 };
        const scoresData = data.scores.data || {};
        const departementsData = data.departements.data || [];
        const statutsData = data.statuts.data || {};

        const parDepartement: { [key: string]: number } = {};
        departementsData.forEach((item: any[]) => {
          if (item.length >= 2) {
            parDepartement[item[0] || 'Inconnu'] = item[1] || 0;
          }
        });

        return {
          employesActifs: employesStats.employesActifs || 0,
          totalEmployes: employesStats.totalEmployes || 0,
          turnover: parseFloat(turnoverData.valeur) || 0,
          absenteisme: parseFloat(absenteismeData.valeur) || 0,
          demandesConge: congesData.EN_ATTENTE || 0,
          formationsEnCours: formationsData.totalParticipants || 0,
          scoresRisque: (scoresData.risqueEleve || 0) + (scoresData.risqueCritique || 0),
          masseSalariale: employesStats.masseSalariale || 0,
          salaireMoyen: employesStats.salaireMoyen || 0,
          parDepartement: parDepartement,
          parStatut: statutsData
        };
      })
    );
  }

  /**
   * Récupère les employés récents depuis l'API
   */
  getEmployesRecents(limit: number = 5): Observable<EmployeRecent[]> {
    return this.http.get<any>(`${this.apiUrl}/employes/recents?limit=${limit}`).pipe(
      map(response => {
        const employes = response.data || [];
        return employes.map((emp: any) => ({
          id: emp.id || 0,
          matricule: emp.matricule || '',
          nom: emp.nom || '',
          prenom: emp.prenom || '',
          email: emp.email || '',
          poste: emp.poste || '',
          dateEmbauche: emp.dateEmbauche || '',
          statut: emp.statut || 'ACTIF',
          departement: emp.departement || '',
          salaire: emp.salaire || 0
        }));
      }),
      catchError(err => {
        console.error('❌ Erreur chargement employés récents:', err);
        return of([]);
      })
    );
  }

  /**
   * Récupère les alertes dynamiques basées sur les données réelles
   */
  getAlertes(): Observable<Alerte[]> {
    return forkJoin({
      conges: this.http.get<any>(`${this.apiUrl}/conges/urgentes`).pipe(
        catchError(err => of({ data: [] }))
      ),
      scores: this.http.get<any>(`${this.apiUrl}/scores-turnover/stats/repartition-risques`).pipe(
        catchError(err => of({ data: [] }))
      ),
      formations: this.http.get<any>(`${this.apiUrl}/formations/populaires?limit=1`).pipe(
        catchError(err => of({ data: [] }))
      ),
      employes: this.http.get<any>(`${this.apiUrl}/employes/actifs?page=0&size=20`).pipe(
        catchError(err => of({ data: [] }))
      )
    }).pipe(
      map((data: any) => {
        const alertes: Alerte[] = [];
        
        const congesUrgents = data.conges.data || [];
        if (congesUrgents.length > 0) {
          alertes.push({
            type: 'warning',
            message: `${congesUrgents.length} demande(s) de congé ${congesUrgents.length > 1 ? 'urgentes' : 'urgente'}`,
            time: this.getRelativeTime(new Date()),
            lien: '/admin/conges?urgent=true'
          });
        }

        const scoresRisques = data.scores.data || [];
        const scoresEleves = scoresRisques.filter((s: any) => 
          s.niveauRisque === 'ELEVE' || s.niveauRisque === 'CRITIQUE'
        );
        if (scoresEleves.length > 0) {
          alertes.push({
            type: 'danger',
            message: `${scoresEleves.length} employé(s) avec risque de turnover ${scoresEleves.length > 1 ? 'élevé' : 'critique'}`,
            time: this.getRelativeTime(new Date(Date.now() - 3600000)),
            lien: '/admin/scores'
          });
        }

        const formations = data.formations.data || [];
        if (formations.length > 0) {
          const prochaineFormation = formations[0];
          alertes.push({
            type: 'info',
            message: `Formation "${prochaineFormation.titre || 'à venir'}" commence bientôt`,
            time: this.getRelativeTime(new Date(Date.now() - 7200000)),
            lien: '/admin/formations'
          });
        }

        const employes = data.employes.data || [];
        const aujourdHui = new Date();
        const employesAnniversaire = employes.filter((e: any) => {
          if (!e.dateEmbauche) return false;
          const dateEmbauche = new Date(e.dateEmbauche);
          return dateEmbauche.getDate() === aujourdHui.getDate() && 
                 dateEmbauche.getMonth() === aujourdHui.getMonth();
        });

        if (employesAnniversaire.length > 0) {
          alertes.push({
            type: 'success',
            message: `${employesAnniversaire.length} employé(s) fêtent leur anniversaire d'embauche aujourd'hui !`,
            time: 'Aujourd\'hui',
            lien: '/admin/employes'
          });
        }

        return alertes;
      })
    );
  }

  /**
   * Récupère les données de turnover pour le graphique
   */
  getTurnoverData(): Observable<TurnoverData> {
    return this.http.get<any>(`${this.apiUrl}/indicateurs/type/TURNOVER`).pipe(
      map(response => {
        const indicateurs = response.data || [];
        const derniersMois = indicateurs.slice(-6);
        
        return {
          labels: derniersMois.map((i: any) => {
            if (i.periode) return i.periode;
            if (i.dateCalcul) {
              const date = new Date(i.dateCalcul);
              return date.toLocaleString('fr-FR', { month: 'short' });
            }
            return 'N/A';
          }),
          data: derniersMois.map((i: any) => i.valeur || 0)
        };
      }),
      catchError(err => {
        console.warn('⚠️ Erreur chargement turnover, données mockées', err);
        return of({
          labels: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin'],
          data: [5.2, 6.1, 4.8, 7.2, 8.5, 7.9]
        });
      })
    );
  }

  /**
   * Récupère la répartition des employés par statut
   */
  getRepartitionEmployes(): Observable<{ labels: string[]; data: number[] }> {
    return this.http.get<any>(`${this.apiUrl}/employes/stats/statut`).pipe(
      map(response => {
        const stats = response.data || {};
        return {
          labels: Object.keys(stats).map(key => this.getStatutLabel(key)),
          data: Object.values(stats) as number[]
        };
      }),
      catchError(err => {
        console.warn('⚠️ Erreur chargement répartition, données mockées', err);
        return of({
          labels: ['Actifs', 'Congé', 'Inactifs'],
          data: [120, 25, 11]
        });
      })
    );
  }

  /**
   * Récupère les top compétences
   */
  getTopCompetences(limit: number = 5): Observable<Competence[]> {
    return this.http.get<any>(`${this.apiUrl}/competences/top/${limit}`).pipe(
      map(response => {
        const competences = response.data || [];
        const maxCount = Math.max(...competences.map((c: any) => c.nombreEmployes || 0));
        
        return competences.map((c: any) => ({
          id: c.id || 0,
          nom: c.nom || '',
          description: c.description || '',
          categorie: c.categorie || '',
          pourcentage: maxCount > 0 ? Math.round(((c.nombreEmployes || 0) / maxCount) * 100) : 0,
          count: c.nombreEmployes || 0,
          niveauMoyen: c.niveauMoyen || 0
        }));
      }),
      catchError(err => {
        console.warn('⚠️ Erreur chargement compétences, données mockées', err);
        return of([
          { id: 1, nom: 'Java', categorie: 'TECHNIQUE', pourcentage: 85, count: 42, niveauMoyen: 3 },
          { id: 2, nom: 'Spring Boot', categorie: 'TECHNIQUE', pourcentage: 72, count: 36, niveauMoyen: 3 },
          { id: 3, nom: 'Angular', categorie: 'TECHNIQUE', pourcentage: 68, count: 34, niveauMoyen: 2 }
        ]);
      })
    );
  }

  /**
   * Récupère les données de performance
   */
  getPerformanceData(): Observable<PerformanceData[]> {
    return this.http.get<any>(`${this.apiUrl}/indicateurs/type/PERFORMANCE`).pipe(
      map(response => response.data || []),
      catchError(err => {
        console.warn('⚠️ Erreur chargement performance, données mockées', err);
        return of([]);
      })
    );
  }

  /**
   * Exporte les données du dashboard
   */
  exporterRapport(): void {
    console.log('📥 Export du rapport...');
    window.open(`${this.apiUrl}/employes/export`, '_blank');
  }

  /**
   * Rafraîchit les données en invalidant le cache
   */
  rafraichirDonnees(): Observable<boolean> {
    return this.http.get<any>(`${this.apiUrl}/employes/refresh`).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  /**
   * Calcule le temps relatif pour les alertes
   */
  private getRelativeTime(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    if (diffDays === 1) return 'Hier';
    return `Il y a ${diffDays} jours`;
  }

  /**
   * Convertit un code statut en label lisible
   */
  private getStatutLabel(statut: string): string {
    const labels: { [key: string]: string } = {
      'ACTIF': 'Actifs',
      'CONGE': 'Congé',
      'INACTIF': 'Inactifs',
      'EN_CONGE': 'En congé'
    };
    return labels[statut] || statut;
  }
}