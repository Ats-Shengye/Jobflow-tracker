import { useId } from 'react'

// ラベル・注記・エラーを入力欄に結び付け、読み上げでも項目ごとのエラーが分かるようにする。multiline で複数行の入力欄にする
export default function TextField({ label, hint, error, multiline = false, ...inputProps }) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ')
  const Input = multiline ? 'textarea' : 'input'

  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      {hint && (
        <p className="field__hint" id={hintId}>
          {hint}
        </p>
      )}
      <Input
        className={`field__input${multiline ? ' field__input--multiline' : ''}`}
        id={id}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy || undefined}
        {...inputProps}
      />
      {error && (
        <p className="field__error" id={errorId}>
          {error}
        </p>
      )}
    </div>
  )
}
