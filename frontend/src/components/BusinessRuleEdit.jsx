import { useState, useEffect } from 'react'
import api from '../services/api'

const BusinessRuleEdit = ({ rule, controlPoints, onClose }) => {
  const [formData, setFormData] = useState({
    ruleNumber: '',
    controlPoint: '',
    applicability: 'Y',
    ruleValue: '',
    description: '',
    activateFlag: true
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (rule) {
      setFormData({
        ruleNumber: rule.ruleNumber || '',
        controlPoint: rule.controlPoint || '',
        applicability: rule.applicability || 'Y',
        ruleValue: rule.ruleValue || '',
        description: rule.description || '',
        activateFlag: rule.activateFlag !== undefined ? rule.activateFlag : true
      })
    }
  }, [rule])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      if (rule) {
        await api.put(`/api/business-rules/${rule.ruleId}`, formData)
      } else {
        await api.post('/api/business-rules', formData)
      }
      onClose()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save business rule')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-900">
            {rule ? 'Edit Business Rule' : 'Create Business Rule'}
          </h2>
          <button
            onClick={onClose}
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
              Rule Number <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              required
              disabled={!!rule}
              className={`w-full border border-gray-300 rounded-md px-3 py-2 ${rule ? 'bg-gray-100' : ''}`}
              value={formData.ruleNumber}
              onChange={(e) => setFormData({ ...formData, ruleNumber: parseInt(e.target.value) || '' })}
              placeholder="e.g., 101, 201, 301"
            />
            <p className="mt-1 text-xs text-gray-500">
              Unique rule number. Cannot be changed after creation.
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Control Point <span className="text-red-500">*</span>
            </label>
            <select
              required
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={formData.controlPoint}
              onChange={(e) => setFormData({ ...formData, controlPoint: e.target.value })}
            >
              <option value="">Select Control Point</option>
              {controlPoints.map((cp) => (
                <option key={cp} value={cp}>
                  {cp}
                </option>
              ))}
              <option value="__NEW__">+ Create New Control Point</option>
            </select>
            {formData.controlPoint === '__NEW__' && (
              <input
                type="text"
                placeholder="Enter new control point"
                className="mt-2 w-full border border-gray-300 rounded-md px-3 py-2"
                onChange={(e) => setFormData({ ...formData, controlPoint: e.target.value })}
              />
            )}
            <p className="mt-1 text-xs text-gray-500">
              Where this rule applies (TASK_UPDATE, WBS, TASK, CONFIRMATION, etc.)
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Applicability <span className="text-red-500">*</span>
            </label>
            <select
              required
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={formData.applicability}
              onChange={(e) => setFormData({ ...formData, applicability: e.target.value })}
            >
              <option value="Y">Y - Applicable (Rule is active)</option>
              <option value="N">N - Not Applicable (Rule is disabled)</option>
            </select>
            <p className="mt-1 text-xs text-gray-500">
              Y = Rule will be validated, N = Rule will be skipped
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Rule Value
            </label>
            <input
              type="text"
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={formData.ruleValue}
              onChange={(e) => setFormData({ ...formData, ruleValue: e.target.value })}
              placeholder="e.g., 7 (for days), Y/N, or leave empty"
              maxLength={500}
            />
            <p className="mt-1 text-xs text-gray-500">
              Rule-specific value (e.g., number of days, Y/N flag, etc.). Leave empty if not needed.
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              rows={4}
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Description of what this rule validates and why"
              maxLength={1000}
            />
            <p className="mt-1 text-xs text-gray-500">
              Human-readable description of the rule's purpose and behavior.
            </p>
          </div>

          <div>
            <label className="flex items-center">
              <input
                type="checkbox"
                className="mr-2"
                checked={formData.activateFlag}
                onChange={(e) => setFormData({ ...formData, activateFlag: e.target.checked })}
              />
              <span className="text-sm font-medium text-gray-700">Active</span>
            </label>
            <p className="mt-1 text-xs text-gray-500">
              Uncheck to temporarily disable this rule without deleting it.
            </p>
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50"
            >
              {loading ? 'Saving...' : rule ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default BusinessRuleEdit

