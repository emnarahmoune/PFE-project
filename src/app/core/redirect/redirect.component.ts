import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { keycloakService } from '../services/keycloak-init.service';

@Component({
  standalone: true,
  template: `<p>Redirection...</p>`
})
export class RedirectComponent implements OnInit {

  constructor(private router: Router) {}

  ngOnInit(): void {

    const token = keycloakService.getToken();
    const roles = keycloakService.getRoles();

    console.log("TOKEN:", token);
    console.log("ROLES:", roles);

    if (!token) {
      this.router.navigate(['/auth/login']);
      return;
    }

    // 🔥 CORRECTION IMPORTANTE (ordre + debug)
    if (roles.includes('admin')) {
      console.log("➡️ REDIRECT ADMIN");
      this.router.navigateByUrl('/admin/dashboard');
      return;
    }

    if (roles.includes('manager')) {
      console.log("➡️ REDIRECT MANAGER");
      this.router.navigateByUrl('/manager/dashboard');
      return;
    }

    // fallback
    this.router.navigate(['/auth/login']);
  }
}