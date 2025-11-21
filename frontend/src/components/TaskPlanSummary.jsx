import { useState, useEffect } from 'react'
import api from '../services/api'

/**
 * Task Plan Summary Widget
 * Displays:
 * - Total planned quantity
 * - Total actual quantity (from updates)
 * - Variance
 * - Active version badge
 */
const TaskPlanSummary = ({ taskId }) => {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (taskId) {
      fetchSummary()
    }
  }, [taskId])

  const fetchSummary = async () => {
    try {
      setLoading(true)
      setError('')
      
      // Fetch plan versions to get active version and total planned
      const versionsResponse = await api.get(`/api/plans/task/${taskId}`)
      const versions = versionsResponse.data || []
      const activeVersion = versions.find(v => v.isActive)
      
      let totalPlanned = 0
      let planLines = []
      
      if (activeVersion) {
        const linesResponse = await api.get(`/api/plans/${activeVersion.planVersionId}/lines`)
        planLines = linesResponse.data || []
        totalPlanned = planLines.reduce((sum, line) => sum + (parseFloat(line.plannedQty) || 0), 0)
      }
      
      // Fetch task updates to get total actual
      let totalActual = 0
      try {
        const updatesResponse = await api.get(`/api/task-updates/task/${taskId}`)
        const updates = updatesResponse.data || []
        totalActual = updates.reduce((sum, update) => sum + (parseFloat(update.actualQty) || 0), 0)
      } catch (err) {
        // Task updates might not exist yet
        console.warn('Could not fetch task updates:', err)
      }
      
      // Fetch task details for unit
      const taskResponse = await api.get(`/api/tasks/${taskId}`)
      const task = taskResponse.data
      
      setSummary({
        totalPlanned,
        totalActual,
        variance: totalPlanned - totalActual,
        activeVersion: activeVersion ? {
          versionNo: activeVersion.versionNo,
          versionDate: activeVersion.versionDate
        } : null,
        unit: task?.unit || '',
        planLinesCount: planLines.length
      })
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch plan summary')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="bg-white shadow rounded-lg p-4">
        <div className="text-center text-gray-500">Loading summary...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <p className="text-red-700 text-sm">{error}</p>
      </div>
    )
  }

  if (!summary) {
    return null
  }

  const varianceColor = summary.variance >= 0 ? 'text-green-600' : 'text-red-600'
  const varianceBg = summary.variance >= 0 ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'

  return (
    <div className="bg-white shadow rounded-lg p-6">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-gray-900">Plan Summary</h3>
        {summary.activeVersion && (
          <span className="inline-flex px-3 py-1 text-xs font-semibold rounded-full bg-indigo-100 text-indigo-800">
            Active: Version {summary.activeVersion.versionNo}
          </span>
        )}
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* Total Planned */}
        <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
          <div className="text-sm font-medium text-blue-800 mb-1">Total Planned</div>
          <div className="text-2xl font-bold text-blue-900">
            {summary.totalPlanned.toFixed(2)} {summary.unit}
          </div>
          <div className="text-xs text-blue-600 mt-1">
            {summary.planLinesCount} plan line{summary.planLinesCount !== 1 ? 's' : ''}
          </div>
        </div>
        
        {/* Total Actual */}
        <div className="bg-purple-50 border border-purple-200 rounded-md p-4">
          <div className="text-sm font-medium text-purple-800 mb-1">Total Actual</div>
          <div className="text-2xl font-bold text-purple-900">
            {summary.totalActual.toFixed(2)} {summary.unit}
          </div>
          <div className="text-xs text-purple-600 mt-1">
            From task updates
          </div>
        </div>
        
        {/* Variance */}
        <div className={`${varianceBg} border rounded-md p-4`}>
          <div className={`text-sm font-medium ${varianceColor.replace('600', '800')} mb-1`}>
            Variance
          </div>
          <div className={`text-2xl font-bold ${varianceColor}`}>
            {summary.variance >= 0 ? '+' : ''}{summary.variance.toFixed(2)} {summary.unit}
          </div>
          <div className={`text-xs ${varianceColor.replace('600', '700')} mt-1`}>
            {summary.variance >= 0 ? 'Over' : 'Under'} planned
          </div>
        </div>
        
        {/* Completion % */}
        <div className="bg-gray-50 border border-gray-200 rounded-md p-4">
          <div className="text-sm font-medium text-gray-800 mb-1">Completion</div>
          <div className="text-2xl font-bold text-gray-900">
            {summary.totalPlanned > 0 
              ? ((summary.totalActual / summary.totalPlanned) * 100).toFixed(1)
              : '0.0'}%
          </div>
          <div className="text-xs text-gray-600 mt-1">
            Actual vs Planned
          </div>
        </div>
      </div>
      
      {summary.activeVersion && (
        <div className="mt-4 pt-4 border-t text-sm text-gray-600">
          <span className="font-medium">Active Version Date:</span>{' '}
          {new Date(summary.activeVersion.versionDate).toLocaleDateString()}
        </div>
      )}
    </div>
  )
}

export default TaskPlanSummary

