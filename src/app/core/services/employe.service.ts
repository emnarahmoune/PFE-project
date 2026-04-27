import { Injectable } from '@angular/core';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class EmployeService {

  constructor(private api: ApiService) {}


getCompetences(userId: number) {
  return this.api.get(`employes/${userId}/competences`);
}

addCompetence(userId: number, data: any) {
  return this.api.post(`employes/${userId}/competences`, data);
}

updateCompetences(userId: number, data: any) {
  return this.api.put(`employes/${userId}/competences`, userId, data);
}
}