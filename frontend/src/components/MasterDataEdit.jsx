import { useState, useEffect } from 'react'
import api from '../services/api'

const MasterDataEdit = ({ code, codeTypes, onClose }) => {
  const [formData, setFormData] = useState({
    codeType: '',
    codeValue: '',
    shortDescription: '',
    longDescription: '',
    activateFlag: true
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [codeTypeInfo, setCodeTypeInfo] = useState(null)

  useEffect(() => {
    if (code) {
      setFormData({
        codeType: code.codeType || '',
        codeValue: code.codeValue || '',
        shortDescription: code.shortDescription || '',
        longDescription: code.longDescription || '',
        activateFlag: code.activateFlag !== undefined ? code.activateFlag : true
      })
      // Fetch code type info if available
      if (code.codeType) {
        fetchCodeTypeInfo(code.codeType)
      }
    }
  }, [code])

  const fetchCodeTypeInfo = async (codeType) => {
    try {
      // Try to get a sample code to show long_description for the code type
      const response = await api.get(`/api/master-codes/by-type/${codeType}`)
      if (response.data && response.data.length > 0 && response.data[0].longDescription) {
        setCodeTypeInfo(response.data[0].longDescription)
      }
    } catch (err) {
      // Ignore errors - code type info is optional
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      if (code) {
        await api.put(`/api/master-codes/${code.codeId}`, formData)
      } else {
        await api.post('/api/master-codes', formData)
      }
      onClose()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save master code')
    } finally {
      setLoading(false)
    }
  }

  const handleCodeTypeChange = (e) => {
    const newCodeType = e.target.value
    setFormData({ ...formData, codeType: newCodeType })
    if (newCodeType) {
      fetchCodeTypeInfo(newCodeType)
    } else {
      setCodeTypeInfo(null)
    }
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-900">
            {code ? 'Edit Master Code' : 'Create Master Code'}
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

        {/* Code Type Info Box */}
        {codeTypeInfo && (
          <div className="mb-4 bg-blue-50 border border-blue-200 rounded-md p-4">
            <h3 className="text-sm font-semibold text-blue-900 mb-2">About {formData.codeType}:</h3>
            <div className="text-sm text-blue-800 whitespace-pre-wrap">{codeTypeInfo}</div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Code Type <span className="text-red-500">*</span>
            </label>
            {code ? (
              <input
                type="text"
                required
                disabled
                className="w-full border border-gray-300 rounded-md px-3 py-2 bg-gray-100"
                value={formData.codeType}
              />
            ) : (
              <select
                required
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={formData.codeType}
                onChange={handleCodeTypeChange}
              >
                <option value="">Select Code Type</option>
                {codeTypes.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
                <option value="__NEW__">+ Create New Code Type</option>
              </select>
            )}
            {formData.codeType === '__NEW__' && (
              <input
                type="text"
                placeholder="Enter new code type"
                className="mt-2 w-full border border-gray-300 rounded-md px-3 py-2"
                onChange={(e) => setFormData({ ...formData, codeType: e.target.value })}
              />
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Code Value <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={formData.codeValue}
              onChange={(e) => setFormData({ ...formData, codeValue: e.target.value })}
              placeholder="e.g., WC_SITE, CC_001"
            />
            <p className="mt-1 text-xs text-gray-500">
              Use intelligent naming conventions (e.g., WORK_CENTER codes: WC_SITE, WC_FOUND)
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Short Description
              {(formData.codeType === 'WORK_CENTER' || formData.codeType === 'COST_CENTER') && (
                <span className="text-red-500"> *</span>
              )}
            </label>
            <input
              type="text"
              required={formData.codeType === 'WORK_CENTER' || formData.codeType === 'COST_CENTER'}
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={formData.shortDescription}
              onChange={(e) => setFormData({ ...formData, shortDescription: e.target.value })}
              placeholder="Brief purpose/help text"
              maxLength={500}
            />
            <p className="mt-1 text-xs text-gray-500">
              Brief description shown in dropdowns and tooltips
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Long Description (Inline Documentation)
            </label>
            <textarea
              rows={6}
              className="w-full border border-gray-300 rounded-md px-3 py-2"
              value={formData.longDescription}
              onChange={(e) => setFormData({ ...formData, longDescription: e.target.value })}
              placeholder="Detailed documentation for developers/users. Supports markdown."
            />
            <p className="mt-1 text-xs text-gray-500">
              Detailed documentation shown in info boxes. Supports markdown formatting.
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
              Only active codes are shown in selection components
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
              {loading ? 'Saving...' : code ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default MasterDataEdit

