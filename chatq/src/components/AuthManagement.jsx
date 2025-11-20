import { useState, useEffect } from 'react'
import Modal from './Modal'
import axios from 'axios'

const AuthManagement = ({ isOpen, onClose, apiBaseUrl }) => {
  const [auths, setAuths] = useState([])
  const [authInfoOptions, setAuthInfoOptions] = useState([])
  const [editingAuth, setEditingAuth] = useState(null)
  const [isAddingNew, setIsAddingNew] = useState(false)
  const [formData, setFormData] = useState({
    auth: '',
    auth_nm: '',
    infos: []
  })

  useEffect(() => {
    if (isOpen) {
      loadAuths()
      loadAuthInfoOptions()
    }
  }, [isOpen])

  const loadAuths = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get(`${apiBaseUrl}/api/auths`)
      setAuths(response.data)
    } catch (error) {
      console.error('Failed to load auths:', error)
      // Mock data for development
      setAuths([
        { auth: 'ADMIN', auth_nm: '관리자', infos: '전체 권한' },
        { auth: 'USER', auth_nm: '일반사용자', infos: '조회 권한' }
      ])
    }
  }

  const loadAuthInfoOptions = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get(`${apiBaseUrl}/api/auth-infos`)
      setAuthInfoOptions(response.data)
    } catch (error) {
      console.error('Failed to load auth info options:', error)
      // Mock data for development
      setAuthInfoOptions([
        { code: 'READ', name: '조회' },
        { code: 'WRITE', name: '작성' },
        { code: 'UPDATE', name: '수정' },
        { code: 'DELETE', name: '삭제' },
        { code: 'ADMIN', name: '관리' }
      ])
    }
  }

  const handleAdd = () => {
    setIsAddingNew(true)
    setEditingAuth(null)
    setFormData({ auth: '', auth_nm: '', infos: [] })
  }

  const handleEdit = (auth) => {
    setIsAddingNew(false)
    setEditingAuth(auth)
    // Parse infos string to array
    const infosArray = auth.infos ? auth.infos.split(',').map(s => s.trim()) : []
    setFormData({ ...auth, infos: infosArray })
  }

  const handleDelete = async (authId) => {
    if (window.confirm(`권한 '${authId}'를 삭제하시겠습니까?`)) {
      try {
        // TODO: Replace with actual API endpoint
        await axios.delete(`${apiBaseUrl}/api/auths/${authId}`)
        loadAuths()
      } catch (error) {
        console.error('Failed to delete auth:', error)
        alert('권한 삭제에 실패했습니다.')
      }
    }
  }

  const handleSave = async () => {
    try {
      // Convert infos array to comma-separated string
      const saveData = {
        ...formData,
        infos: formData.infos.join(', ')
      }
      if (isAddingNew) {
        // TODO: Replace with actual API endpoint
        await axios.post(`${apiBaseUrl}/api/auths`, saveData)
      } else {
        // TODO: Replace with actual API endpoint
        await axios.put(`${apiBaseUrl}/api/auths/${editingAuth.auth}`, saveData)
      }
      setIsAddingNew(false)
      setEditingAuth(null)
      loadAuths()
    } catch (error) {
      console.error('Failed to save auth:', error)
      alert('권한 저장에 실패했습니다.')
    }
  }

  const handleCancel = () => {
    setIsAddingNew(false)
    setEditingAuth(null)
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="w-[800px] mx-auto">
        <h2 className="text-xl font-bold text-white mb-6">권한 관리</h2>
        
        {/* Add Button */}
        <div className="flex justify-end mb-4">
          <button
            onClick={handleAdd}
            className="p-2 bg-green-600 hover:bg-green-700 text-white rounded transition-colors"
            title="권한 추가"
          >
            <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
          </button>
        </div>

        {/* Auth Grid */}
        <div className="bg-slate-800 rounded-lg overflow-hidden mb-4">
          <div className="overflow-y-auto max-h-[400px]">
            <table className="w-full">
              <thead className="bg-slate-700 sticky top-0">
                <tr>
                  <th className="px-4 py-2 text-left text-slate-200 text-sm">권한코드</th>
                  <th className="px-4 py-2 text-left text-slate-200 text-sm">권한명</th>
                  <th className="px-4 py-2 text-left text-slate-200 text-sm">권한 정보</th>
                  <th className="px-4 py-2 text-center text-slate-200 text-sm">작업</th>
                </tr>
              </thead>
              <tbody>
                {auths.map((auth) => (
                  <tr key={auth.auth} className="border-t border-slate-700 hover:bg-slate-750">
                    <td className="px-4 py-2 text-slate-300 text-sm">{auth.auth}</td>
                    <td className="px-4 py-2 text-slate-300 text-sm">{auth.auth_nm}</td>
                    <td className="px-4 py-2 text-slate-300 text-sm">{auth.infos}</td>
                    <td className="px-4 py-2 text-center">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => handleEdit(auth)}
                          className="p-2 hover:bg-blue-600 text-blue-400 hover:text-white rounded transition-colors"
                          title="수정"
                        >
                          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                          </svg>
                        </button>
                        <button
                          onClick={() => handleDelete(auth.auth)}
                          className="p-2 hover:bg-red-600 text-red-400 hover:text-white rounded transition-colors"
                          title="삭제"
                        >
                          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
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
        {(isAddingNew || editingAuth) && (
          <div className="bg-slate-700 p-4 rounded-lg mb-4">
            <h3 className="text-base font-semibold text-white mb-4">
              {isAddingNew ? '새 권한 추가' : '권한 수정'}
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-300 mb-2 text-sm">권한코드</label>
                <input
                  type="text"
                  value={formData.auth}
                  onChange={(e) => setFormData({ ...formData, auth: e.target.value })}
                  disabled={!isAddingNew}
                  className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 disabled:opacity-50 text-sm"
                />
              </div>
              <div>
                <label className="block text-slate-300 mb-2 text-sm">권한명</label>
                <input
                  type="text"
                  value={formData.auth_nm}
                  onChange={(e) => setFormData({ ...formData, auth_nm: e.target.value })}
                  className="w-full p-2 rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                />
              </div>
              <div className="col-span-2">
                <label className="block text-slate-300 mb-2 text-sm">권한 정보</label>
                <div className="grid grid-cols-2 gap-2 p-3 rounded bg-slate-600 border border-slate-500 max-h-[200px] overflow-y-auto">
                  {authInfoOptions.map((option) => (
                    <label key={option.code} className="flex items-center gap-2 text-slate-200 cursor-pointer hover:text-white text-sm">
                      <input
                        type="checkbox"
                        checked={formData.infos.includes(option.code)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormData({
                              ...formData,
                              infos: [...formData.infos, option.code]
                            })
                          } else {
                            setFormData({
                              ...formData,
                              infos: formData.infos.filter(d => d !== option.code)
                            })
                          }
                        }}
                        className="w-4 h-4 rounded bg-slate-700 border-slate-400"
                      />
                      <span>{option.name}</span>
                    </label>
                  ))}
                </div>
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

export default AuthManagement
