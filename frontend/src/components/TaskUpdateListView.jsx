import { useState, useEffect } from 'react'
import api from '../services/api'
import RuleViolationModal from './RuleViolationModal'

/**
 * TaskUpdateListView Component
 * 
 * Simple list view for task updates (alternative to grid view).
 * Shows updates in a table format with edit/delete actions.
 */
const TaskUpdateListView = ({ taskId, onSave, onError }) => {
  const [updates, setUpdates] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [ruleViolation, setRuleViolation] = useState(null)
  const [editingId, setEditingId] = useState(null)
  const [editForm, setEditForm] = useState({
    actualQty: '',
    remarks: ''
  })

  useEffect(() => {
    if (taskId) {
      fetchUpdates()
    }
  }, [taskId])

  const fetchUpdates = async () => {
    try {
      setLoading(true)
      const response = await api.get(`/api/task-updates/task/${taskId}/list`)
      setUpdates(response.data || [])
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch task updates')
    } finally {
      setLoading(false)
    }
  }

  const handleEdit = (update) => {
    setEditingId(update.updateId)
    setEditForm({
      actualQty: update.actualQty?.toString() || '',
      remarks: update.remarks || ''
    })
  }

  const handleSaveEdit = async () => {
    if (!editingId) return

    try {
      const payload = {
        taskId: parseInt(taskId),
        updateDate: updates.find(u => u.updateId === editingId)?.updateDate,
        actualQty: parseFloat(editForm.actualQty),
        remarks: editForm.remarks || null
      }

      await api.post('/api/task-updates', payload)
      
      setEditingId(null)
      setEditForm({ actualQty: '', remarks: '' })
      fetchUpdates()
      
      if (onSave) {
        onSave()
      }
    } catch (err) {
      const errorData = err.response?.data
      if (errorData?.ruleNumber) {
        setRuleViolation({
          ruleNumber: errorData.ruleNumber,
          message: errorData.message || 'Business rule violation',
          hint: errorData.hint || 'Please check your input and try again.'
        })
      } else {
        setError(errorData?.message || 'Failed to save update')
      }
    }
  }

  const handleDelete = async (updateId) => {
    if (!window.confirm('Are you sure you want to delete this update?')) {
      return
    }

    try {
      await api.delete(`/api/task-updates/${updateId}`)
      fetchUpdates()
      
      if (onSave) {
        onSave()
      }
    } catch (err) {
      const errorData = err.response?.data
      if (errorData?.ruleNumber) {
        setRuleViolation({
          ruleNumber: errorData.ruleNumber,
          message: errorData.message || 'Cannot delete update',
          hint: errorData.hint || 'This update may be locked or restricted by business rules.'
        })
      } else {
        setError(errorData?.message || 'Failed to delete update')
      }
    }
  }

  const handleCancelEdit = () => {
    setEditingId(null)
    setEditForm({ actualQty: '', remarks: '' })
  }

  if (loading) {
    return (
      <div className="text-center py-8">
        <div className="text-gray-500">Loading task updates...</div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
          {error}
        </div>
      )}

      <div className="bg-white shadow overflow-hidden sm:rounded-md">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Planned Qty</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actual Qty</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Variance</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Remarks</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {updates.map((update) => {
              const variance = (update.actualQty || 0) - (update.plannedQty || 0)
              const isEditing = editingId === update.updateId

              return (
                <tr key={update.updateId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {new Date(update.updateDate).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {parseFloat(update.plannedQty || 0).toFixed(2)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {isEditing ? (
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        className="w-24 border border-gray-300 rounded px-2 py-1"
                        value={editForm.actualQty}
                        onChange={(e) => setEditForm({ ...editForm, actualQty: e.target.value })}
                      />
                    ) : (
                      <span className="text-gray-500">{parseFloat(update.actualQty || 0).toFixed(2)}</span>
                    )}
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm font-medium ${
                    variance > 0 ? 'text-red-600' : variance < 0 ? 'text-green-600' : 'text-gray-500'
                  }`}>
                    {variance > 0 ? '+' : ''}{variance.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {isEditing ? (
                      <input
                        type="text"
                        className="w-full border border-gray-300 rounded px-2 py-1"
                        value={editForm.remarks}
                        onChange={(e) => setEditForm({ ...editForm, remarks: e.target.value })}
                        placeholder="Enter remarks"
                      />
                    ) : (
                      <span>{update.remarks || '-'}</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    {isEditing ? (
                      <div className="flex space-x-2">
                        <button
                          onClick={handleSaveEdit}
                          className="text-indigo-600 hover:text-indigo-900"
                        >
                          Save
                        </button>
                        <button
                          onClick={handleCancelEdit}
                          className="text-gray-600 hover:text-gray-900"
                        >
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleEdit(update)}
                          className="text-indigo-600 hover:text-indigo-900"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(update.updateId)}
                          className="text-red-600 hover:text-red-900"
                        >
                          Delete
                        </button>
                      </div>
                    )}
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

      {ruleViolation && (
        <RuleViolationModal
          ruleNumber={ruleViolation.ruleNumber}
          message={ruleViolation.message}
          hint={ruleViolation.hint}
          onClose={() => setRuleViolation(null)}
        />
      )}
    </div>
  )
}

export default TaskUpdateListView

