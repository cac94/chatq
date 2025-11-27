import { useState } from 'react'
import axios from 'axios'
import Modal from './Modal'

const UserInfoModal = ({ isOpen, onClose, user, userName, auth, level, infos, onLogout, language, translations }) => {
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const handleSavePassword = async () => {
    if (!password) {
      alert(translations[language].pwdEmpty)
      return
    }
    if (password !== confirmPassword) {
      alert(translations[language].pwdMismatch)
      return
    }
    // TODO: 비밀번호 변경 API 호출
    try {
      await axios.put('/api/users/password', { newPassword: password })
      alert(translations[language].pwdChanged)
      setPassword('')
      setConfirmPassword('')
    } catch (error) {
      console.error('Failed to change password:', error)
      alert(translations[language].pwdChangeFail)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="w-96 mx-auto">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">{translations[language].userInfoTitle}</h2>
        <div className="space-y-4 mb-6">
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">{translations[language].idLabel}</div>
            <div className="text-white font-medium">{user}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">{translations[language].nameLabel}</div>
            <div className="text-white font-medium">{userName}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">{translations[language].authLabel}</div>
            <div className="text-white font-medium">{auth}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">{translations[language].levelLabel}</div>
            <div className="text-white font-medium">{level}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">{translations[language].accessInfoLabel}</div>
            <div className="text-white font-medium">
              {Array.isArray(infos) ? infos.join(', ') : infos}
            </div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-2">{translations[language].changePwdLabel}</div>
            <div className="space-y-2">
              <div className="flex gap-2">
                <input
                  type="password"
                  name="newPassword"
                  autoComplete="new-password"
                  autoCorrect="off"
                  spellCheck={false}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder={translations[language].newPwdPlaceholder}
                  className="flex-1 p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                />
                <button
                  onClick={handleSavePassword}
                  className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded transition-colors text-sm whitespace-nowrap"
                >
                  {translations[language].save}
                </button>
              </div>
              <input
                type="password"
                name="confirmPassword"
                autoComplete="new-password"
                autoCorrect="off"
                spellCheck={false}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder={translations[language].confirmPwdPlaceholder}
                className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
              />
            </div>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={onLogout}
            className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white rounded transition-colors"
          >
            {translations[language].logout}
          </button>
          <button
            onClick={onClose}
            className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
          >
            {translations[language].close}
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default UserInfoModal
