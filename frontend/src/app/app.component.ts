import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { DashboardComponent } from './components/dashboard.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, DashboardComponent],
  template: `
    <div class="app-container">
      <header>
        <div class="header-content">
          <h1>💰 Personal Finance App</h1>
          <nav>
            <button *ngIf="!isLoggedIn" (click)="login()" class="btn-primary">Login</button>
            <div *ngIf="isLoggedIn" class="user-menu">
              <span>Welcome, {{ username }}</span>
              <button (click)="logout()" class="btn-secondary">Logout</button>
            </div>
          </nav>
        </div>
      </header>

      <main>
        <app-dashboard *ngIf="isLoggedIn"></app-dashboard>
        <div *ngIf="!isLoggedIn" class="welcome">
          <h2>Welcome to Personal Finance App</h2>
          <p>Please login to manage your finances</p>
          <button (click)="login()" class="btn-primary-large">Get Started</button>
        </div>
      </main>

      <footer>
        <p>&copy; 2025 Personal Finance App. Secured with KeyCloak.</p>
      </footer>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background: #f5f5f5;
    }

    header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 15px 0;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }

    .header-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    h1 {
      margin: 0;
      font-size: 24px;
    }

    nav {
      display: flex;
      align-items: center;
      gap: 15px;
    }

    .user-menu {
      display: flex;
      align-items: center;
      gap: 15px;
    }

    .user-menu span {
      font-weight: 500;
    }

    main {
      flex: 1;
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
      padding: 20px;
    }

    .welcome {
      text-align: center;
      padding: 60px 20px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      margin-top: 60px;
    }

    .welcome h2 {
      color: #333;
      margin-bottom: 15px;
    }

    .welcome p {
      color: #666;
      font-size: 18px;
      margin-bottom: 30px;
    }

    button {
      border: none;
      padding: 10px 20px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.3s;
    }

    .btn-primary {
      background: white;
      color: #667eea;
    }

    .btn-primary:hover {
      background: #f0f0f0;
    }

    .btn-secondary {
      background: rgba(255,255,255,0.2);
      color: white;
    }

    .btn-secondary:hover {
      background: rgba(255,255,255,0.3);
    }

    .btn-primary-large {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 15px 40px;
      font-size: 16px;
    }

    .btn-primary-large:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }

    footer {
      background: #333;
      color: white;
      text-align: center;
      padding: 20px;
      margin-top: 40px;
    }

    footer p {
      margin: 0;
    }
  `]
})
export class AppComponent implements OnInit {
  title = 'Personal Finance App';
  isLoggedIn = false;
  username = '';

  constructor(private keycloakService: KeycloakService) {}

  async ngOnInit() {
    this.isLoggedIn = await this.keycloakService.isLoggedIn();
    if (this.isLoggedIn) {
      const userProfile = await this.keycloakService.loadUserProfile();
      this.username = userProfile.username || userProfile.email || 'User';
    }
  }

  login() {
    this.keycloakService.login();
  }

  logout() {
    this.keycloakService.logout(window.location.origin);
  }
}
