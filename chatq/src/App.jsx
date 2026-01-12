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
import ChartPromptModal from './components/ChartPromptModal'
import ChartResultModal from './components/ChartResultModal'
import ConfirmModal from './components/ConfirmModal'
import chatqLogo from './assets/chatqicon51x51.png'
import translations from './translation'

// Configure axios to send cookies with requests
axios.defaults.withCredentials = true



const App = () => {
  const [grids, setGrids] = useState([])
  const [language, setLanguage] = useState(navigator.language.startsWith('ko') ? 'ko' : 'en')
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [showLoginModal, setShowLoginModal] = useState(false)
  const [showUserInfoModal, setShowUserInfoModal] = useState(false)
  const [showUserManagement, setShowUserManagement] = useState(false)
  const [showAuthManagement, setShowAuthManagement] = useState(false)
  const [showInfoManagement, setShowInfoManagement] = useState(false)
  const [showAlert, setShowAlert] = useState(false)
  const [alertMessage, setAlertMessage] = useState('')
  const [openUserInfoAfterAlert, setOpenUserInfoAfterAlert] = useState(false)
  const [auth, setAuth] = useState('GUEST')
  const [user, setUser] = useState(null)
  const [infos, setInfos] = useState(null)
  const [infoColumns, setInfoColumns] = useState(null)
  const [level, setLevel] = useState(9)
  const [userNm, setUserNm] = useState(null)
  const [showChartModal, setShowChartModal] = useState(false)
  const [showChartResultModal, setShowChartResultModal] = useState(false)
  const [chartResult, setChartResult] = useState(null)
  const [activeGridForChart, setActiveGridForChart] = useState(null)
  const [activeChartType, setActiveChartType] = useState('bar')
  const [showConfirmModal, setShowConfirmModal] = useState(false)
  const [confirmCallback, setConfirmCallback] = useState(null)
  const [confirmMessage, setConfirmMessage] = useState('')
  const [isListening, setIsListening] = useState(false)
  const [isLoginLoading, setIsLoginLoading] = useState(false)
  const bottomRef = useRef(null)
  const inputContainerRef = useRef(null)
  const recognitionRef = useRef(null)
  const qurl = '/api/chatq' // Your API endpoint

  // Store lastQuery and tableQuery from previous response
  const [lastQuery, setLastQuery] = useState(null)
  const [tableQuery, setTableQuery] = useState(null)
  const [lastDetailYn, setLastDetailYn] = useState(null)
  const [tableName, setTableName] = useState(null)
  const [tableAlias, setTableAlias] = useState(null)
  const [headerColumns, setHeaderColumns] = useState(null)
  const [lastColumns, setLastColumns] = useState(null)
  const [codeMaps, setCodeMaps] = useState(null)
  // ChatQ session history: each session stores its first query and tableAlias
  const [sessions, setSessions] = useState([]) // { id, firstQuery, tableAlias, startedAt }
  const [currentSessionId, setCurrentSessionId] = useState(null)
  const [selectedInfo, setSelectedInfo] = useState('')
  const [autoLogoutSec, setAutoLogoutSec] = useState(0)
  const autoLogoutTimerRef = useRef(null)
  const [isLoadingTopics, setIsLoadingTopics] = useState(false)
  const [hasMoreTopics, setHasMoreTopics] = useState(true)
  const topicsSidebarRef = useRef(null)

  const clearSessionState = () => {
    setGrids([])
    setLastQuery(null)
    setTableQuery(null)
    setLastDetailYn(null)
    setTableName(null)
    setTableAlias(null)
    setHeaderColumns(null)
    setLastColumns(null)
  }

  const handleResetSession = () => {
    clearSessionState()
    // Start a new ChatQ session
    const newId = Date.now()
    setSessions(prev => [...prev, { id: newId, firstQuery: null, tableAlias: null, startedAt: new Date() }])
    setCurrentSessionId(newId)
    return newId
  }

  const toggleShowDetail = (gridId) => {
    setGrids(prevGrids => prevGrids.map(grid =>
      grid.id === gridId ? { ...grid, showDetail: !grid.showDetail } : grid
    ))
  }

  // 로그아웃 처리 함수
  const handleLogout = async (isAutoLogout = false) => {
    try {
      await axios.post('/api/logout')
      clearSessionState()
      setAuth(null)
      setUser(null)
      setInfos(null)
      setInfoColumns(null)
      setLevel(null)
      setUserNm(null)
      setSessions([])
      setCurrentSessionId(null)
      setSelectedInfo('')
      setAutoLogoutSec(0)
      setIsLoadingTopics(false)
      setHasMoreTopics(true)
      if (autoLogoutTimerRef.current) {
        clearTimeout(autoLogoutTimerRef.current)
      }
      setShowUserInfoModal(false)
      
      if (isAutoLogout) {
        setAlertMessage(translations[language].autoLogout)
      } else {
        setAlertMessage(translations[language].logoutSuccess)
      }
      setShowAlert(true)
    } catch (error) {
      console.error('Logout Error:', error)
      if (!isAutoLogout) {
        setAlertMessage(translations[language].logoutError)
        setShowAlert(true)
      }
    }
  }

  // 자동 로그아웃 타이머 초기화 함수
  const resetAutoLogoutTimer = () => {
    if (autoLogoutTimerRef.current) {
      clearTimeout(autoLogoutTimerRef.current)
    }

    if (autoLogoutSec > 0 && user) {
      autoLogoutTimerRef.current = setTimeout(() => {
        handleLogout(true)
      }, autoLogoutSec * 1000)
    }
  }

  // 추가 topics 로드 함수
  const loadMoreTopics = async () => {
    if (isLoadingTopics || !hasMoreTopics || !user) return

    setIsLoadingTopics(true)
    try {
      const backendSessions = sessions.filter(s => s.isFromBackend)
      const lastTopicId = backendSessions.length > 0 
        ? Math.min(...backendSessions.map(s => s.id))
        : null

      const response = await axios.post('/api/topics', {
        lastTopicId: lastTopicId,
        limit: 20
      })

      if (response.data && response.data.length > 0) {
        const newTopicSessions = response.data.map(topic => ({
          id: topic.topicId,
          firstQuery: topic.firstQuery,
          tableAlias: topic.tableAlias,
          startedAt: new Date(topic.startedAt || topic.addDate + ' ' + topic.addTime),
          isFromBackend: true
        }))
        
        setSessions(prev => [...prev, ...newTopicSessions])
        setHasMoreTopics(response.data.length >= 20)
      } else {
        setHasMoreTopics(false)
      }
    } catch (error) {
      console.error('Error loading more topics:', error)
    } finally {
      setIsLoadingTopics(false)
    }
  }

  const handleLogin = async (e) => {
    e.preventDefault()
    const formData = new FormData(e.target)
    const username = formData.get('username')
    const password = formData.get('password')

    setIsLoginLoading(true)
    try {
      const response = await axios.post('/api/login', {
        user: username,
        password: password
      })

      // 응답 메시지 확인
      if (response.data.message === 'FAIL') {
        setAlertMessage(translations[language].loginFail)
        setShowAlert(true)
        return
      }

      // 응답 데이터 저장
      setAuth(response.data.auth)
      setInfos(response.data.infos)
      setInfoColumns(response.data.infoColumns)
      setLevel(response.data.level)
      setUserNm(response.data.user_nm)
      // 사용자 아이디 저장 (우선 서버 응답, 없으면 입력값 사용)
      setUser(response.data.user || username)
      setSelectedInfo('')
      setAutoLogoutSec(response.data.autoLogoutSec || 0)
      
      // topics를 sessions에 추가 (QueryTopic entity 구조 참고)
      if (response.data.topics && response.data.topics.length > 0) {
        const topicSessions = response.data.topics.map(topic => ({
          id: topic.topicId,
          firstQuery: topic.firstQuery,
          tableAlias: topic.tableAlias,
          startedAt: new Date(topic.startedAt || topic.addDate + ' ' + topic.addTime),
          isFromBackend: true // 백엔드에서 받은 topic임을 표시
        }))
        setSessions(topicSessions)
        setHasMoreTopics(true)
      } else {
        setHasMoreTopics(false)
      }

      setShowLoginModal(false)
      // 비밀번호 초기화 상태면 안내 메시지와 함께 사용자정보 모달 오픈
      const pwdFiredYn = response.data.pwdFiredYn ?? response.data.pwd_fired_yn
      if (pwdFiredYn === 'Y') {
        setAlertMessage(translations[language].welcomePwd.replace('{name}', response.data.user_nm))
        setOpenUserInfoAfterAlert(true)
      } else {
        setAlertMessage(translations[language].welcome.replace('{name}', response.data.user_nm))
        setOpenUserInfoAfterAlert(false)
      }
      setShowAlert(true)
    } catch (error) {
      console.error('Login Error:', error)
      setAlertMessage(translations[language].loginFail)
      setShowAlert(true)
    } finally {
      setIsLoginLoading(false)
    }
  }

  const handleSend = async (overridePrompt = null, sessionIdForFirstQuery = null, ignoreContext = false, overrideTableAlias = null) => {
    const effectivePrompt = overridePrompt !== null ? overridePrompt : query

    if (effectivePrompt.trim()) {
      // Check if search target has changed - only when user manually selected different target
      console.log('ignoreContext:', ignoreContext, 'tableAlias:', tableAlias, 'selectedInfo:', selectedInfo)
      if (!ignoreContext && tableAlias && selectedInfo !== tableAlias) {
        // Use setTimeout to ensure state updates are processed
        const tempTableAlias = String(selectedInfo) // Capture current selectedInfo value
        setTimeout(() => {
          setConfirmMessage(translations[language].searchTargetChanged)
          const callback = () => {
            const newId = handleResetSession()
            setTableAlias(tempTableAlias)
            handleSend(effectivePrompt, newId, true, tempTableAlias)
          }
          setConfirmCallback(() => callback)
          setShowConfirmModal(true)
        }, 0)
        return
      }

      // Check if user is logged in
      if (!user && !userNm) {
        setShowLoginModal(true)
        return
      }

      setIsLoading(true)
      try {
        const postData = {
          prompt: effectivePrompt
        }
        

        // Set firstQuery and tableAlias for session if absent (supports replay where session id provided directly)
        const targetSessionId = sessionIdForFirstQuery !== null ? sessionIdForFirstQuery : currentSessionId

        // topicId 추가
        postData.topicId = targetSessionId
        
        if (!ignoreContext) {
          if (lastQuery) postData.lastQuery = lastQuery
          if (tableQuery) postData.tableQuery = tableQuery
          if (lastDetailYn) postData.lastDetailYn = lastDetailYn
          if (tableName) postData.tableName = tableName
          postData.tableAlias = overrideTableAlias || selectedInfo || tableAlias
          if (headerColumns) postData.headerColumns = headerColumns
          if (lastColumns) postData.lastColumns = lastColumns
          if (codeMaps) postData.codeMaps = codeMaps
        } else {
          postData.tableAlias = overrideTableAlias || selectedInfo
        }

        const response = await axios.post(qurl, postData)

        const columns = response.data.columns.map(col => ({
          key: col,
          label: col.charAt(0).toUpperCase() + col.slice(1)
        }))
        const headerColumnsData = response.data.headerColumns
          ? response.data.headerColumns.map(col => ({
            key: col,
            label: col.charAt(0).toUpperCase() + col.slice(1)
          }))
          : []

        // 새로 생성된 그리드 객체 저장
        const newGrid = {
          request: postData,
          id: response.data.id || Date.now(),
          query: effectivePrompt,
          data: response.data.data,
          columns: columns,
          headerColumns: headerColumnsData,
          headerData: response.data.headerData,
          detailYn: response.data.detailYn,
          showDetail: false
        }
        
        setGrids(prevGrids => [...prevGrids, newGrid])
        setSessions(prev => prev.map(s => {
          if (s.id === targetSessionId && !s.firstQuery) {
            return { ...s, firstQuery: effectivePrompt, tableAlias: response.data.tableAlias || null }
          }
          return s
        }))

        setLastQuery(response.data.lastQuery || null)
        setTableQuery(response.data.tableQuery || null)
        setLastDetailYn(response.data.lastDetailYn || null)
        setTableName(response.data.tableName || null)
        setTableAlias(response.data.tableAlias || null)
        if (response.data.tableAlias) {
          setSelectedInfo(response.data.tableAlias)
        }
        if (response.data.headerColumns && response.data.headerColumns.length > 0) {
          setHeaderColumns(response.data.headerColumns)
        }
        setLastColumns(response.data.lastColumns || null)
        setCodeMaps(response.data.codeMaps || null)

        // Clear input only if not a replay (avoid flashing same text before send)
        setQuery('')
        
        return newGrid
      } catch (error) {
        console.error('API Error:', error)
        setAlertMessage(translations[language].apiError)
        setShowAlert(true)
      } finally {
        setIsLoading(false)
      }
    }
  }

  // Replay a previous session's first query by starting a new session and auto-running it
  const handleReplay = (session) => {
    if (!session || !session.firstQuery) return
    // Reset conversation state
    clearSessionState()
    setCurrentSessionId(session.id)

    // get logs for the topic from backend and set grids
    const runReplay = async () => {
      setIsLoading(true)
      try {
        const response = await axios.get('/api/logs/' + session.id)
        
        if (response.data && response.data.length > 0) {
          const newGrids = response.data.map(log => {
            // JSON 문자열을 객체로 변환
            const logResponse = log.response ? (typeof log.response === 'string' ? JSON.parse(log.response) : log.response) : {}
            const logRequest = log.request ? (typeof log.request === 'string' ? JSON.parse(log.request) : log.request) : {}
            
            const columns = logResponse.columns ? logResponse.columns.map(col => ({
              key: col,
              label: col.charAt(0).toUpperCase() + col.slice(1)
            })) : []
            
            const headerColumns = logResponse.headerColumns ? logResponse.headerColumns.map(col => ({
              key: col,
              label: col.charAt(0).toUpperCase() + col.slice(1)
            })) : []
            
            return {
              request: logRequest,
              id: logResponse.id || Date.now(),
              query: logRequest.prompt || '',
              data: logResponse.data || [],
              columns: columns,
              headerColumns: headerColumns,
              headerData: logResponse.headerData || [],
              detailYn: logResponse.detailYn || 'N',
              showDetail: false
            }
          })
          
          setGrids(newGrids)
          
          // 마지막 로그의 response로 컨텍스트 정보 상태 업데이트
          const lastLog = response.data[response.data.length - 1]
          if (lastLog && lastLog.response) {
            const lastResponse = typeof lastLog.response === 'string' ? JSON.parse(lastLog.response) : lastLog.response
            setLastQuery(lastResponse.lastQuery || null)
            setTableQuery(lastResponse.tableQuery || null)
            setLastDetailYn(lastResponse.lastDetailYn || null)
            setTableName(lastResponse.tableName || null)
            setTableAlias(lastResponse.tableAlias || null)
            if (lastResponse.tableAlias) {
              setSelectedInfo(lastResponse.tableAlias)
            }
            if (lastResponse.headerColumns && lastResponse.headerColumns.length > 0) {
              setHeaderColumns(lastResponse.headerColumns)
            }
            setLastColumns(lastResponse.lastColumns || null)
            setCodeMaps(lastResponse.codeMaps || null)
          }
        }
      } catch (error) {
        console.error('Error loading logs:', error)
        setAlertMessage(translations[language].apiError || 'Failed to load conversation history')
        setShowAlert(true)
      } finally {
        setIsLoading(false)
      }
    }
    
    runReplay()
  }

  // 차트 생성 공통 함수
  const generateChart = async (payload, gridId) => {
    setIsLoading(true)
    try {
      const response = await axios.post('/api/chart', payload)
      const chartData = response.data

      setChartResult(chartData)
      setActiveChartType(payload.chartType)
      setActiveGridForChart(gridId)
      setShowChartResultModal(true)
    } catch (error) {
      console.error('Chart generation failed:', error)
      setAlertMessage(translations[language].chartError || 'Failed to generate chart. Please try again.')
      setShowAlert(true)
    } finally {
      setIsLoading(false)
    }
  }

  const handleVoiceInput = () => {
    // Web Speech API 지원 확인
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    
    if (!SpeechRecognition) {
      setAlertMessage(translations[language].speechNotSupported || '음성 인식이 지원되지 않는 브라우저입니다.')
      setShowAlert(true)
      return
    }

    if (isListening) {
      // 청취 중지
      console.log('🛑 음성 인식 중지')
      if (recognitionRef.current) {
        recognitionRef.current.stop()
      }
      setIsListening(false)
      return
    }

    // 즉시 음성 인식 시작
    startVoiceRecognition()
  }

  const startVoiceRecognition = () => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    
    console.log('🎯 음성 인식 시작 준비')
    const recognition = new SpeechRecognition()
    recognition.lang = language === 'ko' ? 'ko-KR' : 'en-US'
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    let finalTranscript = ''
    let isProcessing = false
    let speechDetected = false
    let audioDetected = false

    recognition.onstart = () => {
      console.log('🎤 음성 인식 활성화!')
      console.log('%c🔴 지금 바로 크게 말씀하세요! 🔴', 'color: white; font-size: 24px; font-weight: bold; background: red; padding: 10px;')
      setIsListening(true)
    }

    recognition.onspeechstart = () => {
      console.log('🗣️ 음성 감지됨!')
      speechDetected = true
    }

    recognition.onspeechend = () => {
      console.log('🤐 음성 종료')
    }

    recognition.onaudiostart = () => {
      console.log('🔊 오디오 입력 감지 시작')
      audioDetected = true
      console.log('%c마이크가 작동 중입니다. 지금 말씀하세요!', 'color: green; font-size: 16px; font-weight: bold;')
    }

    recognition.onaudioend = () => {
      console.log('🔇 오디오 입력 종료')
      if (!speechDetected && audioDetected) {
        console.log('⚠️ 경고: 오디오는 감지되었으나 음성은 감지되지 않음 - 더 크게 말씀하세요!')
      }
    }

    recognition.onaudiostart = () => {
      console.log('🔊 오디오 입력 감지 시작')
    }

    recognition.onaudioend = () => {
      console.log('🔇 오디오 입력 종료')
    }

    recognition.onresult = (event) => {
      console.log('📝 음성 인식 결과 수신:', event.results)
      
      let interimTranscript = ''
      
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript
        if (event.results[i].isFinal) {
          finalTranscript += transcript + ' '
          console.log('✅ 최종 결과:', transcript)
        } else {
          interimTranscript += transcript
          console.log('⏳ 중간 결과:', transcript)
        }
      }
      
      // 입력창에 표시 (최종 + 중간 결과)
      const displayText = (finalTranscript + interimTranscript).trim()
      setQuery(displayText)
      console.log('📄 입력창 업데이트:', displayText)
      
      // 최종 결과가 있고 아직 처리 중이 아니면 자동 전송 준비
      if (finalTranscript.trim() && !isProcessing) {
        console.log('🚀 최종 결과 확정 - 2초 후 자동 전송 예정')
        isProcessing = true
        
        // 2초 후에도 추가 음성이 없으면 전송
        setTimeout(() => {
          if (recognitionRef.current) {
            console.log('🛑 음성 인식 중지 (자동)')
            recognitionRef.current.stop()
          }
          const textToSend = finalTranscript.trim()
          console.log('📤 질의 전송:', textToSend)
          handleSend(textToSend)
        }, 2000)
      }
    }

    recognition.onerror = (event) => {
      console.error('❌ 음성 인식 오류:', event.error)
      console.log('오류 상세:', event)
      setIsListening(false)
      
      if (event.error === 'no-speech') {
        if (!speechDetected) {
          setAlertMessage('음성이 감지되지 않았습니다.\n\n확인사항:\n1. 마이크가 제대로 연결되어 있나요?\n2. 마이크 버튼을 누른 직후 바로 말씀하세요\n3. 브라우저 주소창 옆 마이크 아이콘을 확인하세요\n4. 시스템 설정에서 마이크가 활성화되어 있나요?')
        } else {
          // 음성은 감지되었지만 no-speech 오류 발생 - 일시적 문제일 수 있음
          console.log('⚠️ 음성은 감지되었으나 no-speech 오류 발생 (무시)')
          return
        }
        setShowAlert(true)
      } else if (event.error === 'audio-capture') {
        setAlertMessage('마이크에 접근할 수 없습니다.\n\n마이크가 연결되어 있고\n다른 앱에서 사용 중이지 않은지 확인해주세요.')
        setShowAlert(true)
      } else if (event.error === 'not-allowed') {
        setAlertMessage('마이크 권한이 거부되었습니다.\n\n브라우저 설정(주소창 옆 자물쇠 아이콘)에서\n마이크 권한을 허용해주세요.')
        setShowAlert(true)
      } else if (event.error !== 'aborted') {
        setAlertMessage(`음성 인식 오류: ${event.error}\n\n다시 시도해주세요.`)
        setShowAlert(true)
      }
    }

    recognition.onend = () => {
      console.log('🔚 음성 인식 세션 종료')
      setIsListening(false)
      recognitionRef.current = null
    }

    recognitionRef.current = recognition
    try {
      recognition.start()
      console.log('✨ recognition.start() 호출 성공!')
      console.log('💡 팁: 마이크 버튼이 빨간색으로 깜박이는 동안 말씀하세요')
    } catch (error) {
      console.error('💥 음성 인식 시작 실패:', error)
      setAlertMessage('음성 인식을 시작할 수 없습니다.\n\n페이지를 새로고침 후 다시 시도해주세요.')
      setShowAlert(true)
      setIsListening(false)
    }
  }

  const handleChartPromptSubmit = async (prompt, chartType) => {
    console.log('Chart Prompt:', prompt, 'Chart Type:', chartType, 'Grid ID:', activeGridForChart)

    const grid = grids.find(g => g.id === activeGridForChart)
    if (!grid) {
      console.error('Grid not found for ID:', activeGridForChart)
      return
    }

    // prompt가 공백이 아니면 새 쿼리 실행 후 차트 생성
    if (prompt && prompt.trim()) {
      const combinedQuery = `${grid.query} ${prompt}`
      console.log('Combined query:', combinedQuery)
      setShowChartModal(false)
      
      const newGrid = await handleSend(combinedQuery)
      console.log('New grid received from handleSend:', newGrid)
      
      if (newGrid) {
        console.log('Using new grid data - columns:', newGrid.columns.length, 'rows:', newGrid.data.length)
        const payload = {
          prompt: prompt,
          chartType,
          columns: newGrid.columns,
          data: newGrid.data
        }
        console.log('Payload for chart generation:', { chartType, columnsCount: payload.columns.length, dataRowCount: payload.data.length })
        await generateChart(payload, newGrid.id)
      } else {
        console.error('handleSend did not return newGrid')
        setAlertMessage(translations[language].apiError || 'Failed to create new query result')
        setShowAlert(true)
      }
      return
    }

    // prompt가 없으면 기존 그리드로 차트 생성
    console.log('Using existing grid data - columns:', grid.columns.length, 'rows:', grid.data.length)
    setShowChartModal(false)
    const payload = {
      prompt: '',
      chartType,
      columns: grid.columns,
      data: grid.data
    }
    await generateChart(payload, grid.id)
  }

  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [grids])

  // Check session on mount (restore user state after refresh)
  useEffect(() => {
    const checkSession = async () => {
      try {
        const response = await axios.get('/api/check-session')
        if (response.data && response.data.user) {
          // Restore user state from session
          setAuth(response.data.auth || 'GUEST')
          setInfos(response.data.infos || null)
          setInfoColumns(response.data.infoColumns || null)
          setLevel(response.data.level || 9)
          setUserNm(response.data.user_nm || null)
          setUser(response.data.user)
          setAutoLogoutSec(response.data.autoLogoutSec || 0)
          
          // topics 로드
          if (response.data.topics && response.data.topics.length > 0) {
            const topicSessions = response.data.topics.map(topic => ({
              id: topic.topicId,
              firstQuery: topic.firstQuery,
              tableAlias: topic.tableAlias,
              startedAt: new Date(topic.startedAt || topic.addDate + ' ' + topic.addTime),
              isFromBackend: true
            }))
            setSessions(topicSessions)
            setHasMoreTopics(true)
          } else {
            setHasMoreTopics(false)
          }
        }
      } catch (error) {
        console.log('No active session')
      }
    }
    checkSession()
  }, [])

  // Initialize first session on mount if absent
  useEffect(() => {
    if (sessions.length === 0) {
      const newId = Date.now()
      setSessions([{ id: newId, firstQuery: null, tableAlias: null, startedAt: new Date() }])
      setCurrentSessionId(newId)
    }
  }, [sessions.length])

  // After alert is closed, open UserInfoModal if requested
  useEffect(() => {
    if (!showAlert && openUserInfoAfterAlert) {
      setShowUserInfoModal(true)
      setOpenUserInfoAfterAlert(false)
    }
  }, [showAlert, openUserInfoAfterAlert])

  // 자동 로그아웃 타이머 설정 및 사용자 활동 감지
  useEffect(() => {
    if (autoLogoutSec > 0 && user) {
      resetAutoLogoutTimer()

      // 사용자 활동 감지 이벤트
      const handleUserActivity = () => {
        resetAutoLogoutTimer()
      }

      window.addEventListener('mousemove', handleUserActivity)
      window.addEventListener('keydown', handleUserActivity)
      window.addEventListener('click', handleUserActivity)
      window.addEventListener('scroll', handleUserActivity)

      return () => {
        window.removeEventListener('mousemove', handleUserActivity)
        window.removeEventListener('keydown', handleUserActivity)
        window.removeEventListener('click', handleUserActivity)
        window.removeEventListener('scroll', handleUserActivity)
        if (autoLogoutTimerRef.current) {
          clearTimeout(autoLogoutTimerRef.current)
        }
      }
    }
  }, [autoLogoutSec, user])

  return (
    <div className="flex flex-col h-screen bg-slate-900">
      {/* Fixed header with input */}
      <div className="sticky top-0 bg-gradient-to-b from-slate-900 to-slate-900/95 p-4 shadow-xl backdrop-blur-sm z-50" ref={inputContainerRef}>
        {/* Header actions (float right) */}
        <div className="flex items-center gap-2 z-[60] md:absolute md:top-4 md:right-4 mb-2 md:mb-0">
          <button
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors text-slate-300 hover:text-white font-medium"
            onClick={() => setLanguage(prev => prev === 'ko' ? 'en' : 'ko')}
            title="언어 변경 / Change Language"
          >
            {language === 'ko' ? 'EN' : 'KO'}
          </button>
          {level === 1 && (
            <>
              <button
                className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                onClick={() => {
                  setShowUserManagement(true)
                }}
                title={translations[language].userMgmt}
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
                title={translations[language].authMgmt}
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
                title={translations[language].infoMgmt}
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
            title={userNm ? translations[language].userInfo : translations[language].login}
          >
            <svg className="h-6 w-6 text-slate-300 hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </button>
        </div>
        {/* Align header content with main content (reserve sidebar width) */}
        <div className="flex items-center">
          <div className="hidden md:flex md:items-center md:w-64 md:pl-4">
            <div
              className="flex items-center gap-3 cursor-pointer select-none hover:opacity-90"
              role="button"
              tabIndex={0}
              title={translations[language].newChatQ}
              onClick={handleResetSession}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  handleResetSession()
                }
              }}
            >
              <img src={chatqLogo} alt="ChatQ Logo" className="h-10 w-10" />
              <span className="text-2xl font-bold text-white">ChatQ</span>
            </div>
          </div>
          <div className="flex-1 pr-2">
            <div className="relative">
              <img src={chatqLogo} alt="ChatQ" className="absolute left-3 top-1/2 -translate-y-1/2 h-6 w-6 cursor-pointer" onClick={handleResetSession} />
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !isLoading) {
                    handleSend()
                  }
                }}
                placeholder={translations[language].inputPlaceholder}
                className="w-full p-3 pl-12 pr-24 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 focus:outline-none focus:border-slate-500 appearance-none !bg-slate-800 !text-slate-200 !border-slate-700"
                autoComplete="off"
              />
              <button
                onClick={handleVoiceInput}
                className={`absolute right-12 top-1/2 -translate-y-1/2 p-2 rounded-md transition-all ${
                  isListening 
                    ? 'text-red-500 bg-red-500/20 animate-pulse' 
                    : 'text-slate-400 hover:text-slate-300 hover:bg-slate-700'
                }`}
                aria-label="Voice Input"
                title={isListening ? (translations[language].stopListening || '청취 중지') : (translations[language].voiceInput || '음성 입력')}
              >
                <svg className={`h-5 w-5 ${isListening ? 'scale-110' : ''} transition-transform`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                </svg>
                {isListening && (
                  <span className="absolute inset-0 rounded-md bg-red-500/30 animate-ping"></span>
                )}
              </button>
              <button
                onClick={() => handleSend()}
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
          <div className="hidden lg:block lg:w-64" />
        </div>
      </div>

      {/* Layout with left sidebar for first query history */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left sidebar - Search target and columns */}
        <aside className="hidden lg:block w-64 bg-slate-950 overflow-y-auto flex-shrink-0">
          <div className="p-4">
            <div className="mb-4">
              <label className="text-slate-400 text-sm font-semibold block mb-2">{translations[language].searchTarget}</label>
              <select
                value={selectedInfo}
                onChange={(e) => {
                  setSelectedInfo(e.target.value)
                }}
                className="w-full px-3 py-2.5 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 focus:outline-none focus:border-slate-500 text-sm"
              >
                <option value="">{translations[language].searchTargetAll}</option>
                {infos && infos.map((info) => (
                  <option key={info} value={info}>{info}</option>
                ))}
              </select>
            </div>
            {selectedInfo && selectedInfo !== '' ? (
              <>
                <h2 className="text-slate-400 text-sm font-semibold mb-3">
                  {selectedInfo} {translations[language].columnInfo}
                </h2>
                {infoColumns && infoColumns[selectedInfo] ? (
                  <div className="space-y-1">
                    {infoColumns[selectedInfo].map((column, index) => (
                      <div
                        key={index}
                        className="text-xs text-slate-300 bg-slate-800/50 px-3 py-2 rounded hover:bg-slate-800 transition-colors"
                      >
                        {column}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-slate-500 text-xs">
                    {translations[language].noColumnInfo}
                  </div>
                )}
              </>
            ) : (
              <div className="mt-4 p-3 bg-slate-800/50 rounded-lg border border-slate-700/50">
                <p className="text-slate-400 text-xs leading-relaxed">
                  {translations[language].selectSearchTargetHint}
                </p>
              </div>
            )}
          </div>
        </aside>
        {/* Main content */}
        <div className="flex-1 overflow-y-auto bg-slate-900">
          <div className="p-4">
            <div className="space-y-6 mt-4">
              {grids.length === 0 && (
                <div className="flex items-center justify-center min-h-[60vh]">
                  <div className="text-center text-slate-500 max-w-2xl">
                    <div className="font-semibold text-lg mb-3">{translations[language].tipsTitle}</div>
                    <div className="text-sm mb-4 text-left">{translations[language].tipsSubtitle}</div>
                    <ul className="list-disc text-left inline-block space-y-1.5">
                      {translations[language].tips.map((tip, index) => (
                        <li key={index}>{tip}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              )}
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
                        title={translations[language].copyQuery}
                      >
                        <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                        </svg>
                      </button>
                      {grid.detailYn === 'Y' && (
                        <button
                          onClick={() => toggleShowDetail(grid.id)}
                          className={`p-1 hover:bg-slate-700 rounded-md transition-colors ${grid.showDetail ? 'text-blue-500' : 'text-slate-400 hover:text-slate-300'}`}
                          title={grid.showDetail ? translations[language].showHeaderData : translations[language].showDetailData}
                        >
                          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                          </svg>
                        </button>
                      )}
                      <button
                        onClick={() => {
                          // Generate HTML table for Excel compatibility
                          const html = `
                            <html>
                              <head><meta charset="UTF-8"></head>
                              <body>
                                <table>
                                  <thead>
                                    <tr>${grid.columns.map(col => `<th>${col.label}</th>`).join('')}</tr>
                                  </thead>
                                  <tbody>
                                    ${grid.data.map(row => `<tr>${grid.columns.map(col => `<td>${row[col.key] ?? ''}</td>`).join('')}</tr>`).join('')}
                                  </tbody>
                                </table>
                              </body>
                            </html>`;
                          const blob = new Blob(['\uFEFF' + html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
                          const link = document.createElement('a');
                          link.href = URL.createObjectURL(blob);
                          link.download = `chatq_${grid.id}.xls`;
                          link.click();
                        }}
                        className="p-1 hover:bg-slate-700 rounded-md transition-colors text-green-500 hover:text-green-400"
                        title={translations[language].downloadExcel}
                      >
                        <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                      </button>
                      <button
                        onClick={() => {
                          setActiveGridForChart(grid.id)
                          setShowChartModal(true)
                        }}
                        className="p-1 hover:bg-slate-700 rounded-md transition-colors text-purple-500 hover:text-purple-400"
                        title={translations[language].generateChart || "Generate Chart"}
                      >
                        <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
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
                    showDetail={grid.showDetail}
                  />
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
          </div>
        </div>
        {/* Right sidebar - ChatQ topics */}
        <aside 
          ref={topicsSidebarRef}
          onScroll={(e) => {
            const element = e.target
            const bottom = element.scrollHeight - element.scrollTop <= element.clientHeight + 50
            if (bottom && hasMoreTopics && !isLoadingTopics) {
              loadMoreTopics()
            }
          }}
          className="hidden md:block w-64 bg-slate-950 overflow-y-auto flex-shrink-0"
        >
          <div className="p-4">
            <h2 className="text-slate-400 text-sm font-semibold mb-3">{translations[language].myChatQTopics}</h2>
            <ul className="space-y-2 pr-1">
              {sessions.filter(s => s.firstQuery).length === 0 && (
                <li className="text-slate-500 text-xs">{translations[language].noHistory}</li>
              )}
              {sessions
                .filter(s => s.firstQuery)
                .sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt))
                .map(s => (
                  <li key={s.id} className="group relative">
                    <button
                      type="button"
                      onClick={() => handleReplay(s)}
                      className="w-full text-left text-xs text-slate-300 hover:bg-slate-800/60 rounded px-1 py-1"
                      title={`${translations[language].replay}: ${s.firstQuery}`}
                    >
                      <span
                        className="block w-full px-2 py-1 rounded bg-slate-800/70 group-hover:bg-slate-700/70 transition-colors overflow-hidden whitespace-nowrap text-ellipsis"
                      >
                        {s.tableAlias ? `[${s.tableAlias}] ${s.firstQuery}` : s.firstQuery}
                      </span>
                    </button>
                    {/* Hover full content tooltip */}
                    <div className="pointer-events-none absolute left-0 top-full mt-1 z-10 hidden group-hover:block bg-slate-800 text-slate-200 text-xs p-2 rounded shadow-lg max-w-xs whitespace-pre-wrap break-words">
                      {s.tableAlias ? `[${s.tableAlias}] ${s.firstQuery}` : s.firstQuery}
                    </div>
                  </li>
                ))}
              {isLoadingTopics && (
                <li className="text-center py-2">
                  <span className="text-slate-500 text-xs">Loading...</span>
                </li>
              )}
            </ul>
          </div>
        </aside>
        {/* Close flex layout */}
      </div>

      {/* Login Modal */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onSubmit={handleLogin}
        language={language}
        translations={translations}
        isLoading={isLoginLoading}
      />

      {/* Alert Modal */}
      {
        showAlert && (
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
                  {translations[language].confirm}
                </button>
              </div>
            </div>
          </Modal>
        )
      }

      {/* User Info Modal */}
      <UserInfoModal
        isOpen={showUserInfoModal}
        onClose={() => setShowUserInfoModal(false)}
        user={user}
        userName={userNm}
        auth={auth}
        level={level}
        infos={infos ? infos.join(', ') : ''}
        language={language}
        translations={translations}
        onLogout={() => handleLogout(false)}
      />

      {/* User Management Modal */}
      <UserManagement
        isOpen={showUserManagement}
        onClose={() => setShowUserManagement(false)}
        language={language}
      />

      {/* Auth Management Modal */}
      <AuthManagement
        isOpen={showAuthManagement}
        onClose={() => setShowAuthManagement(false)}
        language={language}
      />

      {/* Info Management Modal */}
      <InfoManagement
        isOpen={showInfoManagement}
        onClose={() => setShowInfoManagement(false)}
        language={language}
      />

      {/* Chart Prompt Modal */}
      <ChartPromptModal
        isOpen={showChartModal}
        onClose={() => setShowChartModal(false)}
        onSubmit={handleChartPromptSubmit}
        language={language}
        translations={translations}
      />

      {/* Chart Result Modal */}
      <ChartResultModal
        isOpen={showChartResultModal}
        onClose={() => setShowChartResultModal(false)}
        chartData={chartResult}
        chartType={activeChartType}
      />

      {/* Confirm Modal */}
      <ConfirmModal
        isOpen={showConfirmModal}
        onClose={() => {
          setShowConfirmModal(false)
          setConfirmMessage('')
          setConfirmCallback(null)
        }}
        onConfirm={() => {
          if (confirmCallback && typeof confirmCallback === 'function') {
            confirmCallback()
          }
          setShowConfirmModal(false)
          setConfirmMessage('')
          setConfirmCallback(null)
        }}
        message={confirmMessage}
        language={language}
        translations={translations}
      />
    </div >
  )
}

export default App
