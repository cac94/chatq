import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import './App.css'
import DataGrid from './components/DataGrid'
import Modal from './components/Modal'
import LoginModal from './components/LoginModal'
import UserInfoModal from './components/UserInfoModal'
import UserManagement from './components/UserManagement'
import AuthManagement from './components/AuthManagement'
import InfoManagement from './components/InfoManagement'
import chatqLogo from './assets/chatqicon51x51.png'

const API_BASE_URL = 'http://localhost:8080'

// Configure axios to send cookies with requests
axios.defaults.withCredentials = true

const App = () => {
  const [grids, setGrids] = useState([])
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [showLoginModal, setShowLoginModal] = useState(false)
  const [showUserInfoModal, setShowUserInfoModal] = useState(false)
  const [showUserManagement, setShowUserManagement] = useState(false)
  const [showAuthManagement, setShowAuthManagement] = useState(false)
  const [showInfoManagement, setShowInfoManagement] = useState(false)
  const [showAlert, setShowAlert] = useState(false)
  const [alertMessage, setAlertMessage] = useState('')
  const [auth, setAuth] = useState('GUEST')
  const [infos, setInfos] = useState(null)
  const [level, setLevel] = useState(9)
  const [userNm, setUserNm] = useState(null)
  const bottomRef = useRef(null)
  const inputContainerRef = useRef(null)
  const qurl = `${API_BASE_URL}/api/chatq` // Your API endpoint

  // Store lastQuery and tableQuery from previous response
  const [lastQuery, setLastQuery] = useState(null)
  const [tableQuery, setTableQuery] = useState(null)
  const [lastDetailYn, setLastDetailYn] = useState(null)
  const [tableName, setTableName] = useState(null)
  const [headerColumns, setHeaderColumns] = useState(null)
  const [lastColumns, setLastColumns] = useState(null)

  const handleLogin = async (e) => {
    e.preventDefault()
    const formData = new FormData(e.target)
    const username = formData.get('username')
    const password = formData.get('password')

    try {
      const response = await axios.post(`${API_BASE_URL}/api/login`, {
        user: username,
        password:password
      })

      // 응답 메시지 확인
      if (response.data.message === 'FAIL') {
        setAlertMessage('로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.')
        setShowAlert(true)
        return
      }

      // 응답 데이터 저장
      setAuth(response.data.auth)
      setInfos(response.data.infos)
      setLevel(response.data.level)
      setUserNm(response.data.user_nm)
      
      setShowLoginModal(false)
      setAlertMessage(`${response.data.user_nm}님 환영합니다!`)
      setShowAlert(true)
    } catch (error) {
      console.error('Login Error:', error)
      setAlertMessage('로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.')
      setShowAlert(true)
    }
  }

  const handleSend = async () => {
    if (query.trim()) {
      setIsLoading(true)
      try {
        const postData = {
          prompt: query
        }
        if (lastQuery) postData.lastQuery = lastQuery
        if (tableQuery) postData.tableQuery = tableQuery
        if (lastDetailYn) postData.lastDetailYn = lastDetailYn
        if (tableName) postData.tableName = tableName
        if (headerColumns) postData.headerColumns = headerColumns
        if (lastColumns) postData.lastColumns = lastColumns

        const response = await axios.post(qurl, postData)

        // Convert the response format to match our grid structure
        const columns = response.data.columns.map(col => ({
          key: col,
          label: col.charAt(0).toUpperCase() + col.slice(1) // Capitalize first letter
        }))
        const headerColumnsData = response.data.headerColumns 
          ? response.data.headerColumns.map(col => ({
              key: col,
              label: col.charAt(0).toUpperCase() + col.slice(1) // Capitalize first letter
            }))
          : []

        setGrids(prevGrids => [...prevGrids, {
          id: Date.now(),
          query: query,
          data: response.data.data,
          columns: columns,
          headerColumns: headerColumnsData,
          headerData: response.data.headerData,
          detailYn: response.data.detailYn
        }])

        // Save lastQuery and tableQuery for next request
        setLastQuery(response.data.lastQuery || null)
        setTableQuery(response.data.tableQuery || null)
        setLastDetailYn(response.data.lastDetailYn || null)
        setTableName(response.data.tableName || null)
        if (response.data.headerColumns && response.data.headerColumns.length > 0) {
          setHeaderColumns(response.data.headerColumns)
        }
        setLastColumns(response.data.lastColumns || null)

        setQuery('') // Clear input after sending
      } catch (error) {
        console.error('API Error:', error)
        setAlertMessage('조회를 실패했습니다. 좀 더 구체적으로 입력해보시거나 상단의 ChatQ 로고를 클릭하여 새로 시작해보세요.')
        setShowAlert(true)
      } finally {
        setIsLoading(false)
      }
    }
  }

  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [grids])

  return (
    <div className="min-h-screen w-full bg-slate-900">
      {/* Fixed header with input */}
      <div className="sticky top-0 bg-slate-900 p-4 shadow-lg z-50" ref={inputContainerRef}>
        <div className="absolute top-4 right-4 flex items-center gap-2 z-[60]">
          {level === 1 && (
            <>
              <button
                className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                onClick={() => {
                  setShowUserManagement(true)
                }}
                title="사용자 관리"
              >
                <svg className="h-6 w-6 text-slate-300 hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              </button>
              <button
                className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                onClick={() => {
                  setShowAuthManagement(true)
                }}
                title="권한 관리"
              >
                <svg className="h-6 w-6 text-slate-300 hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </button>
              <button
                className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                onClick={() => {
                  setShowInfoManagement(true)
                }}
                title="정보 관리"
              >
                <svg className="h-6 w-6 text-slate-300 hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </button>
            </>
          )}
          <button
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
            onClick={() => {
              if (userNm) {
                setShowUserInfoModal(true)
              } else {
                setShowLoginModal(true)
              }
            }}
            title={userNm ? "사용자 정보" : "로그인"}
          >
            <svg className="h-6 w-6 text-slate-300 hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </button>
        </div>
        <div className="max-w-4xl mx-auto">
          <h1
            className="text-4xl font-bold text-white mb-6 text-center flex items-center justify-center gap-3 cursor-pointer select-none hover:opacity-90"
            role="button"
            tabIndex={0}
            title="새 ChatQ 시작"
            onClick={() => {
              setGrids([])
              setLastQuery(null)
              setTableQuery(null)
              setDetailYn(null)
              setTableName(null)
              setHeaderColumns(null)
              setLastColumns(null)
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                setGrids([])
                setLastQuery(null)
                setTableQuery(null)
                setDetailYn(null)
                setTableName(null)
                setHeaderColumns(null)
                setLastColumns(null)
              }
            }}
          >
            <img src={chatqLogo} alt="ChatQ Logo" className="h-8 w-8" />
            ChatQ
          </h1>
          <div className="relative">
            <img src={chatqLogo} alt="ChatQ" className="absolute left-3 top-1/2 -translate-y-1/2 h-6 w-6" />
            <input 
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !isLoading) {
                  handleSend()
                }
              }}
              placeholder="DB에서 조회하고 싶은 것을 물어보세요..."
              className="w-full p-3 pl-12 pr-12 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 focus:outline-none focus:border-slate-500"
            />
            <button 
              onClick={handleSend}
              disabled={isLoading}
              className={`absolute right-2 top-1/2 -translate-y-1/2 p-2 rounded-md transition-colors
                ${isLoading 
                  ? 'text-blue-400 cursor-not-allowed' 
                  : 'text-blue-500 hover:text-blue-400 hover:bg-slate-700'}`}
              aria-label="Send"
            >
              {isLoading ? (
                <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              ) : (
                <svg className="h-5 w-5 rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Scrollable content area */}
      <div className="max-w-4xl mx-auto p-4">
        <div className="space-y-6 mt-4">
          {grids.map(grid => (
            <div key={grid.id} className="bg-slate-800 p-4 rounded-lg">
              <div className="text-slate-300 mb-4 font-medium flex items-center justify-between">
                <span>Query: {grid.query}</span>
                <div className="flex items-center gap-0">
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(grid.query)
                    }}
                    className="p-1 hover:bg-slate-700 rounded-md transition-colors text-slate-400 hover:text-slate-300"
                    title="쿼리 복사"
                  >
                    <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                  </button>
                  <button
                    onClick={() => {
                      // Excel export logic will go here
                      const csv = [
                        grid.columns.map(col => col.label).join(','),
                        ...grid.data.map(row => 
                          grid.columns.map(col => {
                            const value = row[col.key]
                            return typeof value === 'string' && value.includes(',') 
                              ? `"${value}"` 
                              : value
                          }).join(',')
                        )
                      ].join('\n')
                      
                      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
                      const link = document.createElement('a')
                      link.href = URL.createObjectURL(blob)
                      link.download = `chatq_${grid.id}.csv`
                      link.click()
                    }}
                    className="p-1 hover:bg-slate-700 rounded-md transition-colors text-green-500 hover:text-green-400"
                    title="Excel로 다운로드"
                  >
                    <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </button>
                </div>
              </div>
              <DataGrid 
                data={grid.data} 
                columns={grid.columns} 
                headerData={grid.headerData}
                headerColumns={grid.headerColumns}
                detailYn={grid.detailYn}  
              />
            </div>
          ))}
          <div ref={bottomRef} />
        </div>
      </div>

      {/* Login Modal */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onSubmit={handleLogin}
      />

      {/* Alert Modal */}
      {showAlert && (
        <Modal isOpen={showAlert} onClose={() => setShowAlert(false)}>
          <div className="w-96 mx-auto">
            <div className="flex flex-col items-center">
              <div className="mb-4">
                <svg className="h-12 w-12 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <p className="text-white text-center mb-6 whitespace-pre-line">{alertMessage}</p>
              <button
                onClick={() => setShowAlert(false)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    setShowAlert(false)
                  }
                }}
                className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
                autoFocus
              >
                확인
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* User Info Modal */}
      <UserInfoModal
        isOpen={showUserInfoModal}
        onClose={() => setShowUserInfoModal(false)}
        userName={userNm}
        auth={auth}
        level={level}
        infos={infos ? infos.join(', ') : ''}
        onLogout={async () => {
          try {
            await axios.post(`${API_BASE_URL}/api/logout`)
            setAuth(null)
            setInfos(null)
            setLevel(null)
            setUserNm(null)
            setGrids([])
            setLastQuery(null)
            setTableQuery(null)
            setLastDetailYn(null)
            setTableName(null)
            setHeaderColumns(null)
            setLastColumns(null)
            setShowUserInfoModal(false)
            setAlertMessage('로그아웃되었습니다.')
            setShowAlert(true)
          } catch (error) {
            console.error('Logout Error:', error)
            setAlertMessage('로그아웃 처리 중 오류가 발생했습니다.')
            setShowAlert(true)
          }
        }}
      />

      {/* User Management Modal */}
      <UserManagement
        isOpen={showUserManagement}
        onClose={() => setShowUserManagement(false)}
        apiBaseUrl={API_BASE_URL}
      />

      {/* Auth Management Modal */}
      <AuthManagement
        isOpen={showAuthManagement}
        onClose={() => setShowAuthManagement(false)}
        apiBaseUrl={API_BASE_URL}
      />

      {/* Info Management Modal */}
      <InfoManagement
        isOpen={showInfoManagement}
        onClose={() => setShowInfoManagement(false)}
        apiBaseUrl={API_BASE_URL}
      />
    </div>
  )
}

export default App
