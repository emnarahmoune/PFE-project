import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';

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
    if (!url) return null;

    let cleanUrl = String(url).trim();
    if (!cleanUrl) return null;

    cleanUrl = cleanUrl.split('?')[0];

    if (cleanUrl.startsWith('data:image')) {
      return cleanUrl;
    }

    if (cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) {
      return `${cleanUrl}?t=${Date.now()}`;
    }

    const baseApi = environment.apiUrl;

    if (cleanUrl.startsWith('/api/')) {
      return `${baseApi}${cleanUrl.replace('/api', '')}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('api/')) {
      return `${baseApi}/${cleanUrl.replace('api/', '')}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('/photos/')) {
      return `${baseApi}${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('photos/')) {
      return `${baseApi}/${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('/uploads/')) {
      return `${baseApi}${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('uploads/')) {
      return `${baseApi}/${cleanUrl}?t=${Date.now()}`;
    }

    return `${baseApi}/photos/${cleanUrl}?t=${Date.now()}`;
  }
}