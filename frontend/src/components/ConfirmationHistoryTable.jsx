const ConfirmationHistoryTable = ({ history = [], canUndo, onUndo, loading }) => {
  if (loading) {
    return <div className="py-6 text-center text-sm text-gray-500">Loading confirmations...</div>
  }

  if (!history.length) {
    return (
      <div className="py-6 text-center text-sm text-gray-500">
        No confirmations have been recorded yet.
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Date
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Confirmed Qty
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Remarks
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
              Created By
            </th>
            <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-gray-500">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {history.map((item) => (
            <tr key={item.confirmationId}>
              <td className="px-4 py-3 text-sm text-gray-900">{item.confirmationDate}</td>
              <td className="px-4 py-3 text-sm font-medium text-gray-900">{item.confirmedQty}</td>
              <td className="px-4 py-3 text-sm text-gray-600">{item.remarks || '—'}</td>
              <td className="px-4 py-3 text-sm text-gray-600">{item.createdBy || 'System'}</td>
              <td className="px-4 py-3 text-right">
                {canUndo && (
                  <button
                    onClick={() => onUndo(item)}
                    className="rounded-md border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                  >
                    Undo
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default ConfirmationHistoryTable


