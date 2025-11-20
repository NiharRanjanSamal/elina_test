import { useState, useEffect } from 'react'
import api from '../../services/api'
import { useParams, useNavigate } from 'react-router-dom'

const Tasks = () => {
  const { wbsId } = useParams()
  const navigate = useNavigate()
  const [wbs, setWbs] = useState(null)
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editingTask, setEditingTask] = useState(null)
  const [formData, setFormData] = useState({
    taskCode: '',
    taskName: '',
    description: '',
    startDate: '',
    endDate: '',
    plannedQty: '',
    unit: ''
  })
  const [showConfirmModal, setShowConfirmModal] = useState(false)
  const [confirmingTask, setConfirmingTask] = useState(null)
  const [confirmFormData, setConfirmFormData] = useState({
    confirmationDate: new Date().toISOString().split('T')[0],
    remarks: ''
  })

  useEffect(() => {
    if (wbsId) {
      fetchWbs()
      fetchTasks()
    }
  }, [wbsId])

  const fetchWbs = async () => {
    try {
      const response = await api.get(`/api/wbs/${wbsId}`)
      setWbs(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch WBS')
    }
  }

  const fetchTasks = async () => {
    try {
      setLoading(true)
      const response = await api.get(`/api/tasks/wbs/${wbsId}`)
      setTasks(response.data || [])
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch tasks')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    // Validation
    if (!formData.taskCode.trim()) {
      setError('Task code is required')
      return
    }
    if (!formData.taskName.trim()) {
      setError('Task name is required')
      return
    }
    if (formData.startDate && formData.endDate && formData.startDate > formData.endDate) {
      setError('Start date must be before end date')
      return
    }

    try {
      // TaskCreateDTO requires both projectId and wbsId
      if (!wbs?.projectId) {
        setError('Project ID is missing. Please refresh the page.')
        return
      }

      const payload = {
        ...formData,
        projectId: wbs.projectId,
        wbsId: parseInt(wbsId),
        plannedQty: formData.plannedQty ? parseFloat(formData.plannedQty) : null,
        startDate: formData.startDate || null,
        endDate: formData.endDate || null
      }

      if (editingTask) {
        await api.put(`/api/tasks/${editingTask.taskId}`, payload)
      } else {
        await api.post('/api/tasks', payload)
      }
      
      setShowForm(false)
      setEditingTask(null)
      setFormData({
        taskCode: '',
        taskName: '',
        description: '',
        startDate: '',
        endDate: '',
        plannedQty: '',
        unit: ''
      })
      fetchTasks()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save task')
    }
  }

  const handleEdit = (task) => {
    setEditingTask(task)
    setFormData({
      taskCode: task.taskCode || '',
      taskName: task.taskName || '',
      description: task.description || '',
      startDate: task.startDate ? task.startDate.split('T')[0] : '',
      endDate: task.endDate ? task.endDate.split('T')[0] : '',
      plannedQty: task.plannedQty?.toString() || '',
      unit: task.unit || ''
    })
    setShowForm(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this task? This will also delete associated task updates and confirmations.')) {
      return
    }

    try {
      await api.delete(`/api/tasks/${id}`)
      fetchTasks()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete task')
    }
  }

  const handleConfirm = (task) => {
    setConfirmingTask(task)
    setConfirmFormData({
      confirmationDate: new Date().toISOString().split('T')[0],
      remarks: ''
    })
    setShowConfirmModal(true)
  }

  const handleConfirmSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!confirmFormData.confirmationDate) {
      setError('Confirmation date is required')
      return
    }

    try {
      const payload = {
        entityType: 'TASK',
        entityId: confirmingTask.taskId,
        confirmationDate: confirmFormData.confirmationDate,
        remarks: confirmFormData.remarks || null
      }

      await api.post('/api/confirmations', payload)
      setShowConfirmModal(false)
      setConfirmingTask(null)
      setConfirmFormData({
        confirmationDate: new Date().toISOString().split('T')[0],
        remarks: ''
      })
      fetchTasks()
      fetchWbs() // Refresh WBS data to show updated confirmation status
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to confirm task')
    }
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

  if (loading && tasks.length === 0) {
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
              onClick={() => navigate(wbs?.projectId ? `/projects/${wbs.projectId}/wbs` : '/projects')}
              className="text-sm text-indigo-600 hover:text-indigo-900 mb-2"
            >
              ← Back to WBS
            </button>
            <h1 className="text-3xl font-bold text-gray-900">
              Tasks {wbs && `- ${wbs.wbsCode} - ${wbs.wbsName}`}
            </h1>
            <p className="mt-2 text-sm text-gray-600">
              Manage tasks for this WBS
            </p>
          </div>
          <button
            onClick={() => {
              setEditingTask(null)
              setFormData({
                taskCode: '',
                taskName: '',
                description: '',
                startDate: '',
                endDate: '',
                plannedQty: '',
                unit: ''
              })
              setShowForm(true)
            }}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            Add Task
          </button>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Table */}
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Code
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Start Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  End Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Planned Qty
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actual Qty
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {tasks.map((task) => (
                <tr key={task.taskId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {task.taskCode}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-900">
                    <div className="font-medium">{task.taskName}</div>
                    {task.description && (
                      <div className="text-xs text-gray-500 mt-1">{task.description}</div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {formatDate(task.startDate)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {formatDate(task.endDate)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {task.plannedQty || 0} {task.unit || ''}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {task.actualQty || 0} {task.unit || ''}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    {task.isConfirmed && (
                      <span className="mr-4 px-2 py-1 bg-green-100 text-green-700 rounded text-xs">Confirmed</span>
                    )}
                    <button
                      onClick={() => navigate(`/tasks/${task.taskId}/updates`)}
                      className="text-blue-600 hover:text-blue-900 mr-4"
                      title="View Updates"
                    >
                      Updates
                    </button>
                    <button
                      onClick={() => navigate(`/tasks/${task.taskId}/plans`)}
                      className="text-purple-600 hover:text-purple-900 mr-4"
                      title="Plan Versions"
                    >
                      Plans
                    </button>
                    {!task.isConfirmed && (
                      <button
                        onClick={() => handleConfirm(task)}
                        className="text-yellow-600 hover:text-yellow-900 mr-4"
                        title="Confirm Task"
                      >
                        Confirm
                      </button>
                    )}
                    <button
                      onClick={() => handleEdit(task)}
                      className="text-indigo-600 hover:text-indigo-900 mr-4"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(task.taskId)}
                      className="text-red-600 hover:text-red-900"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {tasks.length === 0 && (
            <div className="px-4 py-8 text-center text-gray-500">No tasks found</div>
          )}
        </div>

        {/* Form Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-900">
                  {editingTask ? 'Edit Task' : 'Create Task'}
                </h2>
                <button
                  onClick={() => {
                    setShowForm(false)
                    setEditingTask(null)
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

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Task Code <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.taskCode}
                    onChange={(e) => setFormData({ ...formData, taskCode: e.target.value.toUpperCase() })}
                    placeholder="e.g., TASK001"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Task Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.taskName}
                    onChange={(e) => setFormData({ ...formData, taskName: e.target.value })}
                    placeholder="Enter task name"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Description
                  </label>
                  <textarea
                    rows={3}
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Enter task description"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Start Date
                    </label>
                    <input
                      type="date"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.startDate}
                      onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      End Date
                    </label>
                    <input
                      type="date"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.endDate}
                      onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Planned Quantity
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.plannedQty}
                      onChange={(e) => setFormData({ ...formData, plannedQty: e.target.value })}
                      placeholder="0.00"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Unit
                    </label>
                    <input
                      type="text"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.unit}
                      onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                      placeholder="e.g., hours, kg, m"
                    />
                  </div>
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowForm(false)
                      setEditingTask(null)
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
                  >
                    {editingTask ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Confirmation Modal */}
        {showConfirmModal && confirmingTask && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-900">
                  Confirm Task
                </h2>
                <button
                  onClick={() => {
                    setShowConfirmModal(false)
                    setConfirmingTask(null)
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

              <div className="mb-4">
                <p className="text-sm text-gray-600">
                  <span className="font-medium">Task:</span> {confirmingTask.taskCode} - {confirmingTask.taskName}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  Note: Business rules may prevent confirmation if the task is already confirmed.
                </p>
              </div>

              <form onSubmit={handleConfirmSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Confirmation Date <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    required
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={confirmFormData.confirmationDate}
                    onChange={(e) => setConfirmFormData({ ...confirmFormData, confirmationDate: e.target.value })}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Remarks
                  </label>
                  <textarea
                    rows={3}
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={confirmFormData.remarks}
                    onChange={(e) => setConfirmFormData({ ...confirmFormData, remarks: e.target.value })}
                    placeholder="Enter remarks (optional)"
                  />
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowConfirmModal(false)
                      setConfirmingTask(null)
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-yellow-600 text-white rounded-md hover:bg-yellow-700"
                  >
                    Confirm
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Tasks

