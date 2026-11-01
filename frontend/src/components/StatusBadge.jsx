import { isKnownStatus, statusLabel } from '../format.js'
import './StatusBadge.css'

// 色だけで区別しないよう、ラベルの文字は必ず出す
export default function StatusBadge({ status, interviewRound }) {
  const modifier = isKnownStatus(status) ? status.toLowerCase().replaceAll('_', '-') : 'unknown'
  return <span className={`status-badge status-badge--${modifier}`}>{statusLabel(status, interviewRound)}</span>
}
