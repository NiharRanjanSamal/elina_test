import { useState, useEffect } from 'react'
import api from '../services/api'

const PlanVersionComparison = ({ versionId1, versionId2, onClose }) => {
  const [comparison, setComparison] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (versionId1 && versionId2) {
      fetchComparison()
    }
  }, [versionId1, versionId2])

  const fetchComparison = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await api.get(`/api/plans/compare/${versionId1}/${versionId2}`)
      setComparison(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch comparison')
    } finally {
      setLoading(false)
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'SAME':
        return 'text-gray-600'
      case 'INCREASED':
        return 'text-green-600'
      case 'DECREASED':
        return 'text-red-600'
      case 'NEW':
        return 'text-blue-600'
      case 'REMOVED':
        return 'text-orange-600'
      default:
        return 'text-gray-600'
    }
  }

  const getStatusBadge = (status) => {
    const colors = {
      SAME: 'bg-gray-100 text-gray-800',
      INCREASED: 'bg-green-100 text-green-800',
      DECREASED: 'bg-red-100 text-red-800',
      NEW: 'bg-blue-100 text-blue-800',
      REMOVED: 'bg-orange-100 text-orange-800'
    }
    return colors[status] || colors.SAME
  }

  const formatDate = (dateString) => {
    if (!dateString) return '-'
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString()
    } catch {
      return dateString
    }
  }

  const formatNumber = (num) => {
    if (num == null) return '-'
    return parseFloat(num).toFixed(2)
  }

  if (loading) {
    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-2/3 shadow-lg rounded-md bg-white">
          <div className="text-center py-8">Loading comparison...</div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-2/3 shadow-lg rounded-md bg-white">
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md mb-4">
            {error}
          </div>
          <button
            onClick={onClose}
            className="w-full px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
          >
            Close
          </button>
        </div>
      </div>
    )
  }

  if (!comparison) {
    return null
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-10 mx-auto p-6 border w-11/12 md:w-4/5 lg:w-3/4 shadow-lg rounded-md bg-white max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex justify-between items-center mb-4 border-b pb-4">
          <h2 className="text-2xl font-bold text-gray-900">Version Comparison</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Version Info */}
        <div className="grid grid-cols-2 gap-4 mb-6">
          <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
            <h3 className="font-semibold text-blue-900 mb-2">
              Version {comparison.version1?.versionNo}
            </h3>
            <p className="text-sm text-blue-700">
              Date: {formatDate(comparison.version1?.versionDate)}
            </p>
            {comparison.version1?.isActive && (
              <span className="inline-block mt-2 px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                Active
              </span>
            )}
          </div>
          <div className="bg-purple-50 border border-purple-200 rounded-md p-4">
            <h3 className="font-semibold text-purple-900 mb-2">
              Version {comparison.version2?.versionNo}
            </h3>
            <p className="text-sm text-purple-700">
              Date: {formatDate(comparison.version2?.versionDate)}
            </p>
            {comparison.version2?.isActive && (
              <span className="inline-block mt-2 px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                Active
              </span>
            )}
          </div>
        </div>

        {/* Summary */}
        {comparison.summary && (
          <div className="bg-gray-50 border border-gray-200 rounded-md p-4 mb-6">
            <h3 className="font-semibold text-gray-900 mb-3">Comparison Summary</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="text-gray-600">Total Days (V1):</span>
                <span className="ml-2 font-medium">{comparison.summary.totalDaysVersion1}</span>
              </div>
              <div>
                <span className="text-gray-600">Total Days (V2):</span>
                <span className="ml-2 font-medium">{comparison.summary.totalDaysVersion2}</span>
              </div>
              <div>
                <span className="text-gray-600">Common Days:</span>
                <span className="ml-2 font-medium">{comparison.summary.commonDays}</span>
              </div>
              <div>
                <span className="text-gray-600">New Days:</span>
                <span className="ml-2 font-medium text-blue-600">{comparison.summary.newDays}</span>
              </div>
              <div>
                <span className="text-gray-600">Removed Days:</span>
                <span className="ml-2 font-medium text-orange-600">{comparison.summary.removedDays}</span>
              </div>
              <div>
                <span className="text-gray-600">Total Qty (V1):</span>
                <span className="ml-2 font-medium">{formatNumber(comparison.summary.totalQtyVersion1)}</span>
              </div>
              <div>
                <span className="text-gray-600">Total Qty (V2):</span>
                <span className="ml-2 font-medium">{formatNumber(comparison.summary.totalQtyVersion2)}</span>
              </div>
              <div>
                <span className="text-gray-600">Difference:</span>
                <span className={`ml-2 font-medium ${
                  comparison.summary.totalDifference >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  {formatNumber(comparison.summary.totalDifference)}
                </span>
              </div>
            </div>
            {comparison.summary.changeStatistics && (
              <div className="mt-4 pt-4 border-t">
                <h4 className="text-sm font-semibold text-gray-700 mb-2">Change Statistics:</h4>
                <div className="flex flex-wrap gap-2">
                  {Object.entries(comparison.summary.changeStatistics).map(([status, count]) => (
                    <span
                      key={status}
                      className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusBadge(status)}`}
                    >
                      {status}: {count}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Comparison Lines */}
        <div className="flex-1 overflow-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50 sticky top-0">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Quantity (V{comparison.version1?.versionNo})
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Quantity (V{comparison.version2?.versionNo})
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Difference
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {comparison.comparisonLines && comparison.comparisonLines.map((line, index) => (
                <tr key={index} className="hover:bg-gray-50">
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
                    {formatDate(line.workDate || line.plannedDate)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                    {formatNumber(line.qtyVersion1)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                    {formatNumber(line.qtyVersion2)}
                  </td>
                  <td className={`px-4 py-3 whitespace-nowrap text-sm font-medium ${
                    line.difference >= 0 ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {line.difference >= 0 ? '+' : ''}{formatNumber(line.difference)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusBadge(line.status)}`}>
                      {line.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="mt-4 pt-4 border-t flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

export default PlanVersionComparison

