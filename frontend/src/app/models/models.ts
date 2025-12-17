export interface User {
  id: number;
  keycloakId: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface Account {
  id?: number;
  name: string;
  type: AccountType;
  balance: number;
  currency: string;
  description?: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export enum AccountType {
  CHECKING = 'CHECKING',
  SAVINGS = 'SAVINGS',
  CREDIT_CARD = 'CREDIT_CARD',
  INVESTMENT = 'INVESTMENT',
  CASH = 'CASH'
}

export interface Transaction {
  id?: number;
  accountId: number;
  categoryId?: number;
  type: TransactionType;
  amount: number;
  transactionDate: string;
  description?: string;
  payee?: string;
  notes?: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export enum TransactionType {
  INCOME = 'INCOME',
  EXPENSE = 'EXPENSE',
  TRANSFER = 'TRANSFER'
}

export interface Category {
  id?: number;
  name: string;
  type: CategoryType;
  color?: string;
  icon?: string;
  description?: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export enum CategoryType {
  INCOME = 'INCOME',
  EXPENSE = 'EXPENSE'
}

export interface Budget {
  id?: number;
  categoryId: number;
  name: string;
  amount: number;
  startDate: string;
  endDate: string;
  period: BudgetPeriod;
  createdAt?: Date;
  updatedAt?: Date;
}

export enum BudgetPeriod {
  WEEKLY = 'WEEKLY',
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  YEARLY = 'YEARLY'
}
