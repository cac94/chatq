import { useState, useEffect } from 'react';
import Modal from './Modal';
import './DataGrid.css';

const DataGrid = ({ data, columns, headerData, headerColumns, detailYn, showDetail = false }) => {
  const [selectedRows, setSelectedRows] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [sortConfig, setSortConfig] = useState({ key: null, direction: null });
  const [columnWidths, setColumnWidths] = useState({});
  const [resizingColumn, setResizingColumn] = useState(null);

  const handleHeaderClick = (columnKey) => {
    let direction = 'asc';
    if (sortConfig.key === columnKey && sortConfig.direction === 'asc') {
      direction = 'desc';
    }
    setSortConfig({ key: columnKey, direction });
  };

  const handleRowClick = (row) => {
    if (detailYn !== 'Y' || showDetail) return;
    if (data && headerColumns) {
      const matchingRows = data.filter(dataRow =>
        headerColumns.every(col => dataRow[col.key] === row[col.key])
      );
      setSelectedRows(matchingRows);
      setIsModalOpen(true);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'Active':
        return 'text-emerald-500';
      case 'Pending':
        return 'text-yellow-500';
      case 'Inactive':
        return 'text-red-500';
      default:
        return 'text-slate-300';
    }
  };

  const isNumeric = (value) => {
    if (value === null || value === undefined || value === '') return false;
    const str = String(value);
    if (str.length > 1 && str[0] === '0' && /^\d+$/.test(str)) return false;
    return !isNaN(parseFloat(value)) && isFinite(value);
  };

  // Determine which columns and data to display
  const displayColumns = (detailYn === 'Y' && headerColumns && !showDetail) ? headerColumns : columns;
  let displayData = (detailYn === 'Y' && headerColumns && !showDetail) ? headerData : data;

  // Apply sorting if needed
  if (sortConfig.key && displayData) {
    displayData = [...displayData].sort((a, b) => {
      const aVal = a[sortConfig.key];
      const bVal = b[sortConfig.key];
      if (aVal === null || aVal === undefined) return 1;
      if (bVal === null || bVal === undefined) return -1;
      if (isNumeric(aVal) && isNumeric(bVal)) {
        const diff = parseFloat(aVal) - parseFloat(bVal);
        return sortConfig.direction === 'asc' ? diff : -diff;
      }
      const comp = String(aVal).localeCompare(String(bVal));
      return sortConfig.direction === 'asc' ? comp : -comp;
    });
  }

  const getSortIcon = (columnKey) => {
    if (sortConfig.key !== columnKey) return null;
    return sortConfig.direction === 'asc' ? ' ▲' : ' ▼';
  };

  const handleResizeStart = (e, columnKey) => {
    e.stopPropagation();
    setResizingColumn({ key: columnKey, startX: e.clientX, startWidth: columnWidths[columnKey] || 150 });
  };

  const handleResizeMove = (e) => {
    if (!resizingColumn) return;
    const diff = e.clientX - resizingColumn.startX;
    const newWidth = Math.max(50, resizingColumn.startWidth + diff);
    setColumnWidths({ ...columnWidths, [resizingColumn.key]: newWidth });
  };

  const handleResizeEnd = () => {
    setResizingColumn(null);
  };

  useEffect(() => {
    if (resizingColumn) {
      document.addEventListener('mousemove', handleResizeMove);
      document.addEventListener('mouseup', handleResizeEnd);
      return () => {
        document.removeEventListener('mousemove', handleResizeMove);
        document.removeEventListener('mouseup', handleResizeEnd);
      };
    }
  }, [resizingColumn]);

  return (
    <div className="overflow-x-auto overflow-y-auto max-h-[67vh] custom-scrollbar">
      <div className="min-w-max">
        {/* Headers */}
        <div className="flex gap-4 p-4 bg-slate-800 rounded-t-lg border-b border-slate-700 sticky top-0 z-10">
          {displayColumns.map(col => (
            <div
              key={col.key}
              className="font-semibold text-slate-200 flex-shrink-0 text-center cursor-pointer hover:text-blue-400 transition-colors select-none relative overflow-hidden text-sm"
              style={{ width: `${columnWidths[col.key] || 150}px` }}
              onClick={() => handleHeaderClick(col.key)}
              title="클릭하여 정렬"
            >
              {col.label}{getSortIcon(col.key)}
              <div
                className="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-blue-500"
                onMouseDown={(e) => handleResizeStart(e, col.key)}
              />
            </div>
          ))}
        </div>
        {/* Rows */}
        {displayData && displayData.map((row, index) => (
          <div
            key={index}
            onClick={() => handleRowClick(row)}
            className={`flex gap-4 p-4 bg-slate-800 ${index === displayData.length - 1 ? 'rounded-b-lg' : 'border-b border-slate-700'} ${detailYn === 'Y' && !showDetail ? 'hover:bg-slate-700 cursor-pointer' : ''} transition-colors`}
          >
            {displayColumns.map(col => {
              const value = row[col.key];
              const isNum = isNumeric(value);
              return (
                <div
                  key={col.key}
                  className={`flex-shrink-0 overflow-hidden text-sm ${isNum ? 'text-right' : 'text-center'} ${col.key === 'status' ? getStatusColor(value) : "text-slate-300"}`}
                  style={{ width: `${columnWidths[col.key] || 150}px` }}
                  title={value}
                >
                  {value}
                </div>
              );
            })}
          </div>
        ))}
      </div>
      {/* Detail Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)}>
        {selectedRows.length > 0 && (
          <div className="text-slate-200">
            <h2 className="text-2xl font-bold mb-4">상세 정보 ({selectedRows.length}건)</h2>
            <div className="overflow-x-auto overflow-y-auto max-h-[60vh] custom-scrollbar">
              <div className="min-w-max">
                {/* Modal Headers */}
                <div className="flex gap-4 p-4 bg-slate-700 rounded-t-lg border-b border-slate-600 sticky top-0 z-10">
                  {columns.map(col => (
                    <div
                      key={col.key}
                      className="font-semibold text-slate-200 flex-shrink-0 text-center overflow-hidden relative"
                      style={{ width: `${columnWidths[col.key] || 150}px` }}
                    >
                      {col.label}
                      <div
                        className="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-blue-500"
                        onMouseDown={(e) => handleResizeStart(e, col.key)}
                      />
                    </div>
                  ))}
                </div>
                {/* Modal Rows */}
                {selectedRows.map((row, idx) => (
                  <div
                    key={idx}
                    className={`flex gap-4 p-4 bg-slate-700 ${idx === selectedRows.length - 1 ? 'rounded-b-lg' : 'border-b border-slate-600'}`}
                  >
                    {columns.map(col => {
                      const value = row[col.key];
                      const isNum = isNumeric(value);
                      return (
                        <div
                          key={col.key}
                          className={`flex-shrink-0 overflow-hidden ${isNum ? 'text-right' : 'text-center'} ${col.key === 'status' ? getStatusColor(value) : "text-slate-300"}`}
                          style={{ width: `${columnWidths[col.key] || 150}px` }}
                          title={value}
                        >
                          {value}
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default DataGrid;
