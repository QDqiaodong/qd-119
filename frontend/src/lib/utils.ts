import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export const SHELF_POSITION_PATTERN = /^[A-Z]-\d{2}-\d{2}$/

export const SHELF_POSITION_HINT = '格式：A-01-02（字母-两位数字-两位数字）'

export function isValidShelfPosition(position: string | null | undefined): boolean {
  if (!position || !position.trim()) {
    return true
  }
  return SHELF_POSITION_PATTERN.test(position.trim())
}

export function validateShelfPosition(position: string | null | undefined, fieldName: string = '货架位置'): string | null {
  if (!isValidShelfPosition(position)) {
    return `${fieldName}${SHELF_POSITION_HINT}`
  }
  return null
}
