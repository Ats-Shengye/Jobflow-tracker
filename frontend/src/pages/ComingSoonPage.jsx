import { Link } from 'react-router'

// まだ中身を作っていない画面の仮の表示
export default function ComingSoonPage({ title, backTo, backLabel, wide = false }) {
  return (
    <main className={`page ${wide ? 'page--wide' : 'page--narrow'}`}>
      <h1 className="page-title">{title}</h1>
      <p className="status-text">準備中</p>
      <p className="page-link">
        <Link to={backTo}>{backLabel}</Link>
      </p>
    </main>
  )
}
