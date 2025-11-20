import { useState, useEffect } from 'react'
import api from '../services/api'

/**
 * MasterDataSelect component - intelligently renders radio buttons or dropdown
 * based on the number of active codes for the given code type.
 * 
 * Props:
 * - codeType: The code type to fetch (required)
 * - value: Current selected value
 * - onChange: Callback when value changes
 * - label: Label for the field
 * - required: Whether field is required
 * - limitForRadio: Maximum count to show radio buttons (default: 3)
 * - placeholder: Placeholder text for dropdown
 * - className: Additional CSS classes
 */
const MasterDataSelect = ({
  codeType,
  value,
  onChange,
  label,
  required = false,
  limitForRadio = 3,
  placeholder = 'Select...',
  className = ''
}) => {
  const [codes, setCodes] = useState([])
  const [loading, setLoading] = useState(true)
  const [useRadio, setUseRadio] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (codeType) {
      fetchCodes()
      checkCount()
    }
  }, [codeType, limitForRadio])

  const fetchCodes = async () => {
    try {
      setLoading(true)
      const response = await api.get(`/api/master-codes/by-type/${codeType}`)
      setCodes(response.data || [])
      setError('')
    } catch (err) {
      setError('Failed to load codes')
      console.error('Error fetching master codes:', err)
    } finally {
      setLoading(false)
    }
  }

  const checkCount = async () => {
    try {
      const response = await api.get(`/api/master-codes/count?codeType=${codeType}&limit=${limitForRadio}`)
      setUseRadio(response.data.useRadio || false)
    } catch (err) {
      // Fallback: use dropdown if count check fails
      setUseRadio(false)
    }
  }

  if (loading) {
    return (
      <div className={className}>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {label}
            {required && <span className="text-red-500 ml-1">*</span>}
          </label>
        )}
        <div className="text-sm text-gray-500">Loading...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className={className}>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {label}
            {required && <span className="text-red-500 ml-1">*</span>}
          </label>
        )}
        <div className="text-sm text-red-500">{error}</div>
      </div>
    )
  }

  if (codes.length === 0) {
    return (
      <div className={className}>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {label}
            {required && <span className="text-red-500 ml-1">*</span>}
          </label>
        )}
        <div className="text-sm text-gray-500">No codes available for {codeType}</div>
      </div>
    )
  }

  return (
    <div className={className}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
        </label>
      )}

      {useRadio ? (
        // Render Radio Group
        <div className="space-y-2">
          {codes.map((code) => (
            <label
              key={code.codeId}
              className="flex items-start p-3 border border-gray-300 rounded-md hover:bg-gray-50 cursor-pointer"
            >
              <input
                type="radio"
                name={`master-code-${codeType}`}
                value={code.codeValue}
                checked={value === code.codeValue}
                onChange={(e) => onChange(e.target.value)}
                className="mt-1 mr-3"
                required={required}
              />
              <div className="flex-1">
                <div className="font-medium text-gray-900">{code.codeValue}</div>
                {code.shortDescription && (
                  <div className="text-sm text-gray-500 mt-1">{code.shortDescription}</div>
                )}
              </div>
            </label>
          ))}
        </div>
      ) : (
        // Render Dropdown (Select)
        <select
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          required={required}
          className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          <option value="">{placeholder}</option>
          {codes.map((code) => (
            <option key={code.codeId} value={code.codeValue}>
              {code.codeValue} {code.shortDescription ? `- ${code.shortDescription}` : ''}
            </option>
          ))}
        </select>
      )}

      {/* Show tooltip/info for selected code */}
      {value && codes.find(c => c.codeValue === value)?.shortDescription && (
        <div className="mt-2 text-sm text-gray-600">
          {codes.find(c => c.codeValue === value).shortDescription}
        </div>
      )}
    </div>
  )
}

export default MasterDataSelect

