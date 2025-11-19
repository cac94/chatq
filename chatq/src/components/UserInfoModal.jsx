import Modal from './Modal'

const UserInfoModal = ({ isOpen, onClose, userName, auth, level, infos, onLogout }) => {
  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="w-96 mx-auto">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">사용자 정보</h2>
        <div className="space-y-4 mb-6">
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">이름</div>
            <div className="text-white font-medium">{userName}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">권한</div>
            <div className="text-white font-medium">{auth}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">레벨</div>
            <div className="text-white font-medium">{level}</div>
          </div>
          <div className="bg-slate-700 p-3 rounded">
            <div className="text-slate-400 text-sm mb-1">접근가능정보</div>
            <div className="text-white font-medium">
              {Array.isArray(infos) ? infos.join(', ') : infos}
            </div>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={onLogout}
            className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white rounded transition-colors"
          >
            로그아웃
          </button>
          <button
            onClick={onClose}
            className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
          >
            닫기
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default UserInfoModal
