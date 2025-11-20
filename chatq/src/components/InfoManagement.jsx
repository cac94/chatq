import { useState, useEffect } from 'react'
import Modal from './Modal'
import axios from 'axios'

const InfoManagement = ({ isOpen, onClose, apiBaseUrl }) => {
  const [infos, setInfos] = useState([])
  const [selectedInfo, setSelectedInfo] = useState(null)
  const [columns, setColumns] = useState([])
  const [editingInfo, setEditingInfo] = useState(null)
  const [editingColumn, setEditingColumn] = useState(null)
  const [isAddingNewInfo, setIsAddingNewInfo] = useState(false)
  const [isAddingNewColumn, setIsAddingNewColumn] = useState(false)
  const [infoFormData, setInfoFormData] = useState({
    info: '',
    info_nm: '',
    description: ''
  })
  const [columnFormData, setColumnFormData] = useState({
    column_code: '',
    column_nm: '',
    level: '',
    description: ''
  })

  useEffect(() => {
    if (isOpen) {
      loadInfos()
    }
  }, [isOpen])

  useEffect(() => {
    if (selectedInfo) {
      loadColumns(selectedInfo.info)
    } else {
      setColumns([])
    }
  }, [selectedInfo])

  const loadInfos = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get(`${apiBaseUrl}/api/infos`)
      setInfos(response.data)
    } catch (error) {
      console.error('Failed to load infos:', error)
      // Mock data for development
      setInfos([
        { info: 'USER_INFO', info_nm: '사용자정보', description: '사용자 기본 정보' },
        { info: 'ORDER_INFO', info_nm: '주문정보', description: '주문 관련 정보' }
      ])
    }
  }

  const loadColumns = async (infoId) => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get(`${apiBaseUrl}/api/infos/${infoId}/columns`)
      setColumns(response.data)
    } catch (error) {
      console.error('Failed to load columns:', error)
      // Mock data for development
      setColumns([
        { column_code: 'user_id', column_nm: '사용자ID', level: '1', description: '사용자 고유 ID' },
        { column_code: 'user_nm', column_nm: '사용자명', level: '1', description: '사용자 이름' },
        { column_code: 'email', column_nm: '이메일', level: '2', description: '이메일 주소' }
      ])
    }
  }

  const handleInfoSelect = (info) => {
    setSelectedInfo(info)
    setEditingInfo(null)
    setIsAddingNewInfo(false)
    setEditingColumn(null)
    setIsAddingNewColumn(false)
  }

  const handleAddInfo = () => {
    setIsAddingNewInfo(true)
    setEditingInfo(null)
    setInfoFormData({ info: '', info_nm: '', description: '' })
  }

  const handleEditInfo = (info) => {
    setIsAddingNewInfo(false)
    setEditingInfo(info)
    setInfoFormData({ ...info })
  }

  const handleDeleteInfo = async (infoId) => {
    if (window.confirm(`정보 '${infoId}'를 삭제하시겠습니까?`)) {
      try {
        // TODO: Replace with actual API endpoint
        await axios.delete(`${apiBaseUrl}/api/infos/${infoId}`)
        if (selectedInfo?.info === infoId) {
          setSelectedInfo(null)
        }
        loadInfos()
      } catch (error) {
        console.error('Failed to delete info:', error)
        alert('정보 삭제에 실패했습니다.')
      }
    }
  }

  const handleSaveInfo = async () => {
    try {
      if (isAddingNewInfo) {
        // TODO: Replace with actual API endpoint
        await axios.post(`${apiBaseUrl}/api/infos`, infoFormData)
      } else {
        // TODO: Replace with actual API endpoint
        await axios.put(`${apiBaseUrl}/api/infos/${editingInfo.info}`, infoFormData)
      }
      setIsAddingNewInfo(false)
      setEditingInfo(null)
      loadInfos()
    } catch (error) {
      console.error('Failed to save info:', error)
      alert('정보 저장에 실패했습니다.')
    }
  }

  const handleCancelInfo = () => {
    setIsAddingNewInfo(false)
    setEditingInfo(null)
  }

  const handleAddColumn = () => {
    if (!selectedInfo) {
      alert('먼저 정보를 선택해주세요.')
      return
    }
    setIsAddingNewColumn(true)
    setEditingColumn(null)
    setColumnFormData({ column_code: '', column_nm: '', level: '', description: '' })
  }

  const handleEditColumn = (column) => {
    setIsAddingNewColumn(false)
    setEditingColumn(column)
    setColumnFormData({ ...column })
  }

  const handleDeleteColumn = async (columnCode) => {
    if (window.confirm(`칼럼 '${columnCode}'을 삭제하시겠습니까?`)) {
      try {
        // TODO: Replace with actual API endpoint
        await axios.delete(`${apiBaseUrl}/api/infos/${selectedInfo.info}/columns/${columnCode}`)
        loadColumns(selectedInfo.info)
      } catch (error) {
        console.error('Failed to delete column:', error)
        alert('칼럼 삭제에 실패했습니다.')
      }
    }
  }

  const handleSaveColumn = async () => {
    try {
      if (isAddingNewColumn) {
        // TODO: Replace with actual API endpoint
        await axios.post(`${apiBaseUrl}/api/infos/${selectedInfo.info}/columns`, columnFormData)
      } else {
        // TODO: Replace with actual API endpoint
        await axios.put(`${apiBaseUrl}/api/infos/${selectedInfo.info}/columns/${editingColumn.column_code}`, columnFormData)
      }
      setIsAddingNewColumn(false)
      setEditingColumn(null)
      loadColumns(selectedInfo.info)
    } catch (error) {
      console.error('Failed to save column:', error)
      alert('칼럼 저장에 실패했습니다.')
    }
  }

  const handleCancelColumn = () => {
    setIsAddingNewColumn(false)
    setEditingColumn(null)
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="w-[1200px] mx-auto">
        <h2 className="text-xl font-bold text-white mb-6">정보 관리</h2>
        
        <div className="flex gap-4">
          {/* Left Panel - Info List */}
          <div className="w-1/3">{/* w-1/3 makes it half of remaining space when column panel takes 2/3 */}
            <h3 className="text-base font-semibold text-white mb-4">정보 목록</h3>

            {/* Info Grid */}
            <div className="bg-slate-800 rounded-lg overflow-hidden mb-4">
              <div className="overflow-y-auto max-h-[500px]">
                <table className="w-full">
                  <thead className="bg-slate-700 sticky top-0">
                    <tr>
                      <th className="px-3 py-2 text-center text-slate-200 text-sm">정보코드</th>
                      <th className="px-3 py-2 text-center text-slate-200 text-sm">정보명</th>
                    </tr>
                  </thead>
                  <tbody>
                    {infos.map((info) => (
                      <tr 
                        key={info.info} 
                        className={`border-t border-slate-700 cursor-pointer ${
                          selectedInfo?.info === info.info ? 'bg-blue-600/30' : 'hover:bg-slate-750'
                        }`}
                        onClick={() => handleInfoSelect(info)}
                      >
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{info.info}</td>
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{info.info_nm}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Right Panel - Column List */}
          <div className="w-1/3">
            <div className="mb-4">
              <h3 className="text-base font-semibold text-white">
                칼럼 정보 {selectedInfo && `- ${selectedInfo.info_nm}`}
              </h3>
            </div>

            {/* Column Grid */}
            <div className="bg-slate-800 rounded-lg overflow-hidden mb-4">
              <div className="overflow-x-auto overflow-y-auto max-h-[500px]">
                <table className="w-full">
                  <thead className="bg-slate-700 sticky top-0">
                    <tr>
                      <th className="px-3 py-2 text-center text-slate-200 text-sm">칼럼코드</th>
                      <th className="px-3 py-2 text-center text-slate-200 text-sm">칼럼명</th>
                      <th className="px-3 py-2 text-center text-slate-200 text-sm">레벨</th>
                    </tr>
                  </thead>
                  <tbody>
                    {columns.map((column) => (
                      <tr key={column.column_code} className="border-t border-slate-700 hover:bg-slate-750">
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{column.column_code}</td>
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{column.column_nm}</td>
                        <td className="px-3 py-2 text-center">
                          <select
                            value={column.level}
                            onChange={async (e) => {
                              const newLevel = e.target.value
                              // Update locally first for immediate feedback
                              const updatedColumns = columns.map(c => 
                                c.column_code === column.column_code ? { ...c, level: newLevel } : c
                              )
                              setColumns(updatedColumns)
                              // TODO: Replace with actual API endpoint
                              try {
                                await axios.put(`${apiBaseUrl}/api/infos/${selectedInfo.info}/columns/${column.column_code}`, {
                                  ...column,
                                  level: newLevel
                                })
                              } catch (error) {
                                console.error('Failed to update level:', error)
                                // Revert on error
                                setColumns(columns)
                              }
                            }}
                            className="w-16 p-1 text-center rounded bg-slate-600 text-white border border-slate-500 focus:outline-none focus:border-blue-500 text-sm"
                          >
                            {[1, 2, 3, 4, 5, 6, 7, 8, 9].map(level => (
                              <option key={level} value={level}>{level}</option>
                            ))}
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        {/* Close Button */}
        <div className="flex justify-end mt-4">
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

export default InfoManagement
