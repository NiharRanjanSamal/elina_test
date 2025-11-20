import { useState, useEffect } from 'react'
import api from '../services/api'

/**
 * RuleSummaryChip - Small UI component showing rule summary for pages.
 * 
 * Displays a compact chip showing rule information for a specific rule number.
 * Useful for displaying which rules apply to a particular page or operation.
 * 
 * @param {number} ruleNumber - The business rule number to display
 * @param {string} className - Additional CSS classes
 */
const RuleSummaryChip = ({ ruleNumber, className = '' }) => {
  const [rule, setRule] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchRule = async () => {
      try {
        const response = await api.get(`/api/business-rules/by-number/${ruleNumber}`)
        setRule(response.data)
      } catch (err) {
        console.error(`Failed to fetch rule ${ruleNumber}:`, err)
      } finally {
        setLoading(false)
      }
    }

    if (ruleNumber) {
      fetchRule()
    }
  }, [ruleNumber])

  if (loading) {
    return (
      <span className={`inline-flex items-center px-2 py-1 rounded text-xs bg-gray-100 text-gray-600 ${className}`}>
        Loading...
      </span>
    )
  }

  if (!rule) {
    return null
  }

  // Determine chip color based on rule status
  const chipColor = rule.activateFlag && rule.applicability === 'Y'
    ? 'bg-green-100 text-green-800 border-green-300'
    : 'bg-gray-100 text-gray-600 border-gray-300'

  return (
    <span
      className={`inline-flex items-center px-2 py-1 rounded text-xs border ${chipColor} ${className}`}
      title={rule.description || `Rule ${ruleNumber}: ${rule.controlPoint}`}
    >
      <span className="font-semibold mr-1">R{ruleNumber}</span>
      {rule.activateFlag && rule.applicability === 'Y' ? (
        <svg className="w-3 h-3 text-green-600" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
        </svg>
      ) : (
        <svg className="w-3 h-3 text-gray-500" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
        </svg>
      )}
    </span>
  )
}

export default RuleSummaryChip
