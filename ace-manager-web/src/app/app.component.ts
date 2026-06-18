import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet,
    MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule
  ],
  template: `
    @if (isAuthenticated()) {
      <mat-toolbar class="app-topbar">
        <span class="topbar-title">
          <span class="topbar-accent">ACE</span> Manager
        </span>
        <span style="flex:1"></span>
        <button mat-icon-button (click)="logout()" matTooltip="Sair">
          <mat-icon>logout</mat-icon>
        </button>
      </mat-toolbar>

      <main style="min-height: calc(100vh - 64px)">
        <router-outlet />
      </main>
    } @else {
      <router-outlet />
    }
  `
})
export class AppComponent {
  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);

  isAuthenticated = () => this.auth.isAuthenticated();

  logout(): void {
    this.auth.logout();
  }
}
