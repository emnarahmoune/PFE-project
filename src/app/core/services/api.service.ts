import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private baseUrl = environment.apiUrl;


  constructor(
    private http: HttpClient,
    private keycloak: KeycloakService
  ) {}

  // 🔥 HEADERS SYNC
private getHeaders(): HttpHeaders {
  const token = this.keycloak.getKeycloakInstance().token;

  console.log("TOKEN ENVOYÉ =", token); // 🔥 DEBUG

  if (!token) {
    console.error("❌ TOKEN NULL !");
    return new HttpHeaders();
  }

  return new HttpHeaders({
    Authorization: `Bearer ${token}`
  });
}
  // 🔥 GET
  get<T>(endpoint: string, params?: any): Observable<T> {
    const httpParams = new HttpParams({ fromObject: params || {} });

    return this.http.get<T>(`${this.baseUrl}/${endpoint}`, {
      headers: this.getHeaders(),
      params: httpParams
    });
  }

  patch(endpoint: string, data: any): Observable<any> {
  return this.http.patch(`${this.baseUrl}/${endpoint}`, data, {
    headers: this.getHeaders()
  });
}

putSimple(endpoint: string, data: any): Observable<any> {
  return this.http.put(`${this.baseUrl}/${endpoint}`, data, {
    headers: this.getHeaders()
  });
}
  // 🔥 GET BY ID
  getById<T>(endpoint: string, id: number): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}/${endpoint}/${id}`, {
      headers: this.getHeaders()
    });
  }

  // 🔥 POST
  post<T>(endpoint: string, data: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}/${endpoint}`, data, {
      headers: this.getHeaders()
    });
  }


  // 🔥 PUT
  put<T>(endpoint: string, id: number, data: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/${endpoint}/${id}`, data, {
      headers: this.getHeaders()
    });
  }

  // 🔥 PUT CUSTOM (sans id)
  putCustom<T>(endpoint: string, data: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/${endpoint}`, data, {
      headers: this.getHeaders()
    });
  }

  // 🔥 DELETE
  delete<T>(endpoint: string, id: number): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}/${endpoint}/${id}`, {
      headers: this.getHeaders()
    });
  }
}