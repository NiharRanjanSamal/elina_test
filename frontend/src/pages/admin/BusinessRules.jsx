import { useState, useEffect } from 'react'
import api from '../../services/api'
import BusinessRuleEdit from '../../components/BusinessRuleEdit'

const BusinessRules = () => {
  const [businessRules, setBusinessRules] = useState([])
  const [controlPoints, setControlPoints] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showEditModal, setShowEditModal] = useState(false)
  const [editingRule, setEditingRule] = useState(null)
  
  // Filters
  const [selectedControlPoint, setSelectedControlPoint] = useState('')
  const [activeOnly, setActiveOnly] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    fetchControlPoints()
    fetchBusinessRules()
  }, [selectedControlPoint, activeOnly, searchQuery])

  const fetchControlPoints = async () => {
    try {
      const response = await api.get('/api/business-rules/control-points')
      setControlPoints(response.data)
    } catch (err) {
      console.error('Failed to fetch control points:', err)
    }
  }

  const fetchBusinessRules = async () => {
    try {
      setLoading(true)
      const response = await api.get('/api/business-rules')
      let rules = response.data || []
      
      // Apply filters
      if (selectedControlPoint) {
        rules = rules.filter(r => r.controlPoint === selectedControlPoint)
      }
      if (activeOnly) {
        rules = rules.filter(r => r.activateFlag)
      }
      if (searchQuery) {
        const query = searchQuery.toLowerCase()
        rules = rules.filter(r => 
          r.ruleNumber.toString().includes(query) ||
          (r.description && r.description.toLowerCase().includes(query)) ||
          (r.controlPoint && r.controlPoint.toLowerCase().includes(query))
        )
      }
      
      setBusinessRules(rules)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch business rules')
    } finally {
      setLoading(false)
    }
  }

  const handleEdit = (rule) => {
    setEditingRule(rule)
    setShowEditModal(true)
  }

  const handleToggleActivate = async (id) => {
    try {
      await api.put(`/api/business-rules/${id}/activate-toggle`)
      fetchBusinessRules()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to toggle rule status')
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this business rule? This will disable rule validation.')) {
      return
    }

    try {
      await api.delete(`/api/business-rules/${id}`)
      fetchBusinessRules()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete business rule')
    }
  }

  const handleModalClose = () => {
    setShowEditModal(false)
    setEditingRule(null)
    fetchBusinessRules()
  }

  const handleResetFilters = () => {
    setSelectedControlPoint('')
    setActiveOnly(true)
    setSearchQuery('')
  }

  if (loading && businessRules.length === 0) {
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
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Business Rules Management</h1>
            <p className="mt-2 text-sm text-gray-600">
              Business rules are ACTIVE VALIDATORS that prevent actions violating rules.
            </p>
          </div>
          <button
            onClick={() => {
              setEditingRule(null)
              setShowEditModal(true)
            }}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            Add Rule
          </button>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Info Box */}
        <div className="mb-6 bg-blue-50 border border-blue-200 rounded-md p-4">
          <h3 className="text-sm font-semibold text-blue-900 mb-2">About Business Rules:</h3>
          <ul className="text-sm text-blue-800 list-disc list-inside space-y-1">
            <li>Rules are ACTIVE - they prevent invalid operations, not just log violations</li>
            <li>Rules apply globally across WBS, Tasks, Plan Versions, Updates, Confirmations, Allocations, Attendance, and Material Usage</li>
            <li>Set applicability to 'Y' to enable, 'N' to disable</li>
            <li>Set activate_flag to false to temporarily disable a rule</li>
          </ul>
        </div>

        {/* Filters */}
        <div className="mb-6 bg-white shadow rounded-lg p-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Search</label>
              <input
                type="text"
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                placeholder="Search by rule #, description, or control point"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Control Point</label>
              <select
                className="w-full border border-gray-300 rounded-md px-3 py-2"
                value={selectedControlPoint}
                onChange={(e) => setSelectedControlPoint(e.target.value)}
              >
                <option value="">All Control Points</option>
                {controlPoints.map((cp) => (
                  <option key={cp} value={cp}>
                    {cp}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex items-end">
              <label className="flex items-center">
                <input
                  type="checkbox"
                  className="mr-2"
                  checked={activeOnly}
                  onChange={(e) => setActiveOnly(e.target.checked)}
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

        {/* Table */}
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Rule #
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Control Point
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Applicability
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Rule Value
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Description
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
              {businessRules.map((rule) => (
                <tr key={rule.ruleId}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {rule.ruleNumber}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {rule.controlPoint}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <span className={`px-2 py-1 text-xs rounded ${
                      rule.applicability === 'Y' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                    }`}>
                      {rule.applicability}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {rule.ruleValue || '-'}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {rule.description || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                        rule.activateFlag
                          ? 'bg-green-100 text-green-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {rule.activateFlag ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button
                      onClick={() => handleEdit(rule)}
                      className="text-indigo-600 hover:text-indigo-900 mr-4"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleToggleActivate(rule.ruleId)}
                      className={`mr-4 ${
                        rule.activateFlag
                          ? 'text-yellow-600 hover:text-yellow-900'
                          : 'text-green-600 hover:text-green-900'
                      }`}
                      title={rule.activateFlag ? 'Deactivate' : 'Activate'}
                    >
                      {rule.activateFlag ? 'Deactivate' : 'Activate'}
                    </button>
                    <button
                      onClick={() => handleDelete(rule.ruleId)}
                      className="text-red-600 hover:text-red-900"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {businessRules.length === 0 && (
            <div className="px-4 py-8 text-center text-gray-500">No business rules found</div>
          )}
        </div>

        {/* Edit Modal */}
        {showEditModal && (
          <BusinessRuleEdit
            rule={editingRule}
            controlPoints={controlPoints}
            onClose={handleModalClose}
          />
        )}
      </div>
    </div>
  )
}

export default BusinessRules

