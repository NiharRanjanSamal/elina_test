import { useState, useEffect } from 'react'
import api from '../../services/api'
import { useParams, useNavigate } from 'react-router-dom'
import PlanEditor from '../../components/PlanEditor'
import PlanVersionComparison from '../../components/PlanVersionComparison'
import TaskPlanSummary from '../../components/TaskPlanSummary'

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
  const [showComparison, setShowComparison] = useState(false)
  const [comparisonVersion1, setComparisonVersion1] = useState(null)
  const [comparisonVersion2, setComparisonVersion2] = useState(null)

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
    fetchPlanLines(version.planVersionId)
  }

  const handleCreateSuccess = () => {
    setShowCreateModal(false)
    fetchVersions()
    if (selectedVersion) {
      fetchPlanLines(selectedVersion.planVersionId)
    }
  }

  const handleActivateVersion = async (planVersionId) => {
    if (!window.confirm('Are you sure you want to activate this plan version? This will deactivate all other versions for this task.')) {
      return
    }

    try {
      await api.put(`/api/plans/${planVersionId}/activate`)
      setError('')
      fetchTask()
      fetchVersions()
      if (selectedVersion && selectedVersion.planVersionId === planVersionId) {
        fetchPlanLines(planVersionId)
      }
      alert('Plan version activated successfully')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to activate plan version')
    }
  }

  const handleRevertToVersion = async (planVersionId) => {
    if (!window.confirm('Are you sure you want to revert to this plan version? This will update the task plan quantities.')) {
      return
    }

    try {
      await api.put(`/api/plans/${planVersionId}/revert`)
      setError('')
      fetchTask()
      fetchVersions()
      if (selectedVersion && selectedVersion.planVersionId === planVersionId) {
        fetchPlanLines(planVersionId)
      }
      alert('Plan version reverted successfully')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to revert plan version')
    }
  }

  const handleCompareVersions = (version1, version2) => {
    setComparisonVersion1(version1)
    setComparisonVersion2(version2)
    setShowComparison(true)
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
            onClick={() => setShowCreateModal(true)}
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
          <div className="mb-6">
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
              <h3 className="text-lg font-semibold text-blue-900 mb-2">Task Information</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                <div>
                  <span className="font-medium text-blue-800">Code:</span> {task.taskCode}
                </div>
                <div>
                  <span className="font-medium text-blue-800">Name:</span> {task.taskName}
                </div>
                <div>
                  <span className="font-medium text-blue-800">Date Range:</span> {task.startDate ? new Date(task.startDate).toLocaleDateString() : '-'} to {task.endDate ? new Date(task.endDate).toLocaleDateString() : '-'}
                </div>
                <div>
                  <span className="font-medium text-blue-800">Status:</span> {task.status || 'N/A'}
                </div>
              </div>
            </div>
            <TaskPlanSummary taskId={taskId} />
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
                  key={version.planVersionId}
                  className={`p-4 cursor-pointer hover:bg-gray-50 ${
                    selectedVersion?.planVersionId === version.planVersionId ? 'bg-blue-50 border-l-4 border-blue-500' : ''
                  }`}
                  onClick={() => handleSelectVersion(version)}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-medium text-gray-900">
                        Version {version.versionNo}
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
                    <div className="flex gap-2">
                      {!version.isActive && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            handleRevertToVersion(version.planVersionId)
                          }}
                          className="px-3 py-1 text-xs bg-purple-100 text-purple-700 rounded hover:bg-purple-200"
                        >
                          Revert
                        </button>
                      )}
                      {versions.length > 1 && (
                        <>
                          {versions.filter(v => v.planVersionId !== version.planVersionId).map((otherVersion) => (
                            <button
                              key={otherVersion.planVersionId}
                              onClick={(e) => {
                                e.stopPropagation()
                                handleCompareVersions(version, otherVersion)
                              }}
                              className="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
                              title={`Compare with Version ${otherVersion.versionNo}`}
                            >
                              vs V{otherVersion.versionNo}
                            </button>
                          ))}
                        </>
                      )}
                      {!version.isActive && (
                        <>
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              handleActivateVersion(version.planVersionId)
                            }}
                            className="px-3 py-1 text-xs bg-indigo-100 text-indigo-700 rounded hover:bg-indigo-200"
                          >
                            Activate
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              handleRevertToVersion(version.planVersionId)
                            }}
                            className="px-3 py-1 text-xs bg-purple-100 text-purple-700 rounded hover:bg-purple-200"
                          >
                            Revert
                          </button>
                        </>
                      )}
                    </div>
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
                Plan Lines {selectedVersion && `- Version ${selectedVersion.versionNo}`}
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
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Description
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {planLines.map((line) => (
                      <tr key={line.planLineId} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {formatDate(line.workDate)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {line.plannedQty || 0} {task?.unit || ''}
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-500">
                          {line.description || '-'}
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
        {showCreateModal && task && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-10 mx-auto p-6 border w-11/12 md:w-4/5 lg:w-3/4 shadow-lg rounded-md bg-white max-h-[90vh] overflow-y-auto">
              <div className="flex justify-between items-center mb-4 border-b pb-4">
                <h2 className="text-2xl font-bold text-gray-900">Create Plan Version</h2>
                <button
                  onClick={() => setShowCreateModal(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <PlanEditor
                task={task}
                onSuccess={handleCreateSuccess}
                onCancel={() => setShowCreateModal(false)}
              />
            </div>
          </div>
        )}

        {/* Version Comparison Modal */}
        {showComparison && comparisonVersion1 && comparisonVersion2 && (
          <PlanVersionComparison
            versionId1={comparisonVersion1.planVersionId}
            versionId2={comparisonVersion2.planVersionId}
            onClose={() => {
              setShowComparison(false)
              setComparisonVersion1(null)
              setComparisonVersion2(null)
            }}
          />
        )}
      </div>
    </div>
  )
}

export default PlanVersions

