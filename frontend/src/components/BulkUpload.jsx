import { useState } from 'react'
import api from '../services/api'

const BulkUpload = ({ onClose }) => {
  const [file, setFile] = useState(null)
  const [dryRun, setDryRun] = useState(true)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0]
    if (selectedFile) {
      const ext = selectedFile.name.split('.').pop().toLowerCase()
      if (!['csv', 'xlsx', 'xls'].includes(ext)) {
        setError('Please select a CSV or Excel file (.csv, .xlsx, .xls)')
        return
      }
      setFile(selectedFile)
      setError('')
      setResult(null)
    }
  }

  const handleValidate = async () => {
    if (!file) {
      setError('Please select a file first')
      return
    }

    setLoading(true)
    setError('')

    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('dryRun', 'true')

      const response = await api.post('/api/master-codes/bulk-upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      setResult(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to validate file')
    } finally {
      setLoading(false)
    }
  }

  const handleCommit = async () => {
    if (!file) {
      setError('Please select a file first')
      return
    }

    if (!window.confirm('Are you sure you want to commit these changes? This will create/update master codes in the database.')) {
      return
    }

    setLoading(true)
    setError('')

    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('dryRun', 'false')

      const response = await api.post('/api/master-codes/bulk-upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      setResult(response.data)
      if (response.data.invalidRows === 0) {
        setTimeout(() => {
          onClose()
        }, 2000)
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to commit changes')
    } finally {
      setLoading(false)
    }
  }

  const downloadTemplate = () => {
    const csvContent = 'code_type,code_value,short_description,long_description\nWORK_CENTER,WC_SITE,Site Work Center,Primary work center for site operations\nCOST_CENTER,CC_001,Cost Center 001,Primary cost center for general operations'
    const blob = new Blob([csvContent], { type: 'text/csv' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'master_codes_template.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-2/3 shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-900">Bulk Upload Master Codes</h2>
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

        {/* Instructions */}
        <div className="mb-6 bg-blue-50 border border-blue-200 rounded-md p-4">
          <h3 className="text-sm font-semibold text-blue-900 mb-2">Instructions:</h3>
          <ul className="text-sm text-blue-800 list-disc list-inside space-y-1">
            <li>Upload a CSV or Excel file (.csv, .xlsx, .xls)</li>
            <li>Required columns: code_type, code_value</li>
            <li>Optional columns: short_description, long_description</li>
            <li>Use "Validate" to preview changes before committing</li>
            <li>Click "Commit" to apply changes to the database</li>
          </ul>
          <button
            onClick={downloadTemplate}
            className="mt-3 text-sm text-blue-600 hover:text-blue-800 underline"
          >
            Download CSV Template
          </button>
        </div>

        {/* File Upload */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Select File
          </label>
          <div className="flex items-center space-x-4">
            <input
              type="file"
              accept=".csv,.xlsx,.xls"
              onChange={handleFileChange}
              className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
            />
            {file && (
              <span className="text-sm text-gray-600">{file.name}</span>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end space-x-3 mb-6">
          <button
            onClick={handleValidate}
            disabled={!file || loading}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Validating...' : 'Validate'}
          </button>
          <button
            onClick={handleCommit}
            disabled={!file || loading || !result}
            className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
          >
            {loading ? 'Committing...' : 'Commit'}
          </button>
        </div>

        {/* Results */}
        {result && (
          <div className="border border-gray-200 rounded-md p-4">
            <h3 className="text-lg font-semibold mb-4">
              {result.dryRun ? 'Validation Results (Dry Run)' : 'Upload Results'}
            </h3>
            
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
              <div>
                <div className="text-sm text-gray-500">Total Rows</div>
                <div className="text-2xl font-bold">{result.totalRows}</div>
              </div>
              <div>
                <div className="text-sm text-gray-500">Valid</div>
                <div className="text-2xl font-bold text-green-600">{result.validRows}</div>
              </div>
              <div>
                <div className="text-sm text-gray-500">Invalid</div>
                <div className="text-2xl font-bold text-red-600">{result.invalidRows}</div>
              </div>
              {!result.dryRun && (
                <>
                  <div>
                    <div className="text-sm text-gray-500">Created</div>
                    <div className="text-2xl font-bold text-blue-600">{result.createdCount}</div>
                  </div>
                  <div>
                    <div className="text-sm text-gray-500">Updated</div>
                    <div className="text-2xl font-bold text-purple-600">{result.updatedCount}</div>
                  </div>
                </>
              )}
            </div>

            {/* Preview Table */}
            {result.rows && result.rows.length > 0 && (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Row</th>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Code Type</th>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Code Value</th>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Action</th>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Status</th>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Errors</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {result.rows.slice(0, 50).map((row, idx) => (
                      <tr
                        key={idx}
                        className={row.valid ? '' : 'bg-red-50'}
                      >
                        <td className="px-3 py-2">{row.rowNumber}</td>
                        <td className="px-3 py-2">{row.codeType}</td>
                        <td className="px-3 py-2">{row.codeValue}</td>
                        <td className="px-3 py-2">
                          <span className={`px-2 py-1 text-xs rounded ${
                            row.action === 'CREATE' ? 'bg-green-100 text-green-800' :
                            row.action === 'UPDATE' ? 'bg-blue-100 text-blue-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {row.action || '-'}
                          </span>
                        </td>
                        <td className="px-3 py-2">
                          <span className={`px-2 py-1 text-xs rounded ${
                            row.valid ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {row.valid ? 'Valid' : 'Invalid'}
                          </span>
                        </td>
                        <td className="px-3 py-2 text-red-600 text-xs">
                          {row.errors && row.errors.length > 0 ? row.errors.join(', ') : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {result.rows.length > 50 && (
                  <div className="mt-2 text-sm text-gray-500 text-center">
                    Showing first 50 rows. Total: {result.rows.length} rows.
                  </div>
                )}
              </div>
            )}

            {!result.dryRun && result.invalidRows === 0 && (
              <div className="mt-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-md">
                Upload completed successfully!
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export default BulkUpload

