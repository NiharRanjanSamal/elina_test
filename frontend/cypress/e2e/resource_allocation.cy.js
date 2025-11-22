describe('Resource Allocation Module', () => {
  const baseWbs = {
    wbsId: 1,
    wbsCode: 'WBS-2.1',
    wbsName: 'Structural Works',
    startDate: '2025-01-01',
    endDate: '2025-03-31'
  }

  const manpower = [
    {
      allocationId: 11,
      wbsId: 1,
      employeeId: 101,
      employeeName: 'Ram Kumar',
      skillLevel: 'Mason',
      startDate: '2025-01-05',
      endDate: '2025-01-14',
      ratePerDay: 800,
      totalCost: 8000
    }
  ]

  const equipment = [
    {
      allocationId: 22,
      wbsId: 1,
      equipmentId: 301,
      equipmentName: 'JCB Excavator',
      equipmentType: 'Earth Moving',
      startDate: '2025-01-10',
      endDate: '2025-01-14',
      ratePerDay: 3500,
      totalCost: 17500
    }
  ]

  beforeEach(() => {
    cy.intercept('GET', '/api/wbs/1', { statusCode: 200, body: baseWbs }).as('getWbs')
    cy.intercept('GET', '/api/resources/manpower/wbs/1', { statusCode: 200, body: manpower }).as(
      'getManpower'
    )
    cy.intercept('GET', '/api/resources/equipment/wbs/1', { statusCode: 200, body: equipment }).as(
      'getEquipment'
    )
    cy.intercept('GET', '/api/resources/cost/wbs/1', {
      statusCode: 200,
      body: { wbsId: 1, manpowerCost: 8000, equipmentCost: 17500, totalCost: 25500 }
    }).as('getCost')
    cy.intercept('GET', '/api/resources/timeline/wbs/1', {
      statusCode: 200,
      body: [
        { resourceName: 'Ram Kumar', resourceType: 'MANPOWER', startDate: '2025-01-05', endDate: '2025-01-14', durationDays: 10 },
        { resourceName: 'JCB Excavator', resourceType: 'EQUIPMENT', startDate: '2025-01-10', endDate: '2025-01-14', durationDays: 5 }
      ]
    }).as('getTimeline')
    cy.intercept('GET', '/api/resources/manpower/options*', {
      statusCode: 200,
      body: [
        { id: 101, name: 'Ram Kumar', ratePerDay: 800, metadata: 'Mason' },
        { id: 102, name: 'Suresh Das', ratePerDay: 950, metadata: 'Electrician' }
      ]
    }).as('getEmployeeOptions')
    cy.intercept('GET', '/api/resources/equipment/options*', {
      statusCode: 200,
      body: [{ id: 301, name: 'JCB Excavator', ratePerDay: 3500, metadata: 'Earth Moving' }]
    }).as('getEquipmentOptions')

    cy.visit('/wbs/1/resources', {
      onBeforeLoad(win) {
        win.localStorage.setItem('token', 'fake-token')
        win.localStorage.setItem('refreshToken', 'fake-refresh')
        win.localStorage.setItem('user', JSON.stringify({ userId: 1, name: 'Test User' }))
        win.localStorage.setItem('tenantInfo', JSON.stringify({ tenantId: 1, tenantCode: 'DEFAULT' }))
      }
    })

    cy.wait(['@getWbs', '@getManpower', '@getEquipment', '@getCost'])
  })

  it('shows allocations and summary totals', () => {
    cy.contains('Resource Allocation').should('be.visible')
    cy.contains('Ram Kumar').should('be.visible')
    cy.contains('JCB Excavator').should('be.visible')
    cy.contains('₹25,500').should('be.visible')
  })

  it('creates a manpower allocation with cost preview', () => {
    cy.intercept('GET', '/api/resources/cost/manpower/*', {
      statusCode: 200,
      body: { totalDays: 5, ratePerDay: 800, totalCost: 4000 }
    }).as('previewCost')

    const updatedManpower = [
      ...manpower,
      {
        allocationId: 33,
        wbsId: 1,
        employeeId: 102,
        employeeName: 'Suresh Das',
        skillLevel: 'Electrician',
        startDate: '2025-02-01',
        endDate: '2025-02-05',
        ratePerDay: 950,
        totalCost: 4750
      }
    ]

    cy.intercept('POST', '/api/resources/manpower', {
      statusCode: 201,
      body: updatedManpower[1]
    }).as('createManpower')

    cy.intercept('GET', '/api/resources/manpower/wbs/1', {
      statusCode: 200,
      body: updatedManpower
    }).as('refreshManpower')

    cy.contains('Allocate Resource').click()
    cy.get('select[name="resourceId"]').select('102')
    cy.get('input[name="startDate"]').type('2025-02-01')
    cy.get('input[name="endDate"]').type('2025-02-05')
    cy.get('input[name="hoursPerDay"]').type('8')
    cy.wait('@previewCost')
    cy.contains('₹4,000').should('be.visible')
    cy.contains('Save Allocation').click()

    cy.wait(['@createManpower', '@refreshManpower'])
    cy.contains('Suresh Das').should('be.visible')
  })
})

