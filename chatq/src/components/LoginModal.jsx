import { useRef } from 'react'
import Modal from './Modal'

const LoginModal = ({ isOpen, onClose, onSubmit, language, translations, isLoading }) => {
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
              disabled={isLoading}
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
              disabled={isLoading}
              required
            />
          </div>
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={isLoading}
              className={`flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors flex items-center justify-center ${isLoading ? 'cursor-not-allowed opacity-70' : ''}`}
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin h-5 w-5 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {translations[language].loggingIn || '로그인 중...'}
                </>
              ) : (
                translations[language].login
              )}
            </button>
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className={`flex-1 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded transition-colors ${isLoading ? 'cursor-not-allowed opacity-50' : ''}`}
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
