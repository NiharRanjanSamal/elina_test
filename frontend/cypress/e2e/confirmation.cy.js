/**
 * Cypress tests for Confirmation Module
 *
 * Scenarios:
 * - Load confirmation summary + history
 * - Trigger preview modal
 * - Attempt invalid (future) confirmation to surface rule violation modal
 * - Undo (if button available)
 */

describe('Confirmation Module', () => {
  beforeEach(() => {
    cy.visit('/login')
    cy.get('input[name="tenantCode"]').type('DEFAULT')
    cy.get('input[name="email"]').type('admin@example.com')
    cy.get('input[name="password"]').type('Admin@123')
    cy.get('button[type="submit"]').click()
    cy.url().should('not.include', '/login')

    cy.visit('/wbs/1/confirmations')
  })

  it('should display summary cards and lock banner', () => {
    cy.contains('Confirmations').should('be.visible')
    cy.contains('Plan vs Actual vs Confirmed').should('be.visible')
    cy.contains('Confirmation History').should('be.visible')
  })

  it('should open preview modal before confirming', () => {
    cy.get('input[type="date"]').first().invoke('val').then((value) => {
      if (!value) {
        cy.get('input[type="date"]').first().type('2025-11-10')
      }
    })
    cy.contains('Confirm Now').click()
    cy.contains('Confirm Freeze').should('be.visible')
    cy.contains('Cancel').click()
  })

  it('should show rule violation modal for future date', () => {
    const futureDate = new Date()
    futureDate.setFullYear(futureDate.getFullYear() + 2)
    const future = futureDate.toISOString().split('T')[0]

    cy.get('input[type="date"]').first().clear().type(future)
    cy.contains('Confirm Now').click()
    cy.contains('Confirm Freeze').should('be.visible')
    cy.contains('Confirm').click()

    cy.contains('Rule', { timeout: 5000 }).should('be.visible')
  })

  it('should attempt undo when permitted', () => {
    cy.get('body').then(($body) => {
      if ($body.find('button:contains("Undo")').length > 0) {
        cy.contains('Undo').first().click()
        cy.on('window:confirm', () => true)
      }
    })
  })
})


