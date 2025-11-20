import { useState, useEffect } from 'react'
import api from '../../services/api'

const UserAuthorizations = () => {
  const [userAuths, setUserAuths] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editingAuth, setEditingAuth] = useState(null)
  const [formData, setFormData] = useState({
    userId: '',
    resourceType: '',
    resourceId: '',
    permissionType: 'READ',
    isAllowed: true,
    conditions: ''
  })

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    try {
      setLoading(true)
      const [userAuthsRes, usersRes] = await Promise.all([
        api.get('/api/user-authorizations'),
        api.get('/api/users')
      ])
      setUserAuths(userAuthsRes.data)
      setUsers(usersRes.data)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch data')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!formData.userId) {
      setError('User must be selected')
      return
    }

    if (!formData.resourceType) {
      setError('Resource Type is required')
      return
    }

    try {
      const payload = {
        user: { id: parseInt(formData.userId) },
        resourceType: formData.resourceType,
        resourceId: formData.resourceId || null,
        permissionType: formData.permissionType,
        isAllowed: formData.isAllowed,
        conditions: formData.conditions || null
      }

      if (editingAuth) {
        await api.put(`/api/user-authorizations/${editingAuth.id}`, payload)
      } else {
        await api.post('/api/user-authorizations', payload)
      }
      
      setShowForm(false)
      setEditingAuth(null)
      setFormData({
        userId: '',
        resourceType: '',
        resourceId: '',
        permissionType: 'READ',
        isAllowed: true,
        conditions: ''
      })
      fetchData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save user authorization')
    }
  }

  const handleEdit = (auth) => {
    setEditingAuth(auth)
    setFormData({
      userId: auth.user?.id?.toString() || '',
      resourceType: auth.resourceType || '',
      resourceId: auth.resourceId || '',
      permissionType: auth.permissionType || 'READ',
      isAllowed: auth.isAllowed !== undefined ? auth.isAllowed : true,
      conditions: auth.conditions || ''
    })
    setShowForm(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this user authorization?')) {
      return
    }

    try {
      await api.delete(`/api/user-authorizations/${id}`)
      fetchData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete user authorization')
    }
  }

  const handleCancel = () => {
    setShowForm(false)
    setEditingAuth(null)
    setFormData({
      userId: '',
      resourceType: '',
      resourceId: '',
      permissionType: 'READ',
      isAllowed: true,
      conditions: ''
    })
  }

  if (loading) {
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
          <h1 className="text-3xl font-bold text-gray-900">Manage User Authorizations</h1>
          <button
            onClick={() => setShowForm(true)}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            Add User Authorization
          </button>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {showForm && (
          <div className="mb-6 bg-white shadow rounded-lg p-6">
            <h2 className="text-xl font-semibold mb-4">
              {editingAuth ? 'Edit User Authorization' : 'Create User Authorization'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">User *</label>
                  <select
                    required
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.userId}
                    onChange={(e) => setFormData({ ...formData, userId: e.target.value })}
                  >
                    <option value="">Select User</option>
                    {users.map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.firstName && user.lastName
                          ? `${user.firstName} ${user.lastName}`
                          : user.email} ({user.email})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Resource Type *</label>
                  <input
                    type="text"
                    required
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    placeholder="e.g., DOCUMENT, REPORT, DASHBOARD"
                    value={formData.resourceType}
                    onChange={(e) => setFormData({ ...formData, resourceType: e.target.value.toUpperCase() })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Resource ID</label>
                  <input
                    type="text"
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    placeholder="Optional: specific resource ID"
                    value={formData.resourceId}
                    onChange={(e) => setFormData({ ...formData, resourceId: e.target.value })}
                  />
                  <p className="mt-1 text-xs text-gray-500">Leave empty for all resources of this type</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Permission Type</label>
                  <select
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.permissionType}
                    onChange={(e) => setFormData({ ...formData, permissionType: e.target.value })}
                  >
                    <option value="READ">READ</option>
                    <option value="WRITE">WRITE</option>
                    <option value="DELETE">DELETE</option>
                    <option value="FULL">FULL</option>
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700">Conditions (JSON)</label>
                  <textarea
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    rows="3"
                    placeholder='Optional: JSON conditions, e.g., {"status": "active"}'
                    value={formData.conditions}
                    onChange={(e) => setFormData({ ...formData, conditions: e.target.value })}
                  />
                  <p className="mt-1 text-xs text-gray-500">Optional: JSON conditions for fine-grained access control</p>
                </div>
                <div>
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      className="mr-2"
                      checked={formData.isAllowed}
                      onChange={(e) => setFormData({ ...formData, isAllowed: e.target.checked })}
                    />
                    <span className="text-sm font-medium text-gray-700">Allowed</span>
                  </label>
                </div>
              </div>
              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={handleCancel}
                  className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
                >
                  {editingAuth ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <ul className="divide-y divide-gray-200">
            {userAuths.map((auth) => (
              <li key={auth.id}>
                <div className="px-4 py-4 sm:px-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {auth.user?.firstName && auth.user?.lastName
                            ? `${auth.user.firstName} ${auth.user.lastName}`
                            : auth.user?.email || 'Unknown User'}
                        </div>
                        <div className="text-sm text-gray-500">
                          Resource: {auth.resourceType}
                          {auth.resourceId && ` (ID: ${auth.resourceId})`}
                        </div>
                        <div className="text-sm text-gray-400 mt-1">
                          Permission: {auth.permissionType}
                          {auth.conditions && ` | Conditions: ${auth.conditions}`}
                        </div>
                      </div>
                      <div className="ml-4">
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                            auth.isAllowed
                              ? 'bg-green-100 text-green-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {auth.isAllowed ? 'Allowed' : 'Denied'}
                        </span>
                      </div>
                    </div>
                    <div className="flex space-x-2">
                      <button
                        onClick={() => handleEdit(auth)}
                        className="text-indigo-600 hover:text-indigo-900 text-sm font-medium"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(auth.id)}
                        className="text-red-600 hover:text-red-900 text-sm font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
          {userAuths.length === 0 && (
            <div className="px-4 py-8 text-center text-gray-500">No user authorizations found</div>
          )}
        </div>
      </div>
    </div>
  )
}

export default UserAuthorizations

