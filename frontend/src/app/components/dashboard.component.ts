import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../services/account.service';
import { TransactionService } from '../../services/transaction.service';
import { Account, Transaction } from '../../models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard">
      <h1>Personal Finance Dashboard</h1>
      
      <div class="summary-cards">
        <div class="card">
          <h3>Total Balance</h3>
          <p class="amount">\${{ totalBalance.toFixed(2) }}</p>
        </div>
        <div class="card">
          <h3>Accounts</h3>
          <p class="count">{{ accounts.length }}</p>
        </div>
        <div class="card">
          <h3>Transactions</h3>
          <p class="count">{{ transactions.length }}</p>
        </div>
      </div>

      <div class="accounts-section">
        <h2>Your Accounts</h2>
        <div class="accounts-list">
          <div *ngFor="let account of accounts" class="account-card">
            <h3>{{ account.name }}</h3>
            <p>{{ account.type }}</p>
            <p class="balance">\${{ account.balance.toFixed(2) }}</p>
          </div>
        </div>
      </div>

      <div class="transactions-section">
        <h2>Recent Transactions</h2>
        <table class="transactions-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Description</th>
              <th>Type</th>
              <th>Amount</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let transaction of recentTransactions">
              <td>{{ transaction.transactionDate }}</td>
              <td>{{ transaction.description || 'N/A' }}</td>
              <td>{{ transaction.type }}</td>
              <td [class.income]="transaction.type === 'INCOME'" 
                  [class.expense]="transaction.type === 'EXPENSE'">
                \${{ transaction.amount.toFixed(2) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 20px;
    }

    .summary-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
      margin-bottom: 30px;
    }

    .card {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .card h3 {
      margin: 0 0 10px 0;
      color: #666;
      font-size: 14px;
    }

    .amount, .count {
      font-size: 32px;
      font-weight: bold;
      color: #333;
      margin: 0;
    }

    .accounts-section, .transactions-section {
      margin-bottom: 30px;
    }

    .accounts-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 15px;
    }

    .account-card {
      background: white;
      padding: 15px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .account-card h3 {
      margin: 0 0 5px 0;
      font-size: 16px;
    }

    .account-card p {
      margin: 5px 0;
      color: #666;
    }

    .balance {
      font-size: 20px;
      font-weight: bold;
      color: #2196F3 !important;
    }

    .transactions-table {
      width: 100%;
      background: white;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .transactions-table th,
    .transactions-table td {
      padding: 12px;
      text-align: left;
      border-bottom: 1px solid #eee;
    }

    .transactions-table th {
      background: #f5f5f5;
      font-weight: 600;
    }

    .income {
      color: #4CAF50;
      font-weight: bold;
    }

    .expense {
      color: #f44336;
      font-weight: bold;
    }
  `]
})
export class DashboardComponent implements OnInit {
  accounts: Account[] = [];
  transactions: Transaction[] = [];
  totalBalance = 0;
  recentTransactions: Transaction[] = [];

  constructor(
    private accountService: AccountService,
    private transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
    this.loadTransactions();
  }

  loadAccounts(): void {
    this.accountService.getAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.totalBalance = accounts.reduce((sum, acc) => sum + acc.balance, 0);
      },
      error: (error) => console.error('Error loading accounts:', error)
    });
  }

  loadTransactions(): void {
    this.transactionService.getTransactions().subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.recentTransactions = transactions.slice(0, 10);
      },
      error: (error) => console.error('Error loading transactions:', error)
    });
  }
}
