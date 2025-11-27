import { useRef } from 'react'
import Modal from './Modal'

const LoginModal = ({ isOpen, onClose, onSubmit, language, translations }) => {
  const passwordInputRef = useRef(null)

  const handleUsernameKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      passwordInputRef.current?.focus()
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="w-96 mx-auto">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">{translations[language].loginTitle}</h2>
        <form onSubmit={onSubmit}>
          <div className="mb-4">
            <label className="block text-slate-300 mb-2">{translations[language].idLabel}</label>
            <input
              type="text"
              name="username"
              className="w-full p-2 rounded bg-slate-700 text-white border border-slate-600 focus:outline-none focus:border-blue-500"
              placeholder={translations[language].idPlaceholder}
              onKeyDown={handleUsernameKeyDown}
              required
            />
          </div>
          <div className="mb-6">
            <label className="block text-slate-300 mb-2">{translations[language].pwdLabel}</label>
            <input
              type="password"
              name="password"
              ref={passwordInputRef}
              className="w-full p-2 rounded bg-slate-700 text-white border border-slate-600 focus:outline-none focus:border-blue-500"
              placeholder={translations[language].pwdPlaceholder}
              required
            />
          </div>
          <div className="flex gap-2">
            <button
              type="submit"
              className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
            >
              {translations[language].login}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded transition-colors"
            >
              {translations[language].cancel}
            </button>
          </div>
        </form>
      </div>
    </Modal>
  )
}

export default LoginModal
