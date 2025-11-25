import { useState, useEffect } from 'react'
import Modal from './Modal'
import axios from 'axios'

const InfoManagement = ({ isOpen, onClose }) => {
  const [infos, setInfos] = useState([]) // each info: { table_nm, table_alias }
  const [selectedInfo, setSelectedInfo] = useState(null) // selected info object
  const [columns, setColumns] = useState([])
  const [editingInfo, setEditingInfo] = useState(null)
  const [editingColumn, setEditingColumn] = useState(null)
  const [isAddingNewInfo, setIsAddingNewInfo] = useState(false)
  const [isAddingNewColumn, setIsAddingNewColumn] = useState(false)
  const [infoFormData, setInfoFormData] = useState({
    table_nm: '',
    table_alias: ''
  })
  const [columnFormData, setColumnFormData] = useState({
    column_cd: '',
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
      loadColumns(selectedInfo.table_nm)
    } else {
      setColumns([])
    }
  }, [selectedInfo])

  const loadInfos = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get('/api/infos')
      // Expect response.data to be array of { table_nm, table_alias }
      setInfos(response.data)
    } catch (error) {
      console.error('Failed to load infos:', error)
      // Mock data for development
      setInfos([
        { table_nm: 'chatquser', table_alias: '사용자' },
        { table_nm: 'chatqauth', table_alias: '권한' },
        { table_nm: 'chatqtable', table_alias: '테이블' }
      ])
    }
  }

  const loadColumns = async (tableNm) => {
    try {
      // TODO: Replace with actual API endpoint
      const response = await axios.get(`/api/infos/columns/${tableNm}`)
      setColumns(response.data)
    } catch (error) {
      console.error('Failed to load columns:', error)
      // Mock data for development
      setColumns([
        { column_cd: 'user_id', column_nm: '사용자ID', level: '1', description: '사용자 고유 ID' },
        { column_cd: 'user_nm', column_nm: '사용자명', level: '1', description: '사용자 이름' },
        { column_cd: 'email', column_nm: '이메일', level: '2', description: '이메일 주소' }
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
                      <th className="px-3 py-2 text-center text-slate-200 text-sm">테이블명</th>
                      <th className="px-3 py-2 text-center text-slate-200 text-sm">테이블별칭</th>
                    </tr>
                  </thead>
                  <tbody>
                    {infos.map((info) => (
                      <tr 
                        key={info.table_nm} 
                        className={`border-t border-slate-700 cursor-pointer ${
                          selectedInfo?.table_nm === info.table_nm ? 'bg-blue-600/30' : 'hover:bg-slate-750'
                        }`}
                        onClick={() => handleInfoSelect(info)}
                      >
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{info.table_nm}</td>
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{info.table_alias}</td>
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
                칼럼 정보 {selectedInfo && `- ${selectedInfo.table_alias}`}
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
                      <tr key={column.column_cd} className="border-t border-slate-700 hover:bg-slate-750">
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{column.column_cd}</td>
                        <td className="px-3 py-2 text-center text-slate-300 text-sm">{column.column_nm}</td>
                        <td className="px-3 py-2 text-center">
                          <select
                            value={column.level}
                            onChange={async (e) => {
                              const newLevel = e.target.value
                              // Update locally first for immediate feedback
                              const updatedColumns = columns.map(c => 
                                c.column_cd === column.column_cd ? { ...c, level: newLevel } : c
                              )
                              setColumns(updatedColumns)
                              // TODO: Replace with actual API endpoint
                              try {
                                await axios.put(`/api/infos/columns/${selectedInfo.table_nm}`, {
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
