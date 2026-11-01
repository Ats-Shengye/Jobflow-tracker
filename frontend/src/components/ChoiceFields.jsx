import { useId } from 'react'

// TextField と同じく、ラベルとエラーを入力欄に結び付ける。options は { value, label } の配列
export function SelectField({ label, error, options, placeholder, ...selectProps }) {
  const id = useId()
  const errorId = `${id}-error`

  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      <select
        className="field__input"
        id={id}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? errorId : undefined}
        {...selectProps}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && (
        <p className="field__error" id={errorId}>
          {error}
        </p>
      )}
    </div>
  )
}

// ラジオボタンの組は fieldset と legend でまとめ、読み上げで何の選択肢か分かるようにする
export function RadioGroup({ legend, error, name, options, value, onChange, required = false }) {
  const id = useId()
  const errorId = `${id}-error`

  return (
    <fieldset
      className="field field--group"
      role="radiogroup"
      aria-invalid={error ? 'true' : undefined}
      aria-describedby={error ? errorId : undefined}
      aria-required={required ? 'true' : undefined}
    >
      <legend className="field__label">{legend}</legend>
      <div className="field__choices">
        {options.map((option) => (
          <label className="field__choice" key={option.value}>
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={value === option.value}
              onChange={(event) => onChange(event.target.value)}
            />
            {option.label}
          </label>
        ))}
      </div>
      {error && (
        <p className="field__error" id={errorId}>
          {error}
        </p>
      )}
    </fieldset>
  )
}
