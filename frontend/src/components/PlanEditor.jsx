import { useState } from 'react'
import api from '../services/api'

const PlanEditor = ({ task, onSuccess, onCancel }) => {
  const [mode, setMode] = useState('DAILY_ENTRY')
  const [versionDate, setVersionDate] = useState(new Date().toISOString().split('T')[0])
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [validating, setValidating] = useState(false)

  // Daily Entry Mode
  const [dailyLines, setDailyLines] = useState([{ plannedDate: '', plannedQty: '', description: '' }])

  // Date Range Split Mode
  const [rangeSplit, setRangeSplit] = useState({
    startDate: '',
    endDate: '',
    totalQty: '',
    splitType: 'EQUAL_SPLIT',
    splitCount: '',
    customQuantities: []
  })

  // Single Line Quick Mode
  const [singleLine, setSingleLine] = useState({
    plannedDate: '',
    plannedQty: '',
    description: ''
  })

  // Validate single field before submit (inline validation)
  const validateField = async (fieldName, value) => {
    if (!value) return

    setValidating(true)
    try {
      // Call business rule validation endpoint
      await api.post('/api/business-rules/validate-single', {
        ruleNumber: getRuleNumberForField(fieldName),
        context: {
          fieldName,
          value,
          taskId: task?.taskId,
          date: fieldName.includes('Date') ? value : null
        }
      })
      setError('')
    } catch (err) {
      if (err.response?.data?.ruleNumber) {
        setError(`${err.response.data.message} (Rule ${err.response.data.ruleNumber})`)
      }
    } finally {
      setValidating(false)
    }
  }

  const getRuleNumberForField = (fieldName) => {
    if (fieldName.includes('Date')) return 201 // START_DATE_CANNOT_BE_IN_FUTURE
    if (fieldName.includes('backdate')) return 101 // BACKDATE_ALLOWED_TILL
    return null
  }

  const addDailyLine = () => {
    setDailyLines([...dailyLines, { plannedDate: '', plannedQty: '', description: '' }])
  }

  const removeDailyLine = (index) => {
    setDailyLines(dailyLines.filter((_, i) => i !== index))
  }

  const updateDailyLine = (index, field, value) => {
    const updated = [...dailyLines]
    updated[index] = { ...updated[index], [field]: value }
    setDailyLines(updated)
    if (field === 'plannedDate') {
      validateField('plannedDate', value)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      let payload = {
        taskId: task.taskId,
        versionDate,
        description: description || null,
        mode
      }

      switch (mode) {
        case 'DAILY_ENTRY':
          const validDailyLines = dailyLines.filter(l => l.plannedDate && l.plannedQty)
          if (validDailyLines.length === 0) {
            setError('At least one plan line with date and quantity is required')
            setLoading(false)
            return
          }
          payload.dailyLines = validDailyLines.map(l => ({
            plannedDate: l.plannedDate,
            plannedQty: parseFloat(l.plannedQty),
            description: l.description || null
          }))
          break

        case 'DATE_RANGE_SPLIT':
          if (!rangeSplit.startDate || !rangeSplit.endDate || !rangeSplit.totalQty) {
            setError('Start date, end date, and total quantity are required')
            setLoading(false)
            return
          }
          if (rangeSplit.splitType === 'CUSTOM_SPLIT' && (!rangeSplit.splitCount || rangeSplit.customQuantities.length !== parseInt(rangeSplit.splitCount))) {
            setError('For custom split, provide split count and matching quantities')
            setLoading(false)
            return
          }
          payload.rangeSplit = {
            startDate: rangeSplit.startDate,
            endDate: rangeSplit.endDate,
            totalQty: parseFloat(rangeSplit.totalQty),
            splitType: rangeSplit.splitType,
            splitCount: rangeSplit.splitType === 'CUSTOM_SPLIT' ? parseInt(rangeSplit.splitCount) : null,
            customQuantities: rangeSplit.splitType === 'CUSTOM_SPLIT' ? rangeSplit.customQuantities.map(q => parseFloat(q)) : null
          }
          break

        case 'SINGLE_LINE_QUICK':
          if (!singleLine.plannedDate || !singleLine.plannedQty) {
            setError('Planned date and quantity are required')
            setLoading(false)
            return
          }
          payload.singleLine = {
            plannedDate: singleLine.plannedDate,
            plannedQty: parseFloat(singleLine.plannedQty),
            description: singleLine.description || null
          }
          break

        default:
          setError('Invalid mode')
          setLoading(false)
          return
      }

      // Pre-emptive validation
      setValidating(true)
      try {
        await api.post('/api/business-rules/validate-single', {
          ruleNumber: 201,
          context: {
            taskId: task.taskId,
            dates: mode === 'DAILY_ENTRY' ? dailyLines.map(l => l.plannedDate).filter(Boolean) :
                   mode === 'DATE_RANGE_SPLIT' ? [rangeSplit.startDate, rangeSplit.endDate].filter(Boolean) :
                   [singleLine.plannedDate].filter(Boolean)
          }
        })
      } catch (validationErr) {
        if (validationErr.response?.data?.ruleNumber) {
          setError(`${validationErr.response.data.message} (Rule ${validationErr.response.data.ruleNumber})`)
          setLoading(false)
          setValidating(false)
          return
        }
      }
      setValidating(false)

      await api.post('/api/plans/create-with-mode', payload)
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create plan version')
    } finally {
      setLoading(false)
    }
  }

  const updateRangeSplit = (field, value) => {
    setRangeSplit({ ...rangeSplit, [field]: value })
    if (field === 'startDate' || field === 'endDate') {
      validateField('plannedDate', value)
    }
  }

  const addCustomQuantity = () => {
    setRangeSplit({
      ...rangeSplit,
      customQuantities: [...rangeSplit.customQuantities, '']
    })
  }

  const updateCustomQuantity = (index, value) => {
    const updated = [...rangeSplit.customQuantities]
    updated[index] = value
    setRangeSplit({ ...rangeSplit, customQuantities: updated })
  }

  const removeCustomQuantity = (index) => {
    setRangeSplit({
      ...rangeSplit,
      customQuantities: rangeSplit.customQuantities.filter((_, i) => i !== index)
    })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
          {error}
        </div>
      )}

      {/* Mode Selection */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Creation Mode *
        </label>
        <div className="grid grid-cols-3 gap-4">
          <button
            type="button"
            onClick={() => setMode('DAILY_ENTRY')}
            className={`px-4 py-3 border rounded-md text-sm font-medium ${
              mode === 'DAILY_ENTRY'
                ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                : 'border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            Daily Entry
          </button>
          <button
            type="button"
            onClick={() => setMode('DATE_RANGE_SPLIT')}
            className={`px-4 py-3 border rounded-md text-sm font-medium ${
              mode === 'DATE_RANGE_SPLIT'
                ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                : 'border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            Date Range Split
          </button>
          <button
            type="button"
            onClick={() => setMode('SINGLE_LINE_QUICK')}
            className={`px-4 py-3 border rounded-md text-sm font-medium ${
              mode === 'SINGLE_LINE_QUICK'
                ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                : 'border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            Single Line Quick
          </button>
        </div>
      </div>

      {/* Common Fields */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Version Date *
          </label>
          <input
            type="date"
            required
            className="w-full border border-gray-300 rounded-md px-3 py-2"
            value={versionDate}
            onChange={(e) => {
              setVersionDate(e.target.value)
              validateField('versionDate', e.target.value)
            }}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <input
            type="text"
            className="w-full border border-gray-300 rounded-md px-3 py-2"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description"
          />
        </div>
      </div>

      {/* Mode-Specific Forms */}
      {mode === 'DAILY_ENTRY' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <label className="block text-sm font-medium text-gray-700">
              Daily Plan Lines *
            </label>
            <button
              type="button"
              onClick={addDailyLine}
              className="text-sm text-indigo-600 hover:text-indigo-900"
            >
              + Add Line
            </button>
          </div>
          <div className="max-h-96 overflow-y-auto space-y-3 border border-gray-200 rounded-md p-4">
            {dailyLines.map((line, index) => (
              <div key={index} className="grid grid-cols-3 gap-4 items-end">
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">
                    Date *
                  </label>
                  <input
                    type="date"
                    required
                    className="w-full border border-gray-300 rounded-md px-2 py-1 text-sm"
                    value={line.plannedDate}
                    onChange={(e) => updateDailyLine(index, 'plannedDate', e.target.value)}
                    min={task?.startDate}
                    max={task?.endDate}
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">
                    Quantity *
                  </label>
                  <input
                    type="number"
                    required
                    step="0.01"
                    min="0"
                    className="w-full border border-gray-300 rounded-md px-2 py-1 text-sm"
                    value={line.plannedQty}
                    onChange={(e) => updateDailyLine(index, 'plannedQty', e.target.value)}
                    placeholder="0.00"
                  />
                  {task && (
                    <p className="text-xs text-gray-400 mt-0.5">{task.unit || 'UOM'}</p>
                  )}
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    className="flex-1 border border-gray-300 rounded-md px-2 py-1 text-sm"
                    value={line.description || ''}
                    onChange={(e) => updateDailyLine(index, 'description', e.target.value)}
                    placeholder="Description (optional)"
                  />
                  {dailyLines.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeDailyLine(index)}
                      className="px-2 py-1 bg-red-100 text-red-700 rounded hover:bg-red-200 text-sm"
                    >
                      ×
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {mode === 'DATE_RANGE_SPLIT' && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Start Date *
              </label>
              <input
                type="date"
                required
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={rangeSplit.startDate}
                onChange={(e) => updateRangeSplit('startDate', e.target.value)}
                min={task?.startDate}
                max={task?.endDate}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                End Date *
              </label>
              <input
                type="date"
                required
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={rangeSplit.endDate}
                onChange={(e) => updateRangeSplit('endDate', e.target.value)}
                min={rangeSplit.startDate || task?.startDate}
                max={task?.endDate}
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Quantity *
            </label>
            <input
              type="number"
              required
              step="0.01"
              min="0"
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={rangeSplit.totalQty}
              onChange={(e) => updateRangeSplit('totalQty', e.target.value)}
              placeholder="0.00"
            />
            {task && (
              <p className="text-xs text-gray-500 mt-1">Unit: {task.unit || 'UOM'}</p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Split Type *
            </label>
            <select
              required
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={rangeSplit.splitType}
              onChange={(e) => updateRangeSplit('splitType', e.target.value)}
            >
              <option value="EQUAL_SPLIT">Equal Split (daily)</option>
              <option value="WEEKLY_SPLIT">Weekly Split</option>
              <option value="MONTHLY_SPLIT">Monthly Split</option>
              <option value="CUSTOM_SPLIT">Custom Split</option>
            </select>
          </div>
          {rangeSplit.splitType === 'CUSTOM_SPLIT' && (
            <div className="space-y-3 border border-gray-200 rounded-md p-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Number of Splits *
                </label>
                <input
                  type="number"
                  required
                  min="2"
                  className="w-full border border-gray-300 rounded-md px-3 py-2"
                  value={rangeSplit.splitCount}
                  onChange={(e) => {
                    const count = parseInt(e.target.value) || 0
                    updateRangeSplit('splitCount', count)
                    // Auto-adjust customQuantities array
                    const current = rangeSplit.customQuantities.length
                    if (count > current) {
                      setRangeSplit({
                        ...rangeSplit,
                        splitCount: count,
                        customQuantities: [...rangeSplit.customQuantities, ...Array(count - current).fill('')]
                      })
                    } else if (count < current) {
                      setRangeSplit({
                        ...rangeSplit,
                        splitCount: count,
                        customQuantities: rangeSplit.customQuantities.slice(0, count)
                      })
                    }
                  }}
                  placeholder="Number of periods"
                />
              </div>
              {rangeSplit.customQuantities.length > 0 && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Quantity per Period *
                  </label>
                  <div className="space-y-2">
                    {rangeSplit.customQuantities.map((qty, index) => (
                      <div key={index} className="flex gap-2 items-center">
                        <span className="text-sm text-gray-600 w-20">Period {index + 1}:</span>
                        <input
                          type="number"
                          required
                          step="0.01"
                          min="0"
                          className="flex-1 border border-gray-300 rounded-md px-3 py-2"
                          value={qty}
                          onChange={(e) => updateCustomQuantity(index, e.target.value)}
                          placeholder="0.00"
                        />
                        {rangeSplit.customQuantities.length > 1 && (
                          <button
                            type="button"
                            onClick={() => removeCustomQuantity(index)}
                            className="px-2 py-1 bg-red-100 text-red-700 rounded hover:bg-red-200"
                          >
                            ×
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {mode === 'SINGLE_LINE_QUICK' && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Planned Date *
              </label>
              <input
                type="date"
                required
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={singleLine.plannedDate}
                onChange={(e) => {
                  setSingleLine({ ...singleLine, plannedDate: e.target.value })
                  validateField('plannedDate', e.target.value)
                }}
                min={task?.startDate}
                max={task?.endDate}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Planned Quantity *
              </label>
              <input
                type="number"
                required
                step="0.01"
                min="0"
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={singleLine.plannedQty}
                onChange={(e) => setSingleLine({ ...singleLine, plannedQty: e.target.value })}
                placeholder="0.00"
              />
              {task && (
                <p className="text-xs text-gray-500 mt-1">Unit: {task.unit || 'UOM'}</p>
              )}
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <input
              type="text"
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={singleLine.description || ''}
              onChange={(e) => setSingleLine({ ...singleLine, description: e.target.value })}
              placeholder="Optional description"
            />
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="flex justify-end space-x-3 pt-4 border-t">
        <button
          type="button"
          onClick={onCancel}
          disabled={loading || validating}
          className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading || validating}
          className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50"
        >
          {loading || validating ? 'Creating...' : 'Create Plan Version'}
        </button>
      </div>
    </form>
  )
}

export default PlanEditor

