import api from './api'

export const getManpowerAllocations = (wbsId) =>
  api.get(`/api/resources/manpower/wbs/${wbsId}`)

export const getEquipmentAllocations = (wbsId) =>
  api.get(`/api/resources/equipment/wbs/${wbsId}`)

export const createManpowerAllocation = (payload) =>
  api.post('/api/resources/manpower', payload)

export const updateManpowerAllocation = (allocationId, payload) =>
  api.put(`/api/resources/manpower/${allocationId}`, payload)

export const deleteManpowerAllocation = (allocationId) =>
  api.delete(`/api/resources/manpower/${allocationId}`)

export const createEquipmentAllocation = (payload) =>
  api.post('/api/resources/equipment', payload)

export const updateEquipmentAllocation = (allocationId, payload) =>
  api.put(`/api/resources/equipment/${allocationId}`, payload)

export const deleteEquipmentAllocation = (allocationId) =>
  api.delete(`/api/resources/equipment/${allocationId}`)

export const getEmployeeOptions = (search) =>
  api.get('/api/resources/manpower/options', {
    params: { search }
  })

export const getEquipmentOptions = (search) =>
  api.get('/api/resources/equipment/options', {
    params: { search }
  })

export const getTimeline = (wbsId) => api.get(`/api/resources/timeline/wbs/${wbsId}`)

export const getCostSummary = (wbsId) => api.get(`/api/resources/cost/wbs/${wbsId}`)

export const previewManpowerCost = (employeeId, startDate, endDate) =>
  api.get(`/api/resources/cost/manpower/${employeeId}`, {
    params: { startDate, endDate }
  })

export const previewEquipmentCost = (equipmentId, startDate, endDate) =>
  api.get(`/api/resources/cost/equipment/${equipmentId}`, {
    params: { startDate, endDate }
  })

