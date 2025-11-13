import { useState } from 'react'
import Modal from './Modal'
import './DataGrid.css'

const DataGrid = ({ data, columns }) => {
  const [selectedRow, setSelectedRow] = useState(null)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const handleRowClick = (row) => {
    setSelectedRow(row)
    setIsModalOpen(true)
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'Active': return 'text-emerald-500'
      case 'Pending': return 'text-yellow-500'
      case 'Inactive': return 'text-red-500'
      default: return 'text-slate-300'
    }
  }

  const isNumeric = (value) => {
    return !isNaN(parseFloat(value)) && isFinite(value)
  }

  return (
    <div className="overflow-x-auto overflow-y-auto max-h-[67vh] custom-scrollbar">
      <div className="min-w-max">
        {/* Headers */}
        <div className="flex gap-4 p-4 bg-slate-800 rounded-t-lg border-b border-slate-700 sticky top-0 z-10">
          {columns.map(col => (
            <div key={col.key} className="font-semibold text-slate-200 min-w-[150px] text-center">
              {col.label}
            </div>
          ))}
        </div>
        
        {/* Rows */}
        {data.map((row, index) => (
          <div 
            key={index}
            onClick={() => handleRowClick(row)}
            className={`flex gap-4 p-4 bg-slate-800 
              ${index === data.length - 1 ? 'rounded-b-lg' : 'border-b border-slate-700'}
              hover:bg-slate-700 cursor-pointer transition-colors`}
          >
            {columns.map(col => {
              const value = row[col.key]
              const isNum = isNumeric(value)
              return (
                <div key={col.key} className={`min-w-[150px] ${isNum ? 'text-right' : 'text-center'} ${col.key === 'status' ? getStatusColor(value) : "text-slate-300"}`}>
                  {value}
                </div>
              )
            })}
          </div>
        ))}
      </div>

      {/* Detail Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)}>
        {selectedRow && (
          <div className="text-slate-200">
            <h2 className="text-2xl font-bold mb-4">User Details</h2>
            <div className="grid gap-4">
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-700 rounded-lg">
                <div className="font-semibold">ID:</div>
                <div>{selectedRow.id}</div>
              </div>
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-700 rounded-lg">
                <div className="font-semibold">Name:</div>
                <div>{selectedRow.name}</div>
              </div>
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-700 rounded-lg">
                <div className="font-semibold">Email:</div>
                <div>{selectedRow.email}</div>
              </div>
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-700 rounded-lg">
                <div className="font-semibold">Status:</div>
                <div className={getStatusColor(selectedRow.status)}>{selectedRow.status}</div>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default DataGrid