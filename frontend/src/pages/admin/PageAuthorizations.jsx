import { useState, useEffect } from 'react'
import api from '../../services/api'

const PageAuthorizations = () => {
  const [pageAuths, setPageAuths] = useState([])
  const [roles, setRoles] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editingAuth, setEditingAuth] = useState(null)
  const [formData, setFormData] = useState({
    pagePath: '',
    pageName: '',
    roleId: '',
    userId: '',
    isAllowed: true
  })

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    try {
      setLoading(true)
      const [pageAuthsRes, rolesRes, usersRes] = await Promise.all([
        api.get('/api/page-authorizations'),
        api.get('/api/roles'),
        api.get('/api/users')
      ])
      setPageAuths(pageAuthsRes.data)
      setRoles(rolesRes.data)
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

    if (!formData.roleId && !formData.userId) {
      setError('Either Role or User must be selected')
      return
    }

    try {
      const payload = {
        pagePath: formData.pagePath,
        pageName: formData.pageName,
        role: formData.roleId ? { id: parseInt(formData.roleId) } : null,
        user: formData.userId ? { id: parseInt(formData.userId) } : null,
        isAllowed: formData.isAllowed
      }

      if (editingAuth) {
        await api.put(`/api/page-authorizations/${editingAuth.id}`, payload)
      } else {
        await api.post('/api/page-authorizations', payload)
      }
      
      setShowForm(false)
      setEditingAuth(null)
      setFormData({
        pagePath: '',
        pageName: '',
        roleId: '',
        userId: '',
        isAllowed: true
      })
      fetchData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save page authorization')
    }
  }

  const handleEdit = (auth) => {
    setEditingAuth(auth)
    setFormData({
      pagePath: auth.pagePath,
      pageName: auth.pageName || '',
      roleId: auth.role?.id?.toString() || '',
      userId: auth.user?.id?.toString() || '',
      isAllowed: auth.isAllowed
    })
    setShowForm(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this page authorization?')) {
      return
    }

    try {
      await api.delete(`/api/page-authorizations/${id}`)
      fetchData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete page authorization')
    }
  }

  const handleCancel = () => {
    setShowForm(false)
    setEditingAuth(null)
    setFormData({
      pagePath: '',
      pageName: '',
      roleId: '',
      userId: '',
      isAllowed: true
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
          <h1 className="text-3xl font-bold text-gray-900">Manage Page Authorizations</h1>
          <button
            onClick={() => setShowForm(true)}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
          >
            Add Page Authorization
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
              {editingAuth ? 'Edit Page Authorization' : 'Create Page Authorization'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Page Path</label>
                  <input
                    type="text"
                    required
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    placeholder="/admin/users"
                    value={formData.pagePath}
                    onChange={(e) => setFormData({ ...formData, pagePath: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Page Name</label>
                  <input
                    type="text"
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    placeholder="Manage Users"
                    value={formData.pageName}
                    onChange={(e) => setFormData({ ...formData, pageName: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Role</label>
                  <select
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.roleId}
                    onChange={(e) => setFormData({ ...formData, roleId: e.target.value, userId: '' })}
                  >
                    <option value="">Select Role</option>
                    {roles.map((role) => (
                      <option key={role.id} value={role.id}>
                        {role.name} ({role.code})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">User</label>
                  <select
                    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2"
                    value={formData.userId}
                    onChange={(e) => setFormData({ ...formData, userId: e.target.value, roleId: '' })}
                  >
                    <option value="">Select User</option>
                    {users.map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.firstName && user.lastName
                          ? `${user.firstName} ${user.lastName}`
                          : user.email}
                      </option>
                    ))}
                  </select>
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
            {pageAuths.map((auth) => (
              <li key={auth.id}>
                <div className="px-4 py-4 sm:px-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {auth.pageName || auth.pagePath}
                        </div>
                        <div className="text-sm text-gray-500">{auth.pagePath}</div>
                        <div className="text-sm text-gray-400 mt-1">
                          {auth.role && `Role: ${auth.role.name} (${auth.role.code})`}
                          {auth.user && `User: ${auth.user.email}`}
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
          {pageAuths.length === 0 && (
            <div className="px-4 py-8 text-center text-gray-500">No page authorizations found</div>
          )}
        </div>
      </div>
    </div>
  )
}

export default PageAuthorizations

