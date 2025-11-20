import { useState, useEffect } from 'react'
import api from '../../services/api'
import { useParams, useNavigate } from 'react-router-dom'

const PlanVersions = () => {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const [task, setTask] = useState(null)
  const [versions, setVersions] = useState([])
  const [selectedVersion, setSelectedVersion] = useState(null)
  const [planLines, setPlanLines] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newPlanLines, setNewPlanLines] = useState([{ date: '', planQty: '' }])

  useEffect(() => {
    if (taskId) {
      fetchTask()
      fetchVersions()
    }
  }, [taskId])

  const fetchTask = async () => {
    try {
      const response = await api.get(`/api/tasks/${taskId}`)
      setTask(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch task')
    }
  }

  const fetchVersions = async () => {
    try {
      setLoading(true)
      const response = await api.get(`/api/plans/task/${taskId}`)
      setVersions(response.data || [])
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch plan versions')
    } finally {
      setLoading(false)
    }
  }

  const fetchPlanLines = async (versionId) => {
    try {
      const response = await api.get(`/api/plans/${versionId}/lines`)
      setPlanLines(response.data || [])
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch plan lines')
    }
  }

  const handleSelectVersion = (version) => {
    setSelectedVersion(version)
    fetchPlanLines(version.versionId)
  }

  const handleCreateVersion = async () => {
    setError('')

    // Validation
    const validLines = newPlanLines.filter(line => line.date && line.planQty)
    if (validLines.length === 0) {
      setError('At least one plan line with date and quantity is required')
      return
    }

    try {
      // PlanVersionCreateDTO requires versionDate and lines with lineNumber and plannedDate
      const today = new Date().toISOString().split('T')[0]
      const payload = {
        taskId: parseInt(taskId),
        versionDate: today, // Required by backend
        lines: validLines.map((line, index) => ({
          lineNumber: index + 1, // Required by PlanLineCreateDTO
          plannedDate: line.date, // Required by PlanLineCreateDTO (not "date")
          plannedQty: parseFloat(line.planQty) // Required by PlanLineCreateDTO
        }))
      }

      await api.post('/api/plans', payload)
      setShowCreateModal(false)
      setNewPlanLines([{ date: '', planQty: '' }])
      fetchVersions()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create plan version')
    }
  }

  const handleRevertToVersion = async (versionId) => {
    if (!window.confirm('Are you sure you want to revert to this plan version? This will update the task plan quantities.')) {
      return
    }

    try {
      await api.put(`/api/plans/${versionId}/revert`)
      setError('')
      fetchTask()
      fetchVersions()
      if (selectedVersion && selectedVersion.versionId === versionId) {
        fetchPlanLines(versionId)
      }
      alert('Plan version reverted successfully')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to revert plan version')
    }
  }

  const addPlanLine = () => {
    setNewPlanLines([...newPlanLines, { date: '', planQty: '' }])
  }

  const removePlanLine = (index) => {
    setNewPlanLines(newPlanLines.filter((_, i) => i !== index))
  }

  const updatePlanLine = (index, field, value) => {
    const updated = [...newPlanLines]
    updated[index] = { ...updated[index], [field]: value }
    setNewPlanLines(updated)
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

  const formatDateTime = (dateString) => {
    if (!dateString) return '-'
    try {
      const date = new Date(dateString)
      return date.toLocaleString()
    } catch {
      return dateString
    }
  }

  if (loading && versions.length === 0) {
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
        <div className="flex justify-between items-center mb-6">
          <div>
            <button
              onClick={() => navigate(task?.wbsId ? `/wbs/${task.wbsId}/tasks` : '/projects')}
              className="text-sm text-indigo-600 hover:text-indigo-900 mb-2"
            >
              ← Back to Tasks
            </button>
            <h1 className="text-3xl font-bold text-gray-900">
              Plan Versions {task && `- ${task.taskCode} - ${task.taskName}`}
            </h1>
            <p className="mt-2 text-sm text-gray-600">
              Manage plan versions and revert to previous versions
            </p>
          </div>
          <button
            onClick={() => {
              setNewPlanLines([{ date: '', planQty: '' }])
              setShowCreateModal(true)
            }}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            Create Plan Version
          </button>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Task Summary */}
        {task && (
          <div className="mb-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h3 className="text-lg font-semibold text-blue-900 mb-2">Task Summary</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="font-medium text-blue-800">Code:</span> {task.taskCode}
              </div>
              <div>
                <span className="font-medium text-blue-800">Name:</span> {task.taskName}
              </div>
              <div>
                <span className="font-medium text-blue-800">Planned Qty:</span> {task.plannedQty || 0} {task.unit || ''}
              </div>
              <div>
                <span className="font-medium text-blue-800">Current Plan:</span> Version {versions.find(v => v.isActive)?.versionNumber || 'N/A'}
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Plan Versions List */}
          <div className="bg-white shadow overflow-hidden sm:rounded-md">
            <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">Plan Versions</h2>
            </div>
            <div className="divide-y divide-gray-200">
              {versions.map((version) => (
                <div
                  key={version.versionId}
                  className={`p-4 cursor-pointer hover:bg-gray-50 ${
                    selectedVersion?.versionId === version.versionId ? 'bg-blue-50 border-l-4 border-blue-500' : ''
                  }`}
                  onClick={() => handleSelectVersion(version)}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-medium text-gray-900">
                        Version {version.versionNumber}
                        {version.isActive && (
                          <span className="ml-2 inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                            Active
                          </span>
                        )}
                      </div>
                      <div className="text-sm text-gray-500 mt-1">
                        Created: {formatDateTime(version.createdOn)}
                      </div>
                      <div className="text-sm text-gray-500">
                        By: User ID {version.createdBy}
                      </div>
                    </div>
                    {!version.isActive && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          handleRevertToVersion(version.versionId)
                        }}
                        className="px-3 py-1 text-xs bg-purple-100 text-purple-700 rounded hover:bg-purple-200"
                      >
                        Revert
                      </button>
                    )}
                  </div>
                </div>
              ))}
              {versions.length === 0 && (
                <div className="px-4 py-8 text-center text-gray-500">No plan versions found</div>
              )}
            </div>
          </div>

          {/* Plan Lines for Selected Version */}
          <div className="bg-white shadow overflow-hidden sm:rounded-md">
            <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">
                Plan Lines {selectedVersion && `- Version ${selectedVersion.versionNumber}`}
              </h2>
            </div>
            {selectedVersion ? (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Date
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Plan Quantity
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {planLines.map((line) => (
                      <tr key={line.lineId} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {formatDate(line.plannedDate)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {line.planQty || 0} {task?.unit || ''}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {planLines.length === 0 && (
                  <div className="px-4 py-8 text-center text-gray-500">No plan lines found for this version</div>
                )}
              </div>
            ) : (
              <div className="px-4 py-8 text-center text-gray-500">Select a plan version to view its lines</div>
            )}
          </div>
        </div>

        {/* Create Plan Version Modal */}
        {showCreateModal && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-900">Create Plan Version</h2>
                <button
                  onClick={() => {
                    setShowCreateModal(false)
                    setNewPlanLines([{ date: '', planQty: '' }])
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {error && (
                <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
                  {error}
                </div>
              )}

              <div className="space-y-4">
                <div className="max-h-96 overflow-y-auto">
                  {newPlanLines.map((line, index) => (
                    <div key={index} className="flex gap-4 mb-4 items-end">
                      <div className="flex-1">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Date
                        </label>
                        <input
                          type="date"
                          className="w-full border border-gray-300 rounded-md px-3 py-2"
                          value={line.date}
                          onChange={(e) => updatePlanLine(index, 'date', e.target.value)}
                        />
                      </div>
                      <div className="flex-1">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Plan Quantity
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          className="w-full border border-gray-300 rounded-md px-3 py-2"
                          value={line.planQty}
                          onChange={(e) => updatePlanLine(index, 'planQty', e.target.value)}
                          placeholder="0.00"
                        />
                        {task && (
                          <p className="mt-1 text-xs text-gray-500">Unit: {task.unit || 'N/A'}</p>
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={() => removePlanLine(index)}
                        className="px-3 py-2 bg-red-100 text-red-700 rounded-md hover:bg-red-200"
                      >
                        Remove
                      </button>
                    </div>
                  ))}
                </div>

                <button
                  type="button"
                  onClick={addPlanLine}
                  className="w-full px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                >
                  + Add Plan Line
                </button>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowCreateModal(false)
                      setNewPlanLines([{ date: '', planQty: '' }])
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleCreateVersion}
                    className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
                  >
                    Create Version
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default PlanVersions

