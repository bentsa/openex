import { apiFetch } from './client'

export interface WalletBalance {
  accountId: string
  currency: string
  balance: number
}

export function getWallets(): Promise<WalletBalance[]> {
  return apiFetch('/wallets')
}