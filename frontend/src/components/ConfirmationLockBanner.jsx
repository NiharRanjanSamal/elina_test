const ConfirmationLockBanner = ({ lockDate }) => {
  if (!lockDate) {
    return (
      <div className="rounded-md border border-yellow-200 bg-yellow-50 px-4 py-3 text-sm text-yellow-800">
        No confirmation lock is active. Updates are editable.
      </div>
    )
  }

  const today = new Date().toISOString().split('T')[0]
  const isBlocking = lockDate >= today
  const styles = isBlocking
    ? 'border-red-200 bg-red-50 text-red-800'
    : 'border-yellow-200 bg-yellow-50 text-yellow-800'

  return (
    <div className={`rounded-md px-4 py-3 text-sm ${styles}`}>
      WBS is locked until <span className="font-semibold">{lockDate}</span>.
      {isBlocking ? ' Task updates before this date are frozen.' : ' Future dates remain editable.'}
    </div>
  )
}

export default ConfirmationLockBanner


