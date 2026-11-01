import { Link } from 'react-router'

export function LoadingText() {
  return (
    <p className="status-text" role="status">
      読み込み中…
    </p>
  )
}

export function LoadError({ message, onRetry }) {
  return (
    <div className="notice notice--error" role="alert">
      <p>{message}</p>
      <button className="button button--secondary button--small" type="button" onClick={onRetry}>
        再読み込み
      </button>
    </div>
  )
}

export function NotFound({ message, backTo, backLabel }) {
  return (
    <>
      <p className="status-text" role="alert">
        {message}
      </p>
      <p className="page-link">
        <Link to={backTo}>{backLabel}</Link>
      </p>
    </>
  )
}
