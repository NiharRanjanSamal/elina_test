import api from './api'

export const confirmationService = {
  confirmWbs: (wbsId, payload) =>
    api.post(`/api/confirmations/wbs/${wbsId}`, payload),

  getSummary: (wbsId, previewDate) =>
    api.get(`/api/confirmations/wbs/${wbsId}/summary`, {
      params: previewDate ? { previewDate } : {}
    }),

  getHistory: (wbsId) => api.get(`/api/confirmations/wbs/${wbsId}`),

  undoConfirmation: (confirmationId) =>
    api.delete(`/api/confirmations/${confirmationId}`)
}


