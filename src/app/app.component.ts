// src/app/app.component.ts
import { Component, OnInit } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div style="position: fixed; top: 0; left: 0; background: #ff0; padding: 5px; z-index: 9999;">
      URL: {{ currentUrl }}
    </div>
    <router-outlet></router-outlet>
  `
})
export class AppComponent implements OnInit {
  currentUrl = '';
  
  constructor(private router: Router) {
    this.router.events.subscribe(() => {
      this.currentUrl = this.router.url;
    });
  }

  ngOnInit() {
    console.log('✅ AppComponent chargé');
    console.log('📍 Route initiale:', window.location.pathname);
  }
}