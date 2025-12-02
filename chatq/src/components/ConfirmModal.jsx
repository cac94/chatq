const ConfirmModal = ({ isOpen, onClose, onConfirm, title, message, language, translations }) => {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-[9999]" onClick={onClose}>
      <div className="bg-slate-800 rounded-lg shadow-2xl w-96 mx-4" onClick={(e) => e.stopPropagation()}>
        <div className="p-6">
          {/* Icon */}
          <div className="flex justify-center mb-4">
            <svg className="h-12 w-12 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>

          {/* Title */}
          {title && (
            <h3 className="text-xl font-semibold text-white text-center mb-3">
              {title}
            </h3>
          )}

          {/* Message */}
          <p className="text-slate-300 text-center mb-6 whitespace-pre-line">
            {message}
          </p>

          {/* Buttons */}
          <div className="flex gap-3">
            <button
              onClick={onClose}
              className="flex-1 py-2.5 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition-colors font-medium"
            >
              {translations[language].cancel}
            </button>
            <button
              onClick={() => {
                onConfirm()
                onClose()
              }}
              className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors font-medium"
              autoFocus
            >
              {translations[language].confirm}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ConfirmModal
