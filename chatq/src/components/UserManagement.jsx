import { useState, useEffect } from 'react'
import Modal from './Modal'
import axios from 'axios'

const UserManagement = ({ isOpen, onClose, apiBaseUrl }) => {
  const [users, setUsers] = useState([])
  const [authOptions, setAuthOptions] = useState([])
  const [searchTerm, setSearchTerm] = useState('')
  const [editingUser, setEditingUser] = useState(null)
  const [isAddingNew, setIsAddingNew] = useState(false)
  const [formData, setFormData] = useState({
    user: '',
    user_nm: '',
    password: '',
    auth: '',
    level: ''
  })

  useEffect(() => {
    if (isOpen) {
      loadUsers()
      loadAuthOptions()
    }
  }, [isOpen])

  const loadUsers = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get(`${apiBaseUrl}/api/users`)
      setUsers(response.data)
    } catch (error) {
      console.error('Failed to load users:', error)
      // Mock data for development
      setUsers([
        { user: 'admin', user_nm: '관리자', auth: 'ADMIN', level: 1 },
        { user: 'user1', user_nm: '사용자1', auth: 'USER', level: 5 }
      ])
    }
  }

  const loadAuthOptions = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get(`${apiBaseUrl}/api/auth-options`)
      setAuthOptions(response.data)
    } catch (error) {
      console.error('Failed to load auth options:', error)
      // Mock data for development
      setAuthOptions(['ADMIN', 'USER', 'GUEST', 'MANAGER'])
    }
  }

  const handleSearch = () => {
    // Filter users based on search term
    if (searchTerm.trim()) {
      return users.filter(user => 
        user.user.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.user_nm.toLowerCase().includes(searchTerm.toLowerCase())
      )
    }
    return users
  }

  const handleAdd = () => {
    setIsAddingNew(true)
    setEditingUser(null)
    setFormData({ user: '', user_nm: '', password: '', auth: '', level: '' })
  }

  const handleEdit = (user) => {
    setIsAddingNew(false)
    setEditingUser(user)
    setFormData({ ...user, password: '' })
  }

  const handleDelete = async (userId) => {
    if (window.confirm(`사용자 '${userId}'를 삭제하시겠습니까?`)) {
      try {
        // TODO: Replace with actual API endpoint
        await axios.delete(`${apiBaseUrl}/api/users/${userId}`)
        loadUsers()
      } catch (error) {
        console.error('Failed to delete user:', error)
        alert('사용자 삭제에 실패했습니다.')
      }
    }
  }

  const handlePasswordReset = async (userId) => {
    if (window.confirm(`사용자 '${userId}'의 비밀번호를 초기화하시겠습니까?`)) {
      try {
        // TODO: Replace with actual API endpoint
        await axios.post(`${apiBaseUrl}/api/users/${userId}/reset-password`)
        alert('비밀번호가 초기화되었습니다.')
      } catch (error) {
        console.error('Failed to reset password:', error)
        alert('비밀번호 초기화에 실패했습니다.')
      }
    }
  }

  const handleSave = async () => {
    try {
      if (isAddingNew) {
        // TODO: Replace with actual API endpoint
        await axios.post(`${apiBaseUrl}/api/users`, formData)
      } else {
        // TODO: Replace with actual API endpoint
        await axios.put(`${apiBaseUrl}/api/users/${editingUser.user}`, formData)
      }
      setIsAddingNew(false)
      setEditingUser(null)
      loadUsers()
    } catch (error) {
      console.error('Failed to save user:', error)
      alert('사용자 저장에 실패했습니다.')
    }
  }

  const handleCancel = () => {
    setIsAddingNew(false)
    setEditingUser(null)
  }

  const filteredUsers = handleSearch()

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="w-[800px] mx-auto">
        <h2 className="text-xl font-bold text-white mb-6">사용자 관리</h2>
        
        {/* Search and Add Button */}
        <div className="flex gap-2 mb-4">
          <div className="flex-1 relative">
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="사용자 검색..."
              className="w-full p-2 pr-10 rounded bg-slate-700 text-white border border-slate-600 focus:outline-none focus:border-blue-500 text-sm"
            />
            <svg className="h-5 w-5 text-slate-400 absolute right-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <button
            onClick={handleAdd}
            className="p-2 bg-green-600 hover:bg-green-700 text-white rounded transition-colors"
            title="사용자 추가"
          >
            <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
          </button>
        </div>

        {/* User Grid */}
        <div className="bg-slate-800 rounded-lg overflow-hidden mb-4">
          <div className="overflow-y-auto max-h-[400px]">
            <table className="w-full">
              <thead className="bg-slate-700 sticky top-0">
                <tr>
                  <th className="px-4 py-2 text-left text-slate-200 text-sm">아이디</th>
                  <th className="px-4 py-2 text-left text-slate-200 text-sm">이름</th>
                  <th className="px-4 py-2 text-left text-slate-200 text-sm">권한</th>
                  <th className="px-4 py-2 text-left text-slate-200 text-sm">레벨</th>
                  <th className="px-4 py-2 text-center text-slate-200 text-sm">작업</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.user} className="border-t border-slate-700 hover:bg-slate-750">
                    <td className="px-4 py-2 text-slate-300 text-sm">{user.user}</td>
                    <td className="px-4 py-2 text-slate-300 text-sm">{user.user_nm}</td>
                    <td className="px-4 py-2 text-slate-300 text-sm">{user.auth}</td>
                    <td className="px-4 py-2 text-slate-300 text-sm">{user.level}</td>
                    <td className="px-4 py-2 text-center">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => handleEdit(user)}
                          className="p-2 hover:bg-blue-600 text-blue-400 hover:text-white rounded transition-colors"
                          title="수정"
                        >
                          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                          </svg>
                        </button>
                        <button
                          onClick={() => handleDelete(user.user)}
                          className="p-2 hover:bg-red-600 text-red-400 hover:text-white rounded transition-colors"
                          title="삭제"
                        >
                          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                        <button
                          onClick={() => handlePasswordReset(user.user)}
                          className="p-2 hover:bg-yellow-600 text-yellow-400 hover:text-white rounded transition-colors"
                          title="비밀번호 초기화"
                        >
                          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Edit Form */}
        {(isAddingNew || editingUser) && (
          <div className="bg-slate-700 p-4 rounded-lg mb-4">
            <h3 className="text-base font-semibold text-white mb-4">
              {isAddingNew ? '새 사용자 추가' : '사용자 수정'}
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-300 mb-2 text-sm">아이디</label>
                <input
                  type="text"
                  value={formData.user}
                  onChange={(e) => setFormData({ ...formData, user: e.target.value })}
                  disabled={!isAddingNew}
                  className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 disabled:opacity-50 text-sm"
                />
              </div>
              <div>
                <label className="block text-slate-300 mb-2 text-sm">이름</label>
                <input
                  type="text"
                  value={formData.user_nm}
                  onChange={(e) => setFormData({ ...formData, user_nm: e.target.value })}
                  className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                />
              </div>
              <div>
                <label className="block text-slate-300 mb-2 text-sm">비밀번호</label>
                <input
                  type="password"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  placeholder={isAddingNew ? '' : '(변경시만 입력)'}
                  className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                />
              </div>
              <div>
                <label className="block text-slate-300 mb-2 text-sm">권한</label>
                <select
                  value={formData.auth}
                  onChange={(e) => setFormData({ ...formData, auth: e.target.value })}
                  className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                >
                  <option value="">선택하세요</option>
                  {authOptions.map((auth) => (
                    <option key={auth} value={auth}>{auth}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-slate-300 mb-2 text-sm">레벨</label>
                <input
                  type="number"
                  value={formData.level}
                  onChange={(e) => setFormData({ ...formData, level: e.target.value })}
                  className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                />
              </div>
            </div>
            <div className="flex gap-2 mt-4">
              <button
                onClick={handleSave}
                className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors text-sm"
              >
                저장
              </button>
              <button
                onClick={handleCancel}
                className="flex-1 py-2 bg-slate-600 hover:bg-slate-500 text-white rounded transition-colors text-sm"
              >
                취소
              </button>
            </div>
          </div>
        )}

        {/* Close Button */}
        <div className="flex justify-end">
          <button
            onClick={onClose}
            className="px-6 py-2 bg-slate-600 hover:bg-slate-500 text-white rounded transition-colors text-sm"
          >
            닫기
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default UserManagement
