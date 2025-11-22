package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.WbsConfirmationRequest;
import com.elina.projects.dto.WbsConfirmationSummaryDTO;
import com.elina.projects.entity.ConfirmationEntity;
import com.elina.projects.entity.ConfirmationLockEntity;
import com.elina.projects.entity.Wbs;
import com.elina.projects.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConfirmationServiceTest {

    @Mock
    private ConfirmationRepository confirmationRepository;

    @Mock
    private ConfirmationLockRepository confirmationLockRepository;

    @Mock
    private WbsRepository wbsRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskUpdateRepository taskUpdateRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private BusinessRuleEngine businessRuleEngine;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ConfirmationService confirmationService;

    private Tenant tenant;
    private Wbs wbs;

    @BeforeEach
    void init() {
        TenantContext.setTenantId(1L);

        tenant = new Tenant();
        tenant.setId(1L);

        wbs = new Wbs();
        wbs.setWbsId(10L);
        wbs.setTenant(tenant);
        wbs.setWbsCode("1.0");
        wbs.setWbsName("Site Mobilisation");
        wbs.setStartDate(LocalDate.of(2025, 1, 1));
        wbs.setPlannedQty(new BigDecimal("100.00"));
    }

    @Test
    void confirmWbs_ShouldPersistConfirmationAndUpdateLock() {
        WbsConfirmationRequest request = new WbsConfirmationRequest();
        request.setConfirmationDate(LocalDate.of(2025, 11, 10));
        request.setRemarks("Freeze progress");

        wbs.setPlannedQty(null);
        wbs.setActualQty(null);
        when(wbsRepository.findById(wbs.getWbsId())).thenReturn(Optional.of(wbs));
        when(taskRepository.findEarliestTaskStartDateForWbs(wbs.getWbsId())).thenReturn(wbs.getStartDate());
        when(taskUpdateRepository.sumActualQtyForWbsAndDate(wbs.getWbsId(), request.getConfirmationDate()))
            .thenReturn(new BigDecimal("25.00"));
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(confirmationLockRepository.findByWbsId(wbs.getWbsId())).thenReturn(Optional.empty());
        ConfirmationEntity saved = new ConfirmationEntity();
        saved.setConfirmationId(99L);
        saved.setEntityType("WBS");
        saved.setEntityId(wbs.getWbsId());
        saved.setTenant(tenant);
        saved.setConfirmationDate(request.getConfirmationDate());
        saved.setConfirmedBy(1L);
        saved.setConfirmedOn(java.time.LocalDateTime.now());
        when(confirmationRepository.save(any(ConfirmationEntity.class))).thenReturn(saved);
        when(confirmationLockRepository.save(any(ConfirmationLockEntity.class)))
            .thenAnswer(invocation -> {
                ConfirmationLockEntity entity = invocation.getArgument(0);
                if (entity.getLockId() == null) {
                    entity.setLockId(1L);
                }
                return entity;
            });

        when(confirmationRepository.findLatestConfirmationDateForWbs(wbs.getWbsId())).thenReturn(request.getConfirmationDate());
        when(confirmationRepository.sumConfirmedQtyByWbs(wbs.getWbsId())).thenReturn(new BigDecimal("25.00"));
        when(confirmationLockRepository.findLockDateByWbsId(wbs.getWbsId())).thenReturn(request.getConfirmationDate());
        when(taskRepository.sumPlannedQtyByWbsId(wbs.getWbsId())).thenReturn(new BigDecimal("100.00"));
        when(taskRepository.sumActualQtyByWbsId(wbs.getWbsId())).thenReturn(new BigDecimal("40.00"));

        WbsConfirmationSummaryDTO summary = confirmationService.confirmWbs(wbs.getWbsId(), request);

        assertNotNull(summary);
        assertEquals(request.getConfirmationDate(), summary.getLastConfirmationDate());
        verify(confirmationRepository, times(1)).save(any(ConfirmationEntity.class));
        verify(confirmationLockRepository, times(1)).save(any(ConfirmationLockEntity.class));
        verify(auditLogService, atLeastOnce()).writeAuditLog(anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void confirmWbs_WhenRuleFails_ShouldPropagateException() {
        WbsConfirmationRequest request = new WbsConfirmationRequest();
        request.setConfirmationDate(LocalDate.of(2025, 11, 10));

        when(wbsRepository.findById(wbs.getWbsId())).thenReturn(Optional.of(wbs));
        when(taskRepository.findEarliestTaskStartDateForWbs(wbs.getWbsId())).thenReturn(wbs.getStartDate());
        when(taskUpdateRepository.sumActualQtyForWbsAndDate(wbs.getWbsId(), request.getConfirmationDate()))
            .thenReturn(new BigDecimal("0"));

        doThrow(new BusinessRuleException(602, "Missing data"))
            .when(businessRuleEngine).validateAll(anyList(), any());

        assertThrows(BusinessRuleException.class, () -> confirmationService.confirmWbs(wbs.getWbsId(), request));
    }

    @Test
    void undoConfirmation_ShouldDeleteLockWhenNoRemainingRecords() {
        ConfirmationEntity entity = new ConfirmationEntity();
        entity.setConfirmationId(55L);
        entity.setEntityType("WBS");
        entity.setEntityId(wbs.getWbsId());
        entity.setTenant(tenant);
        entity.setConfirmationDate(LocalDate.now());
        entity.setConfirmedBy(1L);
        entity.setConfirmedOn(java.time.LocalDateTime.now());

        ConfirmationLockEntity lock = new ConfirmationLockEntity();
        lock.setLockId(5L);
        lock.setWbs(wbs);
        lock.setTenant(tenant);
        lock.setLockDate(LocalDate.of(2025, 11, 10));

        when(confirmationRepository.findByIdForTenant(55L)).thenReturn(Optional.of(entity));
        when(businessRuleEngine.getRuleValue(603)).thenReturn("10");
        when(confirmationLockRepository.findByWbsId(wbs.getWbsId())).thenReturn(Optional.of(lock));
        when(confirmationRepository.findLatestConfirmationDateForWbs(wbs.getWbsId())).thenReturn(null);
        wbs.setPlannedQty(BigDecimal.ZERO);
        wbs.setActualQty(BigDecimal.ZERO);
        when(wbsRepository.findById(wbs.getWbsId())).thenReturn(Optional.of(wbs));

        WbsConfirmationSummaryDTO summary = confirmationService.undoConfirmation(55L);

        assertNotNull(summary);
        verify(confirmationRepository, times(1)).delete(entity);
        verify(confirmationLockRepository, times(1)).delete(lock);
        verify(auditLogService, atLeastOnce()).writeAuditLog(anyString(), anyLong(), anyString(), any(), any());
    }
}


