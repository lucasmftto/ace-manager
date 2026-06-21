import { Component, inject } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
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
    CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule
  ],
  styles: [`
    .nav-link {
      color: rgba(255,255,255,.7);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      padding: 6px 14px;
      border-radius: 6px;
      transition: background .15s, color .15s;
      &:hover { background: rgba(255,255,255,.1); color: white; }
      &.active { background: rgba(255,255,255,.15); color: white; }
    }
    .nav-links { display: flex; gap: 4px; margin-left: 24px; }
  `],
  template: `
    @if (isAuthenticated()) {
      <mat-toolbar class="app-topbar">
        <span class="topbar-title">
          <span class="topbar-accent">ACE</span> Manager
        </span>
        <nav class="nav-links">
          <a class="nav-link" routerLink="/students" routerLinkActive="active">Alunos</a>
          <a class="nav-link" routerLink="/teachers" routerLinkActive="active">Professores</a>
          <a class="nav-link" routerLink="/plans" routerLinkActive="active">Planos</a>
          <a class="nav-link" routerLink="/classes" routerLinkActive="active">Aulas</a>
        </nav>
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
