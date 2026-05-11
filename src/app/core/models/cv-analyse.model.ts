export interface CvAnalyse {
  id?: number;

  candidatureId?: number;
  offreId?: number;
  employeId?: number;

  cvFileName?: string;
  cvContentType?: string;

  texteExtrait?: string;

  competencesDetectees: string[];
  technologiesDetectees: string[];
  experiencesDetectees?: string[];

  anneesExperienceEstimees?: number;

  resumeProfil?: string;
  pointsForts?: string[];
  pointsFaibles?: string[];

  dateAnalyse?: string;
}

export interface CvAnalyseRequest {
  offreId: number;
  employeId?: number;
  cvFile: File;
}

export interface CvAnalyseResponse {
  success: boolean;
  message?: string;
  data: CvAnalyse;
}