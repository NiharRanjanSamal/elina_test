import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor to handle token refresh and business rule violations
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Handle Business Rule Violations (400 with type BUSINESS_RULE_VIOLATION)
    if (error.response?.status === 400 && error.response?.data?.type === 'BUSINESS_RULE_VIOLATION') {
      // Dispatch custom event for RuleViolationModal to handle
      const event = new CustomEvent('businessRuleViolation', {
        detail: {
          ruleNumber: error.response.data.ruleNumber,
          message: error.response.data.message,
          hint: error.response.data.hint
        }
      })
      window.dispatchEvent(event)
      return Promise.reject(error)
    }

    // Handle 403 Forbidden (Access Denied)
    if (error.response?.status === 403) {
      // Enhance error message for better user feedback
      if (!error.response.data) {
        error.response.data = {}
      }
      if (!error.response.data.message) {
        error.response.data.message = 'Access denied. You do not have permission to perform this action.'
      }
      return Promise.reject(error)
    }

    // If error is 401 and we haven't already retried
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) {
          throw new Error('No refresh token available')
        }

        const response = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
          refreshToken
        })

        const { token: newToken, refreshToken: newRefreshToken } = response.data

        localStorage.setItem('token', newToken)
        localStorage.setItem('refreshToken', newRefreshToken)

        // Update the original request with new token
        originalRequest.headers.Authorization = `Bearer ${newToken}`

        // Retry the original request
        return api(originalRequest)
      } catch (refreshError) {
        // Refresh failed, redirect to login
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        localStorage.removeItem('tenantInfo')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)

// Method to manually set auth token
api.setAuthToken = (token) => {
  if (token) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete api.defaults.headers.common['Authorization']
  }
}

export default api

