import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PictureService {
  private pictureSubject = new BehaviorSubject<string | null>(null);
  picture$ = this.pictureSubject.asObservable();

  setPicture(url: string | null): void {
    this.pictureSubject.next(PictureService.buildDisplayUrl(url));
  }

  static buildDisplayUrl(url?: string | null): string | null {
    if (!url) {
      return null;
    }

    let cleanUrl = String(url).trim();

    if (!cleanUrl) {
      return null;
    }

    cleanUrl = cleanUrl.split('?')[0];

    if (cleanUrl.startsWith('data:image')) {
      return cleanUrl;
    }

    if (cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) {
      return `${cleanUrl}?t=${Date.now()}`;
    }

    // ✅ Déjà une URL API correcte
    if (cleanUrl.startsWith('/api/')) {
      return `${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('api/')) {
      return `/${cleanUrl}?t=${Date.now()}`;
    }

    // ✅ Ton backend retourne visiblement /photos/xxx.jpg
    if (cleanUrl.startsWith('/photos/')) {
      return `/api${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('photos/')) {
      return `/api/${cleanUrl}?t=${Date.now()}`;
    }

    // ✅ Si jamais backend retourne /uploads/...
    if (cleanUrl.startsWith('/uploads/')) {
      return `/api${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('uploads/')) {
      return `/api/${cleanUrl}?t=${Date.now()}`;
    }

    // ✅ Si backend retourne seulement le nom du fichier
    return `/api/photos/${cleanUrl}?t=${Date.now()}`;
  }
}