import { apiFetch } from './client'

export interface WalletBalance {
  accountId: string
  currency: string
  balance: number
}

export interface DepositRequest {
  accountId: string
  amount: number
}

export interface DepositResponse {
  transactionId: string
  accountId: string
  newBalance: number
}

export function getWallets(): Promise<WalletBalance[]> {
  return apiFetch('/wallets')
}

export function deposit(request: DepositRequest): Promise<DepositResponse> {
  return apiFetch('/wallets/deposit', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}