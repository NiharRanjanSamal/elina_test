import { useState, useEffect } from 'react'
import api from '../services/api'
import RuleViolationModal from './RuleViolationModal'

/**
 * DayWiseGrid Component
 * 
 * Excel-like grid for entering day-wise task updates.
 * Features:
 * - Date, Planned Qty, Actual Qty (editable), Variance columns
 * - Auto-calculates variance on change
 * - Color indicators: Green (actual <= plan), Red (actual > plan)
 * - Prevents submission if actual > plan
 * - Shows locked dates (non-editable)
 * - Bulk update mode support
 */
const DayWiseGrid = ({ taskId, onSave, onError }) => {
  const [updates, setUpdates] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [selectedRows, setSelectedRows] = useState(new Set())
  const [bulkMode, setBulkMode] = useState(false)
  const [bulkValue, setBulkValue] = useState('')
  const [ruleViolation, setRuleViolation] = useState(null)
  const [editingCell, setEditingCell] = useState(null)

  useEffect(() => {
    if (taskId) {
      fetchUpdates()
    }
  }, [taskId])

  const fetchUpdates = async () => {
    try {
      setLoading(true)
      const response = await api.get(`/api/task-updates/task/${taskId}`)
      setUpdates(response.data || [])
    } catch (err) {
      if (onError) {
        onError(err.response?.data?.message || 'Failed to fetch day-wise updates')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleCellChange = (index, field, value) => {
    const updated = [...updates]
    const row = updated[index]
    
    if (field === 'actualQty') {
      const numValue = parseFloat(value) || 0
      row.actualQty = numValue
      // Recalculate variance
      const planQty = parseFloat(row.planQty) || 0
      row.variance = numValue - planQty
    } else if (field === 'remarks') {
      row.remarks = value
    }
    
    updated[index] = row
    setUpdates(updated)
  }

  const handleBulkUpdate = () => {
    if (!bulkValue || isNaN(parseFloat(bulkValue))) {
      alert('Please enter a valid number for bulk update')
      return
    }

    const updated = [...updates]
    const value = parseFloat(bulkValue)
    
    selectedRows.forEach(index => {
      const row = updated[index]
      if (row.canEdit && !row.isLocked) {
        row.actualQty = value
        const planQty = parseFloat(row.planQty) || 0
        row.variance = value - planQty
      }
    })
    
    setUpdates(updated)
    setBulkMode(false)
    setBulkValue('')
    setSelectedRows(new Set())
  }

  const handleDelete = async (updateId, index) => {
    if (!window.confirm('Are you sure you want to delete this day-wise update? This action cannot be undone.')) {
      return
    }

    try {
      await api.delete(`/api/task-updates/${updateId}`)
      
      // Remove from local state
      const updated = updates.filter((_, i) => i !== index)
      setUpdates(updated)
      
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
        if (onError) {
          onError(errorData?.message || 'Failed to delete update')
        }
      }
    }
  }

  const handleSave = async () => {
    // Validate: Check if any actual > plan
    const violations = updates.filter(u => 
      u.canEdit && !u.isLocked && 
      u.actualQty > (parseFloat(u.planQty) || 0)
    )

    if (violations.length > 0) {
      setRuleViolation({
        ruleNumber: 401,
        message: `Actual quantity cannot exceed planned quantity. Found ${violations.length} violation(s).`,
        hint: 'Please adjust actual quantities to be less than or equal to planned quantities.'
      })
      return
    }

    try {
      setSaving(true)
      
      // Prepare bulk update payload
      const bulkUpdates = updates
        .filter(u => u.canEdit && !u.isLocked && (u.actualQty > 0 || u.updateId))
        .map(u => ({
          updateDate: u.updateDate,
          planQty: u.planQty,
          actualQty: u.actualQty,
          remarks: u.remarks || null
        }))

      if (bulkUpdates.length === 0) {
        alert('No updates to save')
        setSaving(false)
        return
      }

      const payload = {
        taskId: parseInt(taskId),
        updates: bulkUpdates
      }

      await api.post(`/api/task-updates/task/${taskId}`, payload)
      
      if (onSave) {
        onSave()
      }
      
      // Refresh data
      await fetchUpdates()
    } catch (err) {
      // Check if it's a business rule violation
      const errorData = err.response?.data
      if (errorData?.ruleNumber) {
        setRuleViolation({
          ruleNumber: errorData.ruleNumber,
          message: errorData.message || 'Business rule violation',
          hint: errorData.hint || 'Please check your input and try again.'
        })
      } else {
        if (onError) {
          onError(errorData?.message || 'Failed to save updates')
        }
      }
    } finally {
      setSaving(false)
    }
  }

  const toggleRowSelection = (index) => {
    const newSelected = new Set(selectedRows)
    if (newSelected.has(index)) {
      newSelected.delete(index)
    } else {
      newSelected.add(index)
    }
    setSelectedRows(newSelected)
  }

  const getVarianceColor = (variance) => {
    if (variance > 0) return 'text-red-600 font-bold'
    if (variance < 0) return 'text-green-600'
    return 'text-gray-500'
  }

  const getRowBgColor = (row) => {
    if (row.isLocked) return 'bg-gray-100'
    if (!row.canEdit) return 'bg-yellow-50'
    if (row.variance > 0) return 'bg-red-50'
    return 'bg-white'
  }

  if (loading) {
    return (
      <div className="text-center py-8">
        <div className="text-gray-500">Loading day-wise updates...</div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Bulk Update Mode */}
      {bulkMode && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-blue-900">Bulk Update Mode</h3>
              <p className="text-sm text-blue-700">
                {selectedRows.size} row(s) selected. Enter a value to apply to all selected rows.
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <input
                type="number"
                step="0.01"
                min="0"
                className="border border-blue-300 rounded px-3 py-2 w-32"
                placeholder="Enter value"
                value={bulkValue}
                onChange={(e) => setBulkValue(e.target.value)}
              />
              <button
                onClick={handleBulkUpdate}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
              >
                Apply
              </button>
              <button
                onClick={() => {
                  setBulkMode(false)
                  setBulkValue('')
                  setSelectedRows(new Set())
                }}
                className="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toolbar */}
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setBulkMode(!bulkMode)}
            className={`px-4 py-2 rounded ${
              bulkMode 
                ? 'bg-indigo-600 text-white' 
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            {bulkMode ? 'Exit Bulk Mode' : 'Bulk Update'}
          </button>
          {selectedRows.size > 0 && (
            <span className="text-sm text-gray-600">
              {selectedRows.size} row(s) selected
            </span>
          )}
        </div>
        <button
          onClick={handleSave}
          disabled={saving}
          className="px-6 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:bg-gray-400"
        >
          {saving ? 'Saving...' : 'Save All Changes'}
        </button>
      </div>

      {/* Grid Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200 border border-gray-300">
          <thead className="bg-gray-50">
            <tr>
              {bulkMode && (
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Select
                </th>
              )}
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Date
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Planned Qty
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Actual Qty
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Variance
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Remarks
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Status
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {updates.map((row, index) => (
              <tr 
                key={index} 
                className={`hover:bg-gray-50 ${getRowBgColor(row)}`}
              >
                {bulkMode && (
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selectedRows.has(index)}
                      onChange={() => toggleRowSelection(index)}
                      disabled={!row.canEdit || row.isLocked}
                      className="rounded"
                    />
                  </td>
                )}
                <td className="px-4 py-3 text-sm font-medium text-gray-900">
                  {new Date(row.updateDate).toLocaleDateString()}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">
                  {parseFloat(row.planQty || 0).toFixed(2)}
                </td>
                <td className="px-4 py-3">
                  {row.canEdit && !row.isLocked ? (
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      className={`w-24 border rounded px-2 py-1 ${
                        row.variance > 0 ? 'border-red-500 bg-red-50' : 'border-gray-300'
                      }`}
                      value={row.actualQty || ''}
                      onChange={(e) => handleCellChange(index, 'actualQty', e.target.value)}
                      onFocus={() => setEditingCell(`${index}-actualQty`)}
                      onBlur={() => setEditingCell(null)}
                    />
                  ) : (
                    <span className="text-sm text-gray-500">
                      {parseFloat(row.actualQty || 0).toFixed(2)}
                    </span>
                  )}
                </td>
                <td className={`px-4 py-3 text-sm ${getVarianceColor(row.variance || 0)}`}>
                  {row.variance > 0 ? '+' : ''}{parseFloat(row.variance || 0).toFixed(2)}
                </td>
                <td className="px-4 py-3">
                  {row.canEdit && !row.isLocked ? (
                    <input
                      type="text"
                      className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
                      value={row.remarks || ''}
                      onChange={(e) => handleCellChange(index, 'remarks', e.target.value)}
                      placeholder="Enter remarks"
                    />
                  ) : (
                    <span className="text-sm text-gray-500">{row.remarks || '-'}</span>
                  )}
                </td>
                <td className="px-4 py-3 text-sm">
                  {row.isLocked ? (
                    <span className="px-2 py-1 bg-gray-200 text-gray-700 rounded text-xs">
                      Locked
                    </span>
                  ) : !row.canEdit ? (
                    <span className="px-2 py-1 bg-yellow-200 text-yellow-700 rounded text-xs">
                      Restricted
                    </span>
                  ) : (
                    <span className="px-2 py-1 bg-green-200 text-green-700 rounded text-xs">
                      Editable
                    </span>
                  )}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm font-medium">
                  {row.updateId && row.canEdit && !row.isLocked && (
                    <button
                      onClick={() => handleDelete(row.updateId, index)}
                      className="text-red-600 hover:text-red-900"
                      title="Delete this update"
                    >
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {updates.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            No day-wise updates available. Task may not have a date range or plan version.
          </div>
        )}
      </div>

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
  )
}

export default DayWiseGrid

