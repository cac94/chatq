import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import './App.css'
import DataGrid from './components/DataGrid'

const App = () => {
  const [grids, setGrids] = useState([])
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const bottomRef = useRef(null)
  const inputContainerRef = useRef(null)
  const qurl = 'http://localhost:8080/api/chatq' // Your API endpoint

  const handleSend = async () => {
    if (query.trim()) {
      setIsLoading(true)
      try {
        const response = await axios.post(qurl, {
          prompt: query
        })
        
        // Convert the response format to match our grid structure
        const columns = response.data.columns.map(col => ({
          key: col,
          label: col.charAt(0).toUpperCase() + col.slice(1) // Capitalize first letter
        }))

        setGrids(prevGrids => [...prevGrids, {
          id: Date.now(),
          query: query,
          data: response.data.data,
          columns: columns
        }])
        
        setQuery('') // Clear input after sending
      } catch (error) {
        console.error('API Error:', error)
        // You might want to show an error message to the user here
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
          <h1 className="text-4xl font-bold text-white mb-6 text-center">
            ChatQ
          </h1>
          <div className="flex gap-2">
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
              className="flex-1 p-3 rounded-lg bg-slate-800 text-slate-200 border border-slate-700 focus:outline-none focus:border-slate-500"
            />
            <button 
              onClick={handleSend}
              disabled={isLoading}
              className={`px-6 py-3 rounded-lg transition-colors flex items-center gap-2
                ${isLoading 
                  ? 'bg-blue-500 cursor-not-allowed' 
                  : 'bg-blue-600 hover:bg-blue-700'}`}
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Loading...
                </>
              ) : 'Send'}
            </button>
          </div>
        </div>
      </div>

      {/* Scrollable content area */}
      <div className="max-w-4xl mx-auto p-4">
        <div className="space-y-6 mt-4">
          {grids.map(grid => (
            <div key={grid.id} className="bg-slate-800 p-4 rounded-lg">
              <div className="text-slate-300 mb-4 font-medium">Query: {grid.query}</div>
              <DataGrid data={grid.data} columns={grid.columns} />
            </div>
          ))}
          <div ref={bottomRef} />
        </div>
      </div>
    </div>
  )
}

export default App
