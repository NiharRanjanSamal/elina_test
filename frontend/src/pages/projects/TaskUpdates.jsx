import { useState, useEffect } from 'react'
import api from '../../services/api'
import { useParams, useNavigate } from 'react-router-dom'
import DayWiseGrid from '../../components/DayWiseGrid'
import TaskUpdateListView from '../../components/TaskUpdateListView'
import RuleViolationModal from '../../components/RuleViolationModal'

/**
 * TaskUpdatePage Component
 * 
 * Main page for day-wise task updates.
 * Features:
 * - Task summary header
 * - Plan version information
 * - DayWiseGrid for entering updates
 * - Save button integration
 * - Rule violation handling
 */
const TaskUpdates = () => {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const [task, setTask] = useState(null)
  const [planVersion, setPlanVersion] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [ruleViolation, setRuleViolation] = useState(null)
  const [viewMode, setViewMode] = useState('grid') // 'grid', 'list', or 'summary'
  const [summaryData, setSummaryData] = useState([])
  const [summaryDateRange, setSummaryDateRange] = useState({
    from: task?.startDate || new Date().toISOString().split('T')[0],
    to: task?.endDate || new Date().toISOString().split('T')[0]
  })

  useEffect(() => {
    if (taskId) {
      fetchTask()
      fetchPlanVersion()
    }
  }, [taskId])

  useEffect(() => {
    if (task && !summaryDateRange.from) {
      setSummaryDateRange({
        from: task.startDate ? new Date(task.startDate).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
        to: task.endDate ? new Date(task.endDate).toISOString().split('T')[0] : new Date().toISOString().split('T')[0]
      })
    }
  }, [task])

  const fetchTask = async () => {
    try {
      const response = await api.get(`/api/tasks/${taskId}`)
      setTask(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch task')
    }
  }

  const fetchPlanVersion = async () => {
    try {
      // Fetch plan versions for this task
      const response = await api.get(`/api/plan-versions/task/${taskId}`)
      const versions = response.data || []
      // Find active plan version
      const active = versions.find(v => v.isActive && v.activateFlag)
      setPlanVersion(active || null)
    } catch (err) {
      // Plan version might not exist, that's okay
      console.log('No plan version found for task')
    }
  }

  const handleSaveSuccess = () => {
    setError('')
    // Optionally show success message
    alert('Day-wise updates saved successfully!')
    // Refresh task to get updated actual_qty
    fetchTask()
  }

  const handleError = (errorMessage) => {
    setError(errorMessage)
  }

  const handleRuleViolation = (violation) => {
    setRuleViolation(violation)
  }

  const fetchSummary = async () => {
    if (!taskId || !summaryDateRange.from || !summaryDateRange.to) return

    try {
      const response = await api.get(`/api/task-updates/task/${taskId}/summary`, {
        params: {
          from: summaryDateRange.from,
          to: summaryDateRange.to
        }
      })
      setSummaryData(response.data || [])
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch summary')
    }
  }

  useEffect(() => {
    if (viewMode === 'summary' && taskId) {
      fetchSummary()
    }
  }, [viewMode, summaryDateRange, taskId])

  if (loading && !task) {
    return (
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="text-center">Loading...</div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div className="px-4 py-6 sm:px-0">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <button
              onClick={() => navigate(task?.wbsId ? `/wbs/${task.wbsId}/tasks` : '/projects')}
              className="text-sm text-indigo-600 hover:text-indigo-900 mb-2"
            >
              ← Back to Tasks
            </button>
            <h1 className="text-3xl font-bold text-gray-900">
              Day-Wise Updates {task && `- ${task.taskCode} - ${task.taskName}`}
            </h1>
            <p className="mt-2 text-sm text-gray-600">
              Enter daily progress updates (plan vs actual quantities)
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setViewMode('grid')}
              className={`px-4 py-2 rounded-md ${
                viewMode === 'grid' 
                  ? 'bg-indigo-600 text-white' 
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              Grid View
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`px-4 py-2 rounded-md ${
                viewMode === 'list' 
                  ? 'bg-indigo-600 text-white' 
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              List View
            </button>
            <button
              onClick={() => setViewMode('summary')}
              className={`px-4 py-2 rounded-md ${
                viewMode === 'summary' 
                  ? 'bg-indigo-600 text-white' 
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              Summary
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Task Summary */}
        {task && (
          <div className="mb-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h3 className="text-lg font-semibold text-blue-900 mb-3">Task Summary</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="font-medium text-blue-800">Code:</span> {task.taskCode}
              </div>
              <div>
                <span className="font-medium text-blue-800">Name:</span> {task.taskName}
              </div>
              <div>
                <span className="font-medium text-blue-800">Start Date:</span>{' '}
                {task.startDate ? new Date(task.startDate).toLocaleDateString() : 'N/A'}
              </div>
              <div>
                <span className="font-medium text-blue-800">End Date:</span>{' '}
                {task.endDate ? new Date(task.endDate).toLocaleDateString() : 'N/A'}
              </div>
              <div>
                <span className="font-medium text-blue-800">Planned Qty:</span> {task.plannedQty || 0} {task.unit || ''}
              </div>
              <div>
                <span className="font-medium text-blue-800">Actual Qty:</span> {task.actualQty || 0} {task.unit || ''}
              </div>
              <div>
                <span className="font-medium text-blue-800">Status:</span> {task.status || 'N/A'}
              </div>
              <div>
                <span className="font-medium text-blue-800">Unit:</span> {task.unit || 'N/A'}
              </div>
            </div>
          </div>
        )}

        {/* Plan Version Info */}
        {planVersion && (
          <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4">
            <h3 className="text-lg font-semibold text-green-900 mb-2">Active Plan Version</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="font-medium text-green-800">Version:</span> {planVersion.versionNo}
              </div>
              <div>
                <span className="font-medium text-green-800">Version Date:</span>{' '}
                {planVersion.versionDate ? new Date(planVersion.versionDate).toLocaleDateString() : 'N/A'}
              </div>
              <div>
                <span className="font-medium text-green-800">Description:</span> {planVersion.description || 'N/A'}
              </div>
              <div>
                <span className="font-medium text-green-800">Status:</span>{' '}
                {planVersion.isActive ? 'Active' : 'Inactive'}
              </div>
            </div>
            <p className="mt-2 text-xs text-green-700">
              Planned quantities shown in the grid are from this active plan version.
            </p>
          </div>
        )}

        {!planVersion && (
          <div className="mb-6 bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <h3 className="text-lg font-semibold text-yellow-900 mb-2">No Active Plan Version</h3>
            <p className="text-sm text-yellow-700">
              This task does not have an active plan version. Planned quantities will be taken from the task's planned_qty field.
            </p>
          </div>
        )}

        {/* Day-Wise Grid */}
        {viewMode === 'grid' && (
          <DayWiseGrid
            taskId={taskId}
            onSave={handleSaveSuccess}
            onError={handleError}
          />
        )}

        {/* List View */}
        {viewMode === 'list' && (
          <TaskUpdateListView
            taskId={taskId}
            onSave={handleSaveSuccess}
            onError={handleError}
          />
        )}

        {/* Summary View */}
        {viewMode === 'summary' && (
          <div className="bg-white shadow overflow-hidden sm:rounded-md">
            <div className="px-4 py-5 sm:p-6">
              <div className="mb-4">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Daily Summary Report</h3>
                <div className="flex items-center space-x-4 mb-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">From Date</label>
                    <input
                      type="date"
                      className="border border-gray-300 rounded-md px-3 py-2"
                      value={summaryDateRange.from}
                      onChange={(e) => setSummaryDateRange({ ...summaryDateRange, from: e.target.value })}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">To Date</label>
                    <input
                      type="date"
                      className="border border-gray-300 rounded-md px-3 py-2"
                      value={summaryDateRange.to}
                      onChange={(e) => setSummaryDateRange({ ...summaryDateRange, to: e.target.value })}
                    />
                  </div>
                  <div className="flex items-end">
                    <button
                      onClick={fetchSummary}
                      className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
                    >
                      Refresh
                    </button>
                  </div>
                </div>
              </div>

              {summaryData.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Planned Qty</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actual Qty</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Variance</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {summaryData.map((item, idx) => {
                        const variance = parseFloat(item.variance || 0)
                        return (
                          <tr key={idx} className="hover:bg-gray-50">
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                              {new Date(item.date).toLocaleDateString()}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                              {parseFloat(item.planQty || 0).toFixed(2)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                              {parseFloat(item.actualQty || 0).toFixed(2)}
                            </td>
                            <td className={`px-6 py-4 whitespace-nowrap text-sm font-medium ${
                              variance > 0 ? 'text-red-600' : variance < 0 ? 'text-green-600' : 'text-gray-500'
                            }`}>
                              {variance > 0 ? '+' : ''}{variance.toFixed(2)}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="text-gray-500">No summary data available for the selected date range.</p>
              )}
            </div>
          </div>
        )}

        {/* Rule Violation Modal */}
        {ruleViolation && (
          <RuleViolationModal
            ruleNumber={ruleViolation.ruleNumber}
            message={ruleViolation.message}
            hint={ruleViolation.hint}
            onClose={() => setRuleViolation(null)}
          />
        )}
      </div>
    </div>
  )
}

export default TaskUpdates
