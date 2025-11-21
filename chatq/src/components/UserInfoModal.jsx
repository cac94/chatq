import { useState } from 'react'
import axios from 'axios'
import Modal from './Modal'

const UserInfoModal = ({ isOpen, onClose, user, userName, auth, level, infos, onLogout, apiBaseUrl }) => {
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const handleSavePassword = async () => {
    if (!password) {
      alert('비밀번호를 입력해주세요.')
      return
    }
    if (password !== confirmPassword) {
      alert('비밀번호가 일치하지 않습니다.')
      return
    }
    // TODO: 비밀번호 변경 API 호출
    try {
      await axios.put(`${apiBaseUrl}/api/users/password`, { newPassword: password })
      alert('비밀번호가 변경되었습니다.')
      setPassword('')
      setConfirmPassword('')
    } catch (error) {
      console.error('Failed to change password:', error)
      alert('비밀번호 변경에 실패했습니다.')
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="w-96 mx-auto">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">사용자 정보</h2>
        <div className="space-y-4 mb-6">
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">아이디</div>
            <div className="text-white font-medium">{user}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">이름</div>
            <div className="text-white font-medium">{userName}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">권한</div>
            <div className="text-white font-medium">{auth}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">레벨</div>
            <div className="text-white font-medium">{level}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">접근가능정보</div>
            <div className="text-white font-medium">
              {Array.isArray(infos) ? infos.join(', ') : infos}
            </div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-2">비밀번호 변경</div>
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
                  placeholder="새 비밀번호"
                  className="flex-1 p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                />
                <button
                  onClick={handleSavePassword}
                  className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded transition-colors text-sm whitespace-nowrap"
                >
                  저장
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
                placeholder="비밀번호 확인"
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
            로그아웃
          </button>
          <button
            onClick={onClose}
            className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
          >
            닫기
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default UserInfoModal
