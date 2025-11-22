package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import com.elina.projects.repository.EquipmentAllocationRepository;
import com.elina.projects.repository.ManpowerAllocationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Rule 602 — ALLOCATION_CANNOT_OVERLAP_EXISTING.
 * Prevents overlapping allocations for the same resource within a WBS.
 */
@Component
public class AllocationOverlapRuleValidator implements BusinessRuleValidator {

    private final ManpowerAllocationRepository manpowerAllocationRepository;
    private final EquipmentAllocationRepository equipmentAllocationRepository;

    public AllocationOverlapRuleValidator(ManpowerAllocationRepository manpowerAllocationRepository,
                                          EquipmentAllocationRepository equipmentAllocationRepository) {
        this.manpowerAllocationRepository = manpowerAllocationRepository;
        this.equipmentAllocationRepository = equipmentAllocationRepository;
    }

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        String resourceType = context.getParamAsString("resourceType");
        Long resourceId = context.getParam("resourceId", Long.class);
        Long wbsId = context.getParam("wbsId", Long.class);
        Long allocationId = context.getParam("allocationId", Long.class);
        LocalDate startDate = context.getAllocationStartDate();
        LocalDate endDate = context.getAllocationEndDate();

        if (resourceType == null || resourceId == null || wbsId == null || startDate == null || endDate == null) {
            return;
        }

        boolean overlap = false;
        if ("MANPOWER".equalsIgnoreCase(resourceType)) {
            overlap = manpowerAllocationRepository.existsOverlappingAllocation(wbsId, resourceId, startDate, endDate, allocationId);
        } else if ("EQUIPMENT".equalsIgnoreCase(resourceType)) {
            overlap = equipmentAllocationRepository.existsOverlappingAllocation(wbsId, resourceId, startDate, endDate, allocationId);
        }

        if (overlap) {
            throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    "Allocation overlaps with an existing booking for this resource.",
                    "Select a different date range or resource.");
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{602};
    }
}

