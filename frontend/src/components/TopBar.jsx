import { useAuth } from '../context/AuthContext'
import { useNavigate, useLocation, Link } from 'react-router-dom'

const TopBar = () => {
  const { user, tenantInfo, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const isActive = (path) => {
    return location.pathname === path || location.pathname.startsWith(path + '/')
  }

  if (!user || !tenantInfo) {
    return null
  }

  return (
    <div className="bg-white shadow-md border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center space-x-6">
            <Link to="/" className="text-xl font-semibold text-gray-900 hover:text-indigo-600">
              Authorization Module
            </Link>
            
            {/* Navigation Links */}
            <nav className="hidden md:flex items-center space-x-1">
              <Link
                to="/projects"
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/projects')
                    ? 'bg-indigo-100 text-indigo-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`}
              >
                Projects
              </Link>
              <Link
                to="/admin/users"
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/admin/users')
                    ? 'bg-indigo-100 text-indigo-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`}
              >
                Users
              </Link>
              <Link
                to="/admin/roles"
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/admin/roles')
                    ? 'bg-indigo-100 text-indigo-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`}
              >
                Roles
              </Link>
              <Link
                to="/admin/business-rules"
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/admin/business-rules')
                    ? 'bg-indigo-100 text-indigo-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`}
              >
                Business Rules
              </Link>
              <Link
                to="/admin/master-data"
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/admin/master-data')
                    ? 'bg-indigo-100 text-indigo-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`}
              >
                Master Data
              </Link>
            </nav>
            
            {/* User Info - Read Only */}
            <div className="hidden lg:flex items-center space-x-4 text-sm text-gray-600">
              <div>
                <span className="font-medium">User:</span> {user.fullName || user.email}
              </div>
              <div>
                <span className="font-medium">Tenant:</span> {tenantInfo.name}
              </div>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <button
              onClick={handleLogout}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default TopBar

