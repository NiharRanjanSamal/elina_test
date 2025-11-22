import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import AllocationModal from '../../components/AllocationModal'
import AllocationTimeline from '../../components/AllocationTimeline'
import ResourceSummaryWidget from '../../components/ResourceSummaryWidget'
import api from '../../services/api'
import {
  createEquipmentAllocation,
  createManpowerAllocation,
  deleteEquipmentAllocation,
  deleteManpowerAllocation,
  getCostSummary,
  getEmployeeOptions,
  getEquipmentAllocations,
  getEquipmentOptions,
  getManpowerAllocations,
  getTimeline,
  previewEquipmentCost,
  previewManpowerCost,
  updateEquipmentAllocation,
  updateManpowerAllocation
} from '../../services/resourceService'

const currency = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 0
})

const ResourceAllocationPage = () => {
  const { wbsId } = useParams()
  const [wbs, setWbs] = useState(null)
  const [manpower, setManpower] = useState([])
  const [equipment, setEquipment] = useState([])
  const [employeeOptions, setEmployeeOptions] = useState([])
  const [equipmentOptions, setEquipmentOptions] = useState([])
  const [costSummary, setCostSummary] = useState(null)
  const [timeline, setTimeline] = useState([])
  const [activeTab, setActiveTab] = useState('MANPOWER')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalState, setModalState] = useState({ open: false, type: 'MANPOWER', allocation: null })

  const fetchData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const [
        wbsRes,
        manpowerRes,
        equipmentRes,
        costRes,
        timelineRes,
        employeeRes,
        equipmentOptionsRes
      ] = await Promise.all([
        api.get(`/api/wbs/${wbsId}`),
        getManpowerAllocations(wbsId),
        getEquipmentAllocations(wbsId),
        getCostSummary(wbsId),
        getTimeline(wbsId),
        getEmployeeOptions(),
        getEquipmentOptions()
      ])

      setWbs(wbsRes.data)
      setManpower(manpowerRes.data || [])
      setEquipment(equipmentRes.data || [])
      setCostSummary(costRes.data)
      setTimeline(timelineRes.data || [])
      setEmployeeOptions(employeeRes.data || [])
      setEquipmentOptions(equipmentOptionsRes.data || [])
    } catch (err) {
      console.error(err)
      setError('Failed to load allocations. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [wbsId])

  useEffect(() => {
    if (wbsId) {
      fetchData()
    }
  }, [wbsId, fetchData])

  const handleSaveAllocation = async (payload) => {
    const basePayload = {
      wbsId: Number(wbsId),
      startDate: payload.startDate,
      endDate: payload.endDate,
      hoursPerDay: payload.hoursPerDay,
      remarks: payload.remarks
    }

    try {
      if (modalState.type === 'MANPOWER') {
        const request = { ...basePayload, employeeId: payload.resourceId }
        if (modalState.allocation) {
          await updateManpowerAllocation(modalState.allocation.allocationId, request)
        } else {
          await createManpowerAllocation(request)
        }
      } else {
        const request = { ...basePayload, equipmentId: payload.resourceId }
        if (modalState.allocation) {
          await updateEquipmentAllocation(modalState.allocation.allocationId, request)
        } else {
          await createEquipmentAllocation(request)
        }
      }
      setModalState({ open: false, type: modalState.type, allocation: null })
      fetchData()
    } catch (err) {
      console.error(err)
    }
  }

  const handleDelete = async (allocation, type) => {
    if (!window.confirm('Delete this allocation?')) return
    try {
      if (type === 'MANPOWER') {
        await deleteManpowerAllocation(allocation.allocationId)
      } else {
        await deleteEquipmentAllocation(allocation.allocationId)
      }
      fetchData()
    } catch (err) {
      console.error(err)
    }
  }

  const openModal = (type, allocation = null) => {
    setModalState({ open: true, type, allocation })
  }

  const closeModal = () => setModalState({ open: false, type: 'MANPOWER', allocation: null })

  const tableData = activeTab === 'MANPOWER' ? manpower : equipment

  const previewFn =
    modalState.type === 'MANPOWER'
      ? (resourceId, start, end) => previewManpowerCost(resourceId, start, end)
      : (resourceId, start, end) => previewEquipmentCost(resourceId, start, end)

  const modalInitialValues = modalState.allocation
    ? {
        resourceId:
          modalState.type === 'MANPOWER'
            ? modalState.allocation.employeeId
            : modalState.allocation.equipmentId,
        startDate: modalState.allocation.startDate,
        endDate: modalState.allocation.endDate,
        hoursPerDay: modalState.allocation.hoursPerDay,
        remarks: modalState.allocation.remarks
      }
    : null

  const summary = useMemo(() => {
    if (!costSummary) return null
    return {
      manpowerCost: Number(costSummary.manpowerCost || 0),
      equipmentCost: Number(costSummary.equipmentCost || 0),
      totalCost: Number(costSummary.totalCost || 0)
    }
  }, [costSummary])

  const columns =
    activeTab === 'MANPOWER'
      ? [
          { label: 'Employee', field: 'employeeName', helper: (row) => row.skillLevel },
          { label: 'Date Range', field: null, helper: (row) => `${row.startDate} → ${row.endDate}` },
          { label: 'Rate / Day', field: 'ratePerDay', format: (val) => currency.format(val || 0) },
          { label: 'Total Cost', field: 'totalCost', format: (val) => currency.format(val || 0) }
        ]
      : [
          { label: 'Equipment', field: 'equipmentName', helper: (row) => row.equipmentType },
          { label: 'Date Range', field: null, helper: (row) => `${row.startDate} → ${row.endDate}` },
          { label: 'Rate / Day', field: 'ratePerDay', format: (val) => currency.format(val || 0) },
          { label: 'Total Cost', field: 'totalCost', format: (val) => currency.format(val || 0) }
        ]

  if (loading) {
    return <div className="p-6 text-sm text-gray-500">Loading resource allocations…</div>
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">{error}</div>
      </div>
    )
  }

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wide text-gray-500">Resource Allocation</p>
          <h1 className="text-2xl font-semibold text-gray-900">
            {wbs?.wbsCode} · {wbs?.wbsName}
          </h1>
          <p className="text-sm text-gray-500">
            {wbs?.startDate} → {wbs?.endDate}
          </p>
        </div>
        <button
          className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-semibold text-white shadow hover:bg-blue-700"
          onClick={() => openModal(activeTab)}
        >
          Allocate Resource
        </button>
      </div>

      <ResourceSummaryWidget summary={summary} />

      <div>
        <div className="mb-4 flex space-x-4 border-b">
          {['MANPOWER', 'EQUIPMENT'].map((tab) => (
            <button
              key={tab}
              className={`pb-2 text-sm font-medium ${
                activeTab === tab
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveTab(tab)}
            >
              {tab === 'MANPOWER' ? 'Manpower' : 'Equipment'}
            </button>
          ))}
        </div>
        <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {columns.map((col) => (
                  <th
                    key={col.label}
                    className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500"
                  >
                    {col.label}
                  </th>
                ))}
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-gray-500">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {tableData.length === 0 && (
                <tr>
                  <td colSpan={columns.length + 1} className="px-4 py-6 text-center text-sm text-gray-500">
                    No allocations yet. Click “Allocate Resource” to get started.
                  </td>
                </tr>
              )}
              {tableData.map((row) => (
                <tr key={row.allocationId} className="hover:bg-gray-50">
                  {columns.map((col) => {
                    const rawValue = col.field ? row[col.field] : null
                    const primary = col.field
                      ? col.format
                        ? col.format(rawValue)
                        : rawValue
                      : col.helper?.(row)
                    const secondary = col.field && col.helper ? col.helper(row) : null
                    return (
                      <td key={col.label} className="px-4 py-3 text-sm text-gray-700">
                        <div className="flex flex-col">
                          <span className="font-medium text-gray-900">{primary}</span>
                          {secondary && <span className="text-xs text-gray-500">{secondary}</span>}
                        </div>
                      </td>
                    )
                  })}
                  <td className="px-4 py-3 text-right text-sm">
                    <div className="space-x-2">
                      <button
                        className="text-blue-600 hover:text-blue-800"
                        onClick={() => openModal(activeTab, row)}
                      >
                        Edit
                      </button>
                      <button
                        className="text-red-600 hover:text-red-800"
                        onClick={() => handleDelete(row, activeTab)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">Allocation Timeline</h2>
            <p className="text-sm text-gray-500">Visual snapshot of resource bookings.</p>
          </div>
        </div>
        <AllocationTimeline timeline={timeline} wbsStart={wbs?.startDate} wbsEnd={wbs?.endDate} />
      </div>

      <AllocationModal
        isOpen={modalState.open}
        type={modalState.type}
        resources={modalState.type === 'MANPOWER' ? employeeOptions : equipmentOptions}
        initialValues={modalInitialValues}
        onSubmit={handleSaveAllocation}
        onClose={closeModal}
        onPreviewCost={previewFn}
      />
    </div>
  )
}

export default ResourceAllocationPage

