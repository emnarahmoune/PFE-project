// core/services/picture.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PictureService {
  private rawUrlSubject = new BehaviorSubject<string | null>(null);

  /**
   * Émet l'URL brute sans timestamp.
   * Chaque composant abonné ajoute son propre timestamp via buildDisplayUrl().
   */
  picture$: Observable<string | null> = this.rawUrlSubject.asObservable();

  setPicture(rawUrl: string | null): void {
    this.rawUrlSubject.next(rawUrl || null);
  }

  /**
   * Construit une URL prête à l'affichage avec cache-busting.
   * À appeler dans les subscribers, pas dans setPicture().
   */
  static buildDisplayUrl(rawUrl: string | null): string | null {
    if (!rawUrl) return null;
    const sep = rawUrl.includes('?') ? '&' : '?';
    return `${rawUrl}${sep}t=${Date.now()}`;
  }
}