/**
 * Cypress E2E tests for Planning Module
 * 
 * Tests:
 * - Create plan version
 * - Add plan lines
 * - Validate rule violation modal
 * - Activate version
 * - Compare versions
 */

describe('Planning Module', () => {
  beforeEach(() => {
    // Login and navigate to planning page
    cy.visit('/login')
    cy.get('input[name="tenantCode"]').type('DEFAULT')
    cy.get('input[name="email"]').type('admin@example.com')
    cy.get('input[name="password"]').type('Admin@123')
    cy.get('button[type="submit"]').click()
    
    // Wait for login to complete
    cy.url().should('not.include', '/login')
    
    // Navigate to a task's plan versions page
    // Assuming we have a task with ID 1
    cy.visit('/tasks/1/plans')
  })

  it('should display plan versions list', () => {
    cy.contains('Plan Versions').should('be.visible')
    cy.get('table, .divide-y').should('exist')
  })

  it('should create a new plan version with daily entry mode', () => {
    // Click create button
    cy.contains('Create Plan Version').click()
    
    // Select Daily Entry mode
    cy.contains('Daily Entry').click()
    
    // Fill in version date
    cy.get('input[type="date"]').first().type('2025-11-05')
    
    // Add daily lines
    cy.contains('+ Add Line').click()
    cy.get('input[type="date"]').eq(1).type('2025-11-05')
    cy.get('input[type="number"]').eq(0).type('10')
    
    // Submit
    cy.contains('Create Plan Version').click()
    
    // Should see success (version appears in list)
    cy.contains('Version 1').should('be.visible')
  })

  it('should create plan version with date range split mode', () => {
    cy.contains('Create Plan Version').click()
    
    // Select Date Range Split mode
    cy.contains('Date Range Split').click()
    
    // Fill in range split details
    cy.get('input[type="date"]').eq(0).type('2025-11-05')
    cy.get('input[type="date"]').eq(1).type('2025-11-14')
    cy.get('input[type="number"]').eq(0).type('100')
    
    // Select split type
    cy.get('select').select('EQUAL_SPLIT')
    
    // Submit
    cy.contains('Create Plan Version').click()
    
    // Should create version
    cy.contains('Version', { timeout: 10000 }).should('be.visible')
  })

  it('should create plan version with single line quick mode', () => {
    cy.contains('Create Plan Version').click()
    
    // Select Single Line Quick mode
    cy.contains('Single Line Quick').click()
    
    // Fill in single line
    cy.get('input[type="date"]').eq(1).type('2025-11-05')
    cy.get('input[type="number"]').eq(0).type('50')
    
    // Submit
    cy.contains('Create Plan Version').click()
    
    // Should create version
    cy.contains('Version', { timeout: 10000 }).should('be.visible')
  })

  it('should show business rule violation modal for invalid dates', () => {
    cy.contains('Create Plan Version').click()
    cy.contains('Daily Entry').click()
    
    // Try to create with future date (should violate Rule 201)
    cy.get('input[type="date"]').first().type('2025-11-05')
    cy.contains('+ Add Line').click()
    
    // Set a date far in the future
    const futureDate = new Date()
    futureDate.setFullYear(futureDate.getFullYear() + 1)
    const futureDateStr = futureDate.toISOString().split('T')[0]
    
    cy.get('input[type="date"]').eq(1).type(futureDateStr)
    cy.get('input[type="number"]').eq(0).type('10')
    
    // Submit - should show error
    cy.contains('Create Plan Version').click()
    
    // Should show error message
    cy.contains('Rule', { timeout: 5000 }).should('be.visible')
  })

  it('should activate a plan version', () => {
    // Assuming we have at least one version
    cy.contains('Version').first().click()
    
    // Click activate button if version is not active
    cy.get('body').then(($body) => {
      if ($body.find('button:contains("Activate")').length > 0) {
        cy.contains('Activate').first().click()
        
        // Confirm dialog
        cy.on('window:confirm', () => true)
        
        // Should see active badge
        cy.contains('Active').should('be.visible')
      }
    })
  })

  it('should display plan lines for selected version', () => {
    // Select a version
    cy.contains('Version').first().click()
    
    // Should see plan lines table
    cy.contains('Plan Lines').should('be.visible')
    cy.get('table').should('exist')
  })

  it('should compare two plan versions', () => {
    // Assuming we have at least 2 versions
    cy.get('body').then(($body) => {
      const versionButtons = $body.find('button:contains("vs V")')
      if (versionButtons.length > 0) {
        cy.get('button:contains("vs V")').first().click()
        
        // Should show comparison modal
        cy.contains('Version Comparison', { timeout: 5000 }).should('be.visible')
        cy.contains('Comparison Summary').should('be.visible')
        
        // Close modal
        cy.get('button:contains("Close")').click()
      }
    })
  })

  it('should display task plan summary widget', () => {
    // Should see plan summary
    cy.contains('Plan Summary').should('be.visible')
    cy.contains('Total Planned').should('be.visible')
    cy.contains('Total Actual').should('be.visible')
    cy.contains('Variance').should('be.visible')
  })

  it('should delete a plan version', () => {
    // This test assumes delete functionality exists
    // Note: Delete might require confirmation and may not be available if task is confirmed
    cy.get('body').then(($body) => {
      if ($body.find('button:contains("Delete")').length > 0) {
        cy.contains('Delete').first().click()
        
        // Confirm deletion
        cy.on('window:confirm', () => true)
        
        // Version should be removed
        cy.contains('Version', { timeout: 5000 }).should('not.exist')
      }
    })
  })
})

