import { useState, useEffect } from 'react'
import api from '../../services/api'
import { useParams, useNavigate } from 'react-router-dom'

const TaskUpdates = () => {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const [task, setTask] = useState(null)
  const [updates, setUpdates] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editingUpdate, setEditingUpdate] = useState(null)
  const [formData, setFormData] = useState({
    updateDate: '',
    planQty: '',
    actualQty: '',
    remarks: ''
  })

  useEffect(() => {
    if (taskId) {
      fetchTask()
      fetchUpdates()
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

  const fetchUpdates = async () => {
    try {
      setLoading(true)
      const response = await api.get(`/api/task-updates/task/${taskId}`)
      setUpdates(response.data || [])
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch task updates')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    // Validation
    if (!formData.updateDate) {
      setError('Update date is required')
      return
    }
    // Backend requires actualQty as @NotNull
    if (!formData.actualQty || formData.actualQty.trim() === '') {
      setError('Actual quantity is required')
      return
    }
    if (formData.planQty && parseFloat(formData.planQty) < 0) {
      setError('Planned quantity cannot be negative')
      return
    }
    if (parseFloat(formData.actualQty) < 0) {
      setError('Actual quantity cannot be negative')
      return
    }

    try {
      const payload = {
        taskId: parseInt(taskId),
        updateDate: formData.updateDate,
        planQty: formData.planQty ? parseFloat(formData.planQty) : null,
        actualQty: parseFloat(formData.actualQty), // Required by backend @NotNull
        remarks: formData.remarks || null
      }

      // Task updates use POST for both create and update (upsert behavior)
      await api.post('/api/task-updates', payload)
      
      setShowForm(false)
      setEditingUpdate(null)
      setFormData({
        updateDate: '',
        planQty: '',
        actualQty: '',
        remarks: ''
      })
      fetchUpdates()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save task update')
    }
  }

  const handleEdit = (update) => {
    setEditingUpdate(update)
    setFormData({
      updateDate: update.updateDate ? update.updateDate.split('T')[0] : '',
      planQty: update.planQty?.toString() || '',
      actualQty: update.actualQty?.toString() || '',
      remarks: update.remarks || ''
    })
    setShowForm(true)
  }

  // Note: Delete functionality not available in backend API
  const handleDelete = async (id) => {
    alert('Delete functionality is not available. Please contact the administrator.')
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

  if (loading && updates.length === 0) {
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
              Task Updates {task && `- ${task.taskCode} - ${task.taskName}`}
            </h1>
            <p className="mt-2 text-sm text-gray-600">
              Manage day-wise task updates (plan vs actual quantities)
            </p>
          </div>
          <button
            onClick={() => {
              setEditingUpdate(null)
              setFormData({
                updateDate: new Date().toISOString().split('T')[0],
                planQty: '',
                actualQty: '',
                remarks: ''
              })
              setShowForm(true)
            }}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            Add Update
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
                <span className="font-medium text-blue-800">Actual Qty:</span> {task.actualQty || 0} {task.unit || ''}
              </div>
            </div>
          </div>
        )}

        {/* Table */}
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Update Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Planned Qty
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actual Qty
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Variance
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Remarks
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {updates.map((update) => {
                const variance = (update.actualQty || 0) - (update.planQty || 0)
                const unit = task?.unit || ''
                return (
                  <tr key={update.updateId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {formatDate(update.updateDate)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {update.planQty || 0} {unit}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {update.actualQty || 0} {unit}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`font-medium ${
                        variance > 0 ? 'text-red-600' :
                        variance < 0 ? 'text-green-600' :
                        'text-gray-500'
                      }`}>
                        {variance > 0 ? '+' : ''}{variance.toFixed(2)} {unit}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {update.remarks || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <button
                        onClick={() => handleEdit(update)}
                        className="text-indigo-600 hover:text-indigo-900"
                      >
                        Edit
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          {updates.length === 0 && (
            <div className="px-4 py-8 text-center text-gray-500">No task updates found</div>
          )}
        </div>

        {/* Form Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-900">
                  {editingUpdate ? 'Edit Task Update' : 'Create Task Update'}
                </h2>
                <button
                  onClick={() => {
                    setShowForm(false)
                    setEditingUpdate(null)
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
                    Update Date <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    required
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.updateDate}
                    onChange={(e) => setFormData({ ...formData, updateDate: e.target.value })}
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    Note: Business rules may restrict updates for dates older than allowed backdate period.
                  </p>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Planned Quantity
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.planQty}
                      onChange={(e) => setFormData({ ...formData, planQty: e.target.value })}
                      placeholder="0.00"
                    />
                    {task && (
                      <p className="mt-1 text-xs text-gray-500">
                        Unit: {task.unit || 'N/A'}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Actual Quantity
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.actualQty}
                      onChange={(e) => setFormData({ ...formData, actualQty: e.target.value })}
                      placeholder="0.00"
                    />
                    {task && (
                      <p className="mt-1 text-xs text-gray-500">
                        Unit: {task.unit || 'N/A'}
                      </p>
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Remarks
                  </label>
                  <textarea
                    rows={3}
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.remarks}
                    onChange={(e) => setFormData({ ...formData, remarks: e.target.value })}
                    placeholder="Enter remarks or notes"
                  />
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowForm(false)
                      setEditingUpdate(null)
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
                  >
                    {editingUpdate ? 'Update' : 'Create'}
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

export default TaskUpdates

