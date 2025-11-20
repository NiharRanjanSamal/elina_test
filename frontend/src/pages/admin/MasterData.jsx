import { useState, useEffect } from 'react'
import api from '../../services/api'
import MasterDataEdit from '../../components/MasterDataEdit'
import BulkUpload from '../../components/BulkUpload'

const MasterData = () => {
  const [masterCodes, setMasterCodes] = useState([])
  const [codeTypes, setCodeTypes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showEditModal, setShowEditModal] = useState(false)
  const [showBulkUpload, setShowBulkUpload] = useState(false)
  const [editingCode, setEditingCode] = useState(null)
  
  // Filters
  const [selectedCodeType, setSelectedCodeType] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [activeOnly, setActiveOnly] = useState(true)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  useEffect(() => {
    fetchCodeTypes()
    fetchMasterCodes()
  }, [selectedCodeType, searchTerm, activeOnly, page, size])

  const fetchCodeTypes = async () => {
    try {
      const response = await api.get('/api/master-codes/code-types')
      setCodeTypes(response.data)
    } catch (err) {
      console.error('Failed to fetch code types:', err)
    }
  }

  const fetchMasterCodes = async () => {
    try {
      setLoading(true)
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        activeOnly: activeOnly.toString()
      })
      if (selectedCodeType) params.append('codeType', selectedCodeType)
      if (searchTerm) params.append('search', searchTerm)

      const response = await api.get(`/api/master-codes?${params.toString()}`)
      setMasterCodes(response.data.content || [])
      setTotalPages(response.data.totalPages || 0)
      setTotalElements(response.data.totalElements || 0)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch master codes')
    } finally {
      setLoading(false)
    }
  }

  const handleEdit = (code) => {
    setEditingCode(code)
    setShowEditModal(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this master code?')) {
      return
    }

    try {
      await api.delete(`/api/master-codes/${id}`)
      fetchMasterCodes()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete master code')
    }
  }

  const handleModalClose = () => {
    setShowEditModal(false)
    setEditingCode(null)
    fetchMasterCodes()
  }

  const handleBulkUploadClose = () => {
    setShowBulkUpload(false)
    fetchMasterCodes()
  }

  const handleResetFilters = () => {
    setSelectedCodeType('')
    setSearchTerm('')
    setActiveOnly(true)
    setPage(0)
  }

  if (loading && masterCodes.length === 0) {
    return (
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="text-center">Loading...</div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div className="px-4 py-6 sm:px-0">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900">Master Data Management</h1>
          <div className="flex space-x-3">
            <button
              onClick={() => setShowBulkUpload(true)}
              className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
            >
              Bulk Upload
            </button>
            <button
              onClick={() => {
                setEditingCode(null)
                setShowEditModal(true)
              }}
              className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
            >
              Add Code
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Filters */}
        <div className="mb-6 bg-white shadow rounded-lg p-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Code Type</label>
              <select
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={selectedCodeType}
                onChange={(e) => {
                  setSelectedCodeType(e.target.value)
                  setPage(0)
                }}
              >
                <option value="">All Types</option>
                {codeTypes.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Search</label>
              <input
                type="text"
                placeholder="Search code value or description..."
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value)
                  setPage(0)
                }}
              />
            </div>
            <div className="flex items-end">
              <label className="flex items-center">
                <input
                  type="checkbox"
                  className="mr-2"
                  checked={activeOnly}
                  onChange={(e) => {
                    setActiveOnly(e.target.checked)
                    setPage(0)
                  }}
                />
                <span className="text-sm font-medium text-gray-700">Active Only</span>
              </label>
            </div>
            <div className="flex items-end">
              <button
                onClick={handleResetFilters}
                className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
              >
                Reset Filters
              </button>
            </div>
          </div>
        </div>

        {/* Results count and pagination info */}
        <div className="mb-4 text-sm text-gray-600">
          Showing {masterCodes.length} of {totalElements} master codes
          {selectedCodeType && ` (filtered by: ${selectedCodeType})`}
        </div>

        {/* Table */}
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Code Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Code Value
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Short Description
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {masterCodes.map((code) => (
                <tr key={code.codeId}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {code.codeType}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {code.codeValue}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {code.shortDescription || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                        code.activateFlag
                          ? 'bg-green-100 text-green-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {code.activateFlag ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button
                      onClick={() => handleEdit(code)}
                      className="text-indigo-600 hover:text-indigo-900 mr-4"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(code.codeId)}
                      className="text-red-600 hover:text-red-900"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {masterCodes.length === 0 && (
            <div className="px-4 py-8 text-center text-gray-500">No master codes found</div>
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-700">Page size:</span>
              <select
                className="border border-gray-300 rounded-md px-2 py-1"
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value))
                  setPage(0)
                }}
              >
                <option value="10">10</option>
                <option value="20">20</option>
                <option value="50">50</option>
                <option value="100">100</option>
              </select>
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-3 py-1 border border-gray-300 rounded-md disabled:opacity-50"
              >
                Previous
              </button>
              <span className="text-sm text-gray-700">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 border border-gray-300 rounded-md disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        )}

        {/* Edit Modal */}
        {showEditModal && (
          <MasterDataEdit
            code={editingCode}
            codeTypes={codeTypes}
            onClose={handleModalClose}
          />
        )}

        {/* Bulk Upload Modal */}
        {showBulkUpload && (
          <BulkUpload onClose={handleBulkUploadClose} />
        )}
      </div>
    </div>
  )
}

export default MasterData

