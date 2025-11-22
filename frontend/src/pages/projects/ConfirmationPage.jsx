import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import ConfirmationLockBanner from '../../components/ConfirmationLockBanner'
import ConfirmationHistoryTable from '../../components/ConfirmationHistoryTable'
import { confirmationService } from '../../services/confirmationService'
import { useAuth } from '../../context/AuthContext'

const numberFormat = (value) => {
  if (value === null || value === undefined) return '0.00'
  return Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const ConfirmationPage = () => {
  const { wbsId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [summary, setSummary] = useState(null)
  const [history, setHistory] = useState([])
  const [loadingSummary, setLoadingSummary] = useState(true)
  const [loadingHistory, setLoadingHistory] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    confirmationDate: new Date().toISOString().split('T')[0],
    remarks: ''
  })
  const [previewQty, setPreviewQty] = useState(null)
  const [showPreview, setShowPreview] = useState(false)

  const canEdit = user?.permissions?.includes('PAGE_CONFIRMATION_EDIT')
  const canUndo = user?.permissions?.includes('PAGE_CONFIRMATION_ADMIN')

  const loadSummary = async (previewDate) => {
    if (!wbsId) return
    setLoadingSummary(true)
    try {
      const response = await confirmationService.getSummary(wbsId, previewDate)
      setSummary(response.data)
      setPreviewQty(response.data.previewActualQty ?? null)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load confirmation summary')
    } finally {
      setLoadingSummary(false)
    }
  }

  const loadHistory = async () => {
    if (!wbsId) return
    setLoadingHistory(true)
    try {
      const response = await confirmationService.getHistory(wbsId)
      setHistory(response.data || [])
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load confirmation history')
    } finally {
      setLoadingHistory(false)
    }
  }

  useEffect(() => {
    setPreviewQty(null)
    loadSummary()
    loadHistory()
  }, [wbsId])

  const handleChange = (event) => {
    const { name, value } = event.target
    const updated = { ...form, [name]: value }
    setForm(updated)
    if (name === 'confirmationDate') {
      loadSummary(value)
    }
  }

  const handleConfirm = (event) => {
    event.preventDefault()
    setShowPreview(true)
  }

  const executeConfirmation = async () => {
    try {
      const response = await confirmationService.confirmWbs(wbsId, form)
      setSummary(response.data)
      setPreviewQty(null)
      setForm((prev) => ({ ...prev, remarks: '' }))
      setShowPreview(false)
      setError('')
      loadHistory()
    } catch (err) {
      setShowPreview(false)
      setError(err.response?.data?.message || 'Failed to confirm WBS')
    }
  }

  const handleUndo = async (confirmation) => {
    if (!window.confirm(`Undo confirmation dated ${confirmation.confirmationDate}?`)) return
    try {
      const response = await confirmationService.undoConfirmation(confirmation.confirmationId)
      setSummary(response.data)
      setError('')
      loadHistory()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to undo confirmation')
    }
  }

  const planned = summary?.plannedQty ?? 0
  const actual = summary?.actualQty ?? 0
  const confirmed = summary?.confirmedQtyToDate ?? 0
  const variance = summary?.variance ?? 0

  return (
    <div className="max-w-6xl mx-auto py-6 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="text-sm text-indigo-600 hover:text-indigo-900"
          >
            ← Back
          </button>
          <h1 className="mt-2 text-3xl font-bold text-gray-900">
            Confirmations {summary?.wbsName && `— ${summary.wbsName}`}
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            Freeze WBS progress for auditable reporting.
          </p>
        </div>
        <button
          onClick={() => navigate(`/wbs/${wbsId}/tasks`)}
          className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          View Tasks
        </button>
      </div>

      {error && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900">Summary</h2>
          <p className="text-sm text-gray-500">Plan vs Actual vs Confirmed</p>
          <div className="mt-4 grid grid-cols-2 gap-4">
            <div className="rounded-lg border border-gray-100 bg-gray-50 p-4">
              <p className="text-xs uppercase text-gray-500">Planned</p>
              <p className="text-2xl font-semibold text-gray-900">{numberFormat(planned)}</p>
            </div>
            <div className="rounded-lg border border-gray-100 bg-gray-50 p-4">
              <p className="text-xs uppercase text-gray-500">Actual</p>
              <p className="text-2xl font-semibold text-gray-900">{numberFormat(actual)}</p>
            </div>
            <div className="rounded-lg border border-gray-100 bg-gray-50 p-4">
              <p className="text-xs uppercase text-gray-500">Confirmed</p>
              <p className="text-2xl font-semibold text-gray-900">{numberFormat(confirmed)}</p>
            </div>
            <div className="rounded-lg border border-gray-100 bg-gray-50 p-4">
              <p className="text-xs uppercase text-gray-500">Variance</p>
              <p className="text-2xl font-semibold text-gray-900">{numberFormat(variance)}</p>
            </div>
          </div>
          <div className="mt-4">
            {loadingSummary ? (
              <div className="text-sm text-gray-500">Loading lock details...</div>
            ) : (
              <ConfirmationLockBanner lockDate={summary?.lockDate} />
            )}
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900">Confirm WBS Progress</h2>
          <p className="text-sm text-gray-500">
            Select a freeze date. Actual quantity for the date is auto-computed.
          </p>
          <form className="mt-4 space-y-4" onSubmit={handleConfirm}>
            <div>
              <label className="block text-sm font-medium text-gray-700">Confirmation Date</label>
              <input
                type="date"
                name="confirmationDate"
                value={form.confirmationDate}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Remarks</label>
              <textarea
                name="remarks"
                rows="3"
                value={form.remarks}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                placeholder="Optional note for audit trail"
              />
            </div>
            <div className="rounded-md border border-indigo-100 bg-indigo-50 px-4 py-3 text-sm text-indigo-800">
              Actual quantity on <strong>{form.confirmationDate}</strong>:{' '}
              <span className="font-semibold">
                {previewQty !== null ? numberFormat(previewQty) : 'Calculating...'}
              </span>
            </div>
            <button
              type="submit"
              disabled={!canEdit || loadingSummary}
              className="w-full rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-gray-300"
            >
              Confirm Now
            </button>
            {!canEdit && (
              <p className="text-center text-xs text-gray-500">
                You need PAGE_CONFIRMATION_EDIT permission to confirm.
              </p>
            )}
          </form>
        </div>
      </div>

      <div className="mt-8 rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">Confirmation History</h2>
            <p className="text-sm text-gray-500">
              Sorted by confirmation date (latest first).
            </p>
          </div>
          <button
            onClick={() => loadHistory()}
            className="text-sm font-medium text-indigo-600 hover:text-indigo-800"
          >
            Refresh
          </button>
        </div>
        <div className="mt-4">
          <ConfirmationHistoryTable
            history={history}
            canUndo={canUndo}
            onUndo={handleUndo}
            loading={loadingHistory}
          />
        </div>
      </div>

      {showPreview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/60 px-4">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900">Confirm Freeze</h3>
            <p className="mt-2 text-sm text-gray-600">
              You are about to lock all task updates up to{' '}
              <strong>{form.confirmationDate}</strong>. Confirmed quantity will be{' '}
              <strong>{previewQty !== null ? numberFormat(previewQty) : 'calculated'}</strong>.
            </p>
            <div className="mt-4 flex justify-end space-x-3">
              <button
                onClick={() => setShowPreview(false)}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={executeConfirmation}
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default ConfirmationPage


