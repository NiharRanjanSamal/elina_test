import { createContext, useContext, useState, useEffect } from 'react'
import api from '../services/api'

const AuthContext = createContext(null)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [tenantInfo, setTenantInfo] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [refreshToken, setRefreshToken] = useState(localStorage.getItem('refreshToken'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Check if we have a stored token and try to restore session
    const storedUser = localStorage.getItem('user')
    const storedTenantInfo = localStorage.getItem('tenantInfo')

    if (token && storedUser && storedTenantInfo) {
      try {
        setUser(JSON.parse(storedUser))
        setTenantInfo(JSON.parse(storedTenantInfo))
        api.setAuthToken(token)
      } catch (error) {
        console.error('Error parsing stored auth data:', error)
        logout()
      }
    }
    setLoading(false)
  }, [token])

  const login = async (tenantCode, email, password) => {
    try {
      const response = await api.post('/api/auth/login', {
        tenantCode,
        email,
        password
      })

      const { token: newToken, refreshToken: newRefreshToken, userProfile, tenantInfo: tenant } = response.data

      setToken(newToken)
      setRefreshToken(newRefreshToken)
      setUser(userProfile)
      setTenantInfo(tenant)

      // Store in localStorage
      localStorage.setItem('token', newToken)
      localStorage.setItem('refreshToken', newRefreshToken)
      localStorage.setItem('user', JSON.stringify(userProfile))
      localStorage.setItem('tenantInfo', JSON.stringify(tenant))

      // Set auth token in API service
      api.setAuthToken(newToken)

      return { success: true }
    } catch (error) {
      // Log full error for debugging
      console.error('Login error:', error.response?.data || error.message)
      
      // Handle validation errors (field-specific) or runtime exceptions (message field)
      let errorMessage = 'Login failed'
      if (error.response?.data) {
        // Check for validation errors (field-specific messages)
        const errorData = error.response.data
        if (errorData.message) {
          errorMessage = errorData.message
        } else {
          // Extract first validation error
          const fieldErrors = Object.values(errorData)
          if (fieldErrors.length > 0) {
            errorMessage = fieldErrors[0]
          } else {
            errorMessage = 'Invalid request. Please check your input.'
          }
        }
      } else if (error.message) {
        errorMessage = error.message
      }
      
      return {
        success: false,
        error: errorMessage
      }
    }
  }

  const logout = () => {
    setToken(null)
    setRefreshToken(null)
    setUser(null)
    setTenantInfo(null)

    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    localStorage.removeItem('tenantInfo')

    api.setAuthToken(null)
  }

  const refreshAuthToken = async () => {
    try {
      const currentRefreshToken = localStorage.getItem('refreshToken')
      if (!currentRefreshToken) {
        throw new Error('No refresh token available')
      }

      const response = await api.post('/api/auth/refresh', {
        refreshToken: currentRefreshToken
      })

      const { token: newToken, refreshToken: newRefreshToken, userProfile, tenantInfo: tenant } = response.data

      setToken(newToken)
      setRefreshToken(newRefreshToken)
      setUser(userProfile)
      setTenantInfo(tenant)

      localStorage.setItem('token', newToken)
      localStorage.setItem('refreshToken', newRefreshToken)
      localStorage.setItem('user', JSON.stringify(userProfile))
      localStorage.setItem('tenantInfo', JSON.stringify(tenant))

      api.setAuthToken(newToken)

      return { success: true }
    } catch (error) {
      logout()
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Token refresh failed'
      }
    }
  }

  const value = {
    user,
    tenantInfo,
    token,
    isAuthenticated: !!token && !!user,
    login,
    logout,
    refreshAuthToken,
    loading
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

