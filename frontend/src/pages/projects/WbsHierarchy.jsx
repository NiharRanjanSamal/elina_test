import { useState, useEffect } from 'react'
import api from '../../services/api'
import { useParams, useNavigate } from 'react-router-dom'

const WbsHierarchy = () => {
  const { projectId } = useParams()
  const navigate = useNavigate()
  const [project, setProject] = useState(null)
  const [wbsList, setWbsList] = useState([])
  const [hierarchy, setHierarchy] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editingWbs, setEditingWbs] = useState(null)
  const [parentWbsId, setParentWbsId] = useState(null)
  const [expandedNodes, setExpandedNodes] = useState(new Set())
  const [formData, setFormData] = useState({
    wbsCode: '',
    wbsName: '',
    description: '',
    startDate: '',
    endDate: '',
    parentWbsId: null
  })
  const [showConfirmModal, setShowConfirmModal] = useState(false)
  const [confirmingEntity, setConfirmingEntity] = useState(null)
  const [confirmFormData, setConfirmFormData] = useState({
    confirmationDate: new Date().toISOString().split('T')[0],
    remarks: ''
  })

  useEffect(() => {
    if (projectId) {
      fetchProject()
      fetchWbsHierarchy()
    }
  }, [projectId])

  const fetchProject = async () => {
    try {
      const response = await api.get(`/api/projects/${projectId}`)
      setProject(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch project')
    }
  }

  const fetchWbsHierarchy = async () => {
    try {
      setLoading(true)
      const response = await api.get(`/api/wbs/project/${projectId}/hierarchy`)
      const wbsData = response.data || []
      setWbsList(wbsData)
      
      // Build hierarchy tree
      const rootNodes = wbsData.filter(w => !w.parentWbsId)
      const buildTree = (parentId) => {
        return wbsData
          .filter(w => w.parentWbsId === parentId)
          .map(w => ({
            ...w,
            children: buildTree(w.wbsId)
          }))
      }
      
      const tree = rootNodes.map(node => ({
        ...node,
        children: buildTree(node.wbsId)
      }))
      
      setHierarchy(tree)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch WBS hierarchy')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    // Validation
    if (!formData.wbsCode.trim()) {
      setError('WBS code is required')
      return
    }
    if (!formData.wbsName.trim()) {
      setError('WBS name is required')
      return
    }
    if (formData.startDate && formData.endDate && formData.startDate > formData.endDate) {
      setError('Start date must be before end date')
      return
    }

    try {
      const payload = {
        ...formData,
        projectId: parseInt(projectId),
        parentWbsId: formData.parentWbsId || null,
        startDate: formData.startDate || null,
        endDate: formData.endDate || null
      }

      if (editingWbs) {
        await api.put(`/api/wbs/${editingWbs.wbsId}`, payload)
      } else {
        await api.post('/api/wbs', payload)
      }
      
      setShowForm(false)
      setEditingWbs(null)
      setParentWbsId(null)
      setFormData({
        wbsCode: '',
        wbsName: '',
        description: '',
        startDate: '',
        endDate: '',
        parentWbsId: null
      })
      fetchWbsHierarchy()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save WBS')
    }
  }

  const handleEdit = (wbs, parentId = null) => {
    setEditingWbs(wbs)
    setParentWbsId(parentId)
    setFormData({
      wbsCode: wbs.wbsCode || '',
      wbsName: wbs.wbsName || '',
      description: wbs.description || '',
      startDate: wbs.startDate ? wbs.startDate.split('T')[0] : '',
      endDate: wbs.endDate ? wbs.endDate.split('T')[0] : '',
      parentWbsId: wbs.parentWbsId || null
    })
    setShowForm(true)
  }

  const handleAddChild = (parentWbsId) => {
    setEditingWbs(null)
    setParentWbsId(parentWbsId)
    setFormData({
      wbsCode: '',
      wbsName: '',
      description: '',
      startDate: '',
      endDate: '',
      parentWbsId: parentWbsId
    })
    setShowForm(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this WBS? This will also delete child WBS and associated Tasks.')) {
      return
    }

    try {
      await api.delete(`/api/wbs/${id}`)
      fetchWbsHierarchy()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete WBS')
    }
  }

  const handleConfirm = (wbs) => {
    setConfirmingEntity({ type: 'WBS', id: wbs.wbsId, name: wbs.wbsName })
    setConfirmFormData({
      confirmationDate: new Date().toISOString().split('T')[0],
      remarks: ''
    })
    setShowConfirmModal(true)
  }

  const handleConfirmSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!confirmFormData.confirmationDate) {
      setError('Confirmation date is required')
      return
    }

    try {
      const payload = {
        entityType: confirmingEntity.type,
        entityId: confirmingEntity.id,
        confirmationDate: confirmFormData.confirmationDate,
        remarks: confirmFormData.remarks || null
      }

      await api.post('/api/confirmations', payload)
      setShowConfirmModal(false)
      setConfirmingEntity(null)
      setConfirmFormData({
        confirmationDate: new Date().toISOString().split('T')[0],
        remarks: ''
      })
      fetchWbsHierarchy()
      fetchProject() // Refresh project data to show updated confirmation status
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to confirm WBS')
    }
  }

  const toggleExpand = (wbsId) => {
    const newExpanded = new Set(expandedNodes)
    if (newExpanded.has(wbsId)) {
      newExpanded.delete(wbsId)
    } else {
      newExpanded.add(wbsId)
    }
    setExpandedNodes(newExpanded)
  }

  const formatDate = (dateString) => {
    if (!dateString) return '-'
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString()
    } catch {
      return dateString
    }
  }

  const getAllWbsOptions = () => {
    const flatten = (nodes, level = 0) => {
      let result = []
      nodes.forEach(node => {
        result.push({ ...node, level })
        if (node.children && node.children.length > 0) {
          result = result.concat(flatten(node.children, level + 1))
        }
      })
      return result
    }
    return flatten(hierarchy)
  }

  const renderTreeNode = (node, level = 0) => {
    const hasChildren = node.children && node.children.length > 0
    const isExpanded = expandedNodes.has(node.wbsId)
    const indent = level * 24

    return (
      <div key={node.wbsId} className="border-b border-gray-200">
        <div className="flex items-center py-3 hover:bg-gray-50" style={{ paddingLeft: `${indent + 16}px` }}>
          <div className="flex items-center flex-1">
            {hasChildren && (
              <button
                onClick={() => toggleExpand(node.wbsId)}
                className="mr-2 text-gray-500 hover:text-gray-700"
              >
                {isExpanded ? (
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                ) : (
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                )}
              </button>
            )}
            {!hasChildren && <div className="w-6 mr-2" />}
            
            <div className="flex-1">
              <div className="font-medium text-gray-900">
                {node.wbsCode} - {node.wbsName}
              </div>
              {node.description && (
                <div className="text-xs text-gray-500 mt-1">{node.description}</div>
              )}
              <div className="text-xs text-gray-400 mt-1">
                Level {node.level} | {formatDate(node.startDate)} - {formatDate(node.endDate)}
              </div>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <span className="text-xs text-gray-500">
              Planned: {node.plannedQty || 0} | Confirmed: {node.confirmedQty || 0}
              {node.isConfirmed && (
                <span className="ml-2 px-2 py-0.5 bg-green-100 text-green-700 rounded text-xs">Confirmed</span>
              )}
            </span>
            <button
              onClick={() => navigate(`/wbs/${node.wbsId}/tasks`)}
              className="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
              title="View Tasks"
            >
              Tasks
            </button>
            {!node.isConfirmed && (
              <button
                onClick={() => handleConfirm(node)}
                className="text-xs px-2 py-1 bg-yellow-100 text-yellow-700 rounded hover:bg-yellow-200"
                title="Confirm WBS"
              >
                Confirm
              </button>
            )}
            <button
              onClick={() => handleAddChild(node.wbsId)}
              className="text-xs px-2 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200"
              title="Add Child"
            >
              + Child
            </button>
            <button
              onClick={() => handleEdit(node, node.parentWbsId)}
              className="text-xs px-2 py-1 bg-indigo-100 text-indigo-700 rounded hover:bg-indigo-200"
            >
              Edit
            </button>
            <button
              onClick={() => handleDelete(node.wbsId)}
              className="text-xs px-2 py-1 bg-red-100 text-red-700 rounded hover:bg-red-200"
            >
              Delete
            </button>
          </div>
        </div>

        {hasChildren && isExpanded && (
          <div>
            {node.children.map(child => renderTreeNode(child, level + 1))}
          </div>
        )}
      </div>
    )
  }

  if (loading && hierarchy.length === 0) {
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
            <button
              onClick={() => navigate('/projects')}
              className="text-sm text-indigo-600 hover:text-indigo-900 mb-2"
            >
              ← Back to Projects
            </button>
            <h1 className="text-3xl font-bold text-gray-900">
              WBS Hierarchy {project && `- ${project.projectName}`}
            </h1>
            <p className="mt-2 text-sm text-gray-600">
              Manage Work Breakdown Structure hierarchy
            </p>
          </div>
          <button
            onClick={() => {
              setEditingWbs(null)
              setParentWbsId(null)
              setFormData({
                wbsCode: '',
                wbsName: '',
                description: '',
                startDate: '',
                endDate: '',
                parentWbsId: null
              })
              setShowForm(true)
            }}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            Add Root WBS
          </button>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Hierarchy Tree */}
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          {hierarchy.length === 0 ? (
            <div className="px-4 py-8 text-center text-gray-500">
              No WBS found. Create a root WBS to get started.
            </div>
          ) : (
            hierarchy.map(node => renderTreeNode(node))
          )}
        </div>

        {/* Form Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-900">
                  {editingWbs ? 'Edit WBS' : 'Create WBS'}
                </h2>
                <button
                  onClick={() => {
                    setShowForm(false)
                    setEditingWbs(null)
                    setParentWbsId(null)
                  }}
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

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    WBS Code <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.wbsCode}
                    onChange={(e) => setFormData({ ...formData, wbsCode: e.target.value.toUpperCase() })}
                    placeholder="e.g., WBS001"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    WBS Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.wbsName}
                    onChange={(e) => setFormData({ ...formData, wbsName: e.target.value })}
                    placeholder="Enter WBS name"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Description
                  </label>
                  <textarea
                    rows={3}
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Enter WBS description"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Parent WBS
                  </label>
                  <select
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.parentWbsId || parentWbsId || ''}
                    onChange={(e) => setFormData({ ...formData, parentWbsId: e.target.value ? parseInt(e.target.value) : null })}
                  >
                    <option value="">None (Root WBS)</option>
                    {getAllWbsOptions()
                      .filter(w => !editingWbs || w.wbsId !== editingWbs.wbsId)
                      .map(w => (
                        <option key={w.wbsId} value={w.wbsId}>
                          {'  '.repeat(w.level)}{w.wbsCode} - {w.wbsName}
                        </option>
                      ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Start Date
                    </label>
                    <input
                      type="date"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.startDate}
                      onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      End Date
                    </label>
                    <input
                      type="date"
                      className="w-full border border-gray-300 rounded-md px-3 py-2"
                      value={formData.endDate}
                      onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                    />
                  </div>
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowForm(false)
                      setEditingWbs(null)
                      setParentWbsId(null)
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
                  >
                    {editingWbs ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Confirmation Modal */}
        {showConfirmModal && confirmingEntity && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-900">
                  Confirm {confirmingEntity.type}
                </h2>
                <button
                  onClick={() => {
                    setShowConfirmModal(false)
                    setConfirmingEntity(null)
                  }}
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

              <div className="mb-4">
                <p className="text-sm text-gray-600">
                  <span className="font-medium">{confirmingEntity.type}:</span> {confirmingEntity.name}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  Note: Business rules may prevent confirmation if the entity is already confirmed.
                </p>
              </div>

              <form onSubmit={handleConfirmSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Confirmation Date <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    required
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={confirmFormData.confirmationDate}
                    onChange={(e) => setConfirmFormData({ ...confirmFormData, confirmationDate: e.target.value })}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Remarks
                  </label>
                  <textarea
                    rows={3}
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    value={confirmFormData.remarks}
                    onChange={(e) => setConfirmFormData({ ...confirmFormData, remarks: e.target.value })}
                    placeholder="Enter remarks (optional)"
                  />
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowConfirmModal(false)
                      setConfirmingEntity(null)
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-yellow-600 text-white rounded-md hover:bg-yellow-700"
                  >
                    Confirm
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default WbsHierarchy

