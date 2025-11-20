import { useState, useEffect } from 'react'

/**
 * RuleViolationModal - Displays business rule violations.
 * 
 * Can be used in two ways:
 * 1. Event-based: Listens to 'businessRuleViolation' custom event (for global use in App.jsx)
 * 2. Prop-based: Controlled via props (ruleNumber, message, hint, onClose)
 * 
 * Usage:
 * - Global: <RuleViolationModal /> in App.jsx
 * - Controlled: <RuleViolationModal ruleNumber={...} message={...} hint={...} onClose={...} />
 */
const RuleViolationModal = ({ ruleNumber, message, hint, onClose }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [violation, setViolation] = useState(null)

  // If props are provided, use controlled mode
  const isControlled = ruleNumber !== undefined

  useEffect(() => {
    if (isControlled) {
      // Controlled mode: show if props are provided
      if (ruleNumber !== null && ruleNumber !== undefined) {
        setViolation({ ruleNumber, message, hint })
        setIsOpen(true)
      } else {
        setIsOpen(false)
        setViolation(null)
      }
    } else {
      // Event-based mode: listen for business rule violations from axios interceptor
      const handleViolation = (event) => {
        setViolation(event.detail)
        setIsOpen(true)
      }

      window.addEventListener('businessRuleViolation', handleViolation)

      return () => {
        window.removeEventListener('businessRuleViolation', handleViolation)
      }
    }
  }, [ruleNumber, message, hint, isControlled])

  const handleClose = () => {
    setIsOpen(false)
    setViolation(null)
    if (onClose) {
      onClose()
    }
  }

  const displayViolation = isControlled ? violation : violation
  if (!isOpen || !displayViolation) {
    return null
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center">
      <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-red-200 bg-red-50">
          <div className="flex items-center">
            <svg
              className="w-6 h-6 text-red-600 mr-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
            <h3 className="text-lg font-semibold text-red-900">
              Business Rule Violation
            </h3>
          </div>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <div className="mb-4">
            <div className="inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold bg-red-100 text-red-800 mb-3">
              Rule #{displayViolation.ruleNumber}
            </div>
            <p className="text-gray-800 font-medium mb-2">{displayViolation.message}</p>
            {displayViolation.hint && (
              <p className="text-sm text-gray-600 bg-blue-50 p-3 rounded-md border border-blue-200">
                <span className="font-semibold text-blue-900">💡 Hint:</span> {displayViolation.hint}
              </p>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end p-6 border-t border-gray-200 bg-gray-50">
          <button
            onClick={handleClose}
            className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
          >
            I Understand
          </button>
        </div>
      </div>
    </div>
  )
}

export default RuleViolationModal
