import { useEffect, useMemo, useState } from 'react'
import PropTypes from 'prop-types'

const defaultFormState = {
  resourceId: '',
  startDate: '',
  endDate: '',
  hoursPerDay: '',
  remarks: ''
}

const AllocationModal = ({
  isOpen,
  type,
  resources,
  initialValues,
  onSubmit,
  onClose,
  onPreviewCost
}) => {
  const [form, setForm] = useState(defaultFormState)
  const [preview, setPreview] = useState(null)
  const [previewLoading, setPreviewLoading] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setForm({
        resourceId: initialValues?.resourceId || '',
        startDate: initialValues?.startDate || '',
        endDate: initialValues?.endDate || '',
        hoursPerDay: initialValues?.hoursPerDay || '',
        remarks: initialValues?.remarks || ''
      })
    } else {
      setForm(defaultFormState)
      setPreview(null)
    }
  }, [isOpen, initialValues])

  useEffect(() => {
    const canPreview = Boolean(form.resourceId && form.startDate && form.endDate && onPreviewCost)
    if (!canPreview) {
      setPreview(null)
      return
    }

    setPreviewLoading(true)
    onPreviewCost(form.resourceId, form.startDate, form.endDate)
      .then((response) => setPreview(response.data))
      .catch(() => setPreview(null))
      .finally(() => setPreviewLoading(false))
  }, [form.resourceId, form.startDate, form.endDate, onPreviewCost])

  const title = useMemo(
    () => (type === 'MANPOWER' ? 'Allocate Manpower' : 'Allocate Equipment'),
    [type]
  )

  const resourceLabel = type === 'MANPOWER' ? 'Employee' : 'Equipment'

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    if (!form.resourceId || !form.startDate || !form.endDate) return

    onSubmit({
      resourceId: Number(form.resourceId),
      startDate: form.startDate,
      endDate: form.endDate,
      hoursPerDay: form.hoursPerDay ? Number(form.hoursPerDay) : null,
      remarks: form.remarks
    })
  }

  if (!isOpen) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">{title}</h2>
            <p className="text-sm text-gray-500">Fill in the details to reserve this resource.</p>
          </div>
          <button
            className="rounded-full p-1 text-gray-500 hover:bg-gray-100"
            onClick={onClose}
            type="button"
          >
            ✕
          </button>
        </div>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="text-sm font-medium text-gray-700">
              {resourceLabel}
              <select
                name="resourceId"
                value={form.resourceId}
                onChange={handleChange}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                required
              >
                <option value="">Select {resourceLabel.toLowerCase()}</option>
                {resources.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.name} · ₹{option.ratePerDay}/day
                  </option>
                ))}
              </select>
            </label>
            <label className="text-sm font-medium text-gray-700">
              Hours / Day
              <input
                type="number"
                min="0"
                step="0.5"
                name="hoursPerDay"
                value={form.hoursPerDay}
                onChange={handleChange}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              />
            </label>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="text-sm font-medium text-gray-700">
              Start Date
              <input
                type="date"
                name="startDate"
                value={form.startDate}
                onChange={handleChange}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                required
              />
            </label>
            <label className="text-sm font-medium text-gray-700">
              End Date
              <input
                type="date"
                name="endDate"
                value={form.endDate}
                onChange={handleChange}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                required
              />
            </label>
          </div>

          <label className="text-sm font-medium text-gray-700">
            Remarks
            <textarea
              name="remarks"
              rows="3"
              value={form.remarks}
              onChange={handleChange}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              placeholder="Any additional context"
            />
          </label>

          <div className="rounded-xl bg-slate-50 p-4">
            <p className="text-sm font-medium text-slate-600">Cost preview</p>
            {previewLoading && <p className="text-sm text-slate-500">Calculating…</p>}
            {!previewLoading && preview && (
              <div className="mt-2 flex items-center justify-between text-sm">
                <span>
                  {preview.totalDays} days × ₹{preview.ratePerDay}/day
                </span>
                <span className="text-lg font-semibold text-slate-900">₹{preview.totalCost}</span>
              </div>
            )}
            {!previewLoading && !preview && (
              <p className="text-sm text-slate-500">Select resource & date range for cost preview.</p>
            )}
          </div>

          <div className="flex justify-end space-x-3 pt-2">
            <button
              type="button"
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              onClick={onClose}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-semibold text-white shadow hover:bg-blue-700"
            >
              Save Allocation
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

AllocationModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  type: PropTypes.oneOf(['MANPOWER', 'EQUIPMENT']).isRequired,
  resources: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
      ratePerDay: PropTypes.number
    })
  ),
  initialValues: PropTypes.object,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
  onPreviewCost: PropTypes.func
}

export default AllocationModal

