import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import './App.css'
import DataGrid from './components/DataGrid'
import chatqLogo from './assets/chatqicon51x51.png'

const App = () => {
  const [grids, setGrids] = useState([])
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const bottomRef = useRef(null)
  const inputContainerRef = useRef(null)
  const qurl = 'http://localhost:8080/api/chatq' // Your API endpoint

  // Store lastQuery and tableQuery from previous response
  const [lastQuery, setLastQuery] = useState(null)
  const [tableQuery, setTableQuery] = useState(null)
  const [detailYn, setDetailYn] = useState(null)
  const [tableName, setTableName] = useState(null)
  const [headerColumns, setHeaderColumns] = useState(null)

  const handleSend = async () => {
    if (query.trim()) {
      setIsLoading(true)
      try {
        const postData = {
          prompt: query
        }
        if (lastQuery) postData.lastQuery = lastQuery
        if (tableQuery) postData.tableQuery = tableQuery
        if (detailYn) postData.detailYn = detailYn
        if (tableName) postData.tableName = tableName
        if (headerColumns) postData.headerColumns = headerColumns

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
        setDetailYn(response.data.detailYn || null)
        setTableName(response.data.tableName || null)
        setHeaderColumns(response.data.headerColumns || null)

        setQuery('') // Clear input after sending
      } catch (error) {
        console.error('API Error:', error)
        alert('조회를 실패했습니다. 좀 더 구체적으로 입력해보시거나 상단의 ChatQ 로고를 클릭하여 새로 시작해보세요.')
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
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                setGrids([])
                setLastQuery(null)
                setTableQuery(null)
                setDetailYn(null)
                setTableName(null)
                setHeaderColumns(null)
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
    </div>
  )
}

export default App
