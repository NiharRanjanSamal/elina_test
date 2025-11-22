package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.projects.dto.resource.*;
import com.elina.projects.entity.Wbs;
import com.elina.projects.entity.resource.*;
import com.elina.projects.exception.NotFoundException;
import com.elina.projects.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resource allocation orchestration service that handles validation, costing and audit logging.
 */
@Service
public class ResourceAllocationService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceAllocationService.class);

    private final ManpowerAllocationRepository manpowerAllocationRepository;
    private final EquipmentAllocationRepository equipmentAllocationRepository;
    private final EmployeeRepository employeeRepository;
    private final EquipmentRepository equipmentRepository;
    private final WbsRepository wbsRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;
    private final ResourceCostCalculator resourceCostCalculator;

    public ResourceAllocationService(ManpowerAllocationRepository manpowerAllocationRepository,
                                     EquipmentAllocationRepository equipmentAllocationRepository,
                                     EmployeeRepository employeeRepository,
                                     EquipmentRepository equipmentRepository,
                                     WbsRepository wbsRepository,
                                     BusinessRuleEngine businessRuleEngine,
                                     AuditLogService auditLogService,
                                     ResourceCostCalculator resourceCostCalculator) {
        this.manpowerAllocationRepository = manpowerAllocationRepository;
        this.equipmentAllocationRepository = equipmentAllocationRepository;
        this.employeeRepository = employeeRepository;
        this.equipmentRepository = equipmentRepository;
        this.wbsRepository = wbsRepository;
        this.businessRuleEngine = businessRuleEngine;
        this.auditLogService = auditLogService;
        this.resourceCostCalculator = resourceCostCalculator;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    private Wbs getWbsOrThrow(Long wbsId) {
        Wbs wbs = wbsRepository.findById(wbsId)
                .orElseThrow(() -> new NotFoundException("WBS not found"));
        Long tenantId = TenantContext.getTenantId();
        if (!wbs.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("WBS not found");
        }
        return wbs;
    }

    private EmployeeEntity getEmployeeOrThrow(Long employeeId) {
        return employeeRepository.findByIdForTenant(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private EquipmentEntity getEquipmentOrThrow(Long equipmentId) {
        return equipmentRepository.findByIdForTenant(equipmentId)
                .orElseThrow(() -> new NotFoundException("Equipment not found"));
    }

    private void assertTenantOwnership(Long entityTenantId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || entityTenantId == null || !tenantId.equals(entityTenantId)) {
            throw new NotFoundException("Resource allocation not found");
        }
    }

    private void validateAllocationRules(Wbs wbs,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         String resourceType,
                                         Long resourceId,
                                         Long allocationId) {
        Long tenantId = TenantContext.getTenantId();
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(getCurrentUserId())
                .entityType("RESOURCE_ALLOCATION")
                .entityId(wbs.getWbsId())
                .wbsStartDate(wbs.getStartDate())
                .wbsEndDate(wbs.getEndDate())
                .lockDate(wbs.getLockDate())
                .isLocked(wbs.getIsLocked())
                .allocationStartDate(startDate)
                .allocationEndDate(endDate)
                .updateDate(startDate)
                .build();

        context.addParam("resourceType", resourceType);
        context.addParam("resourceId", resourceId);
        context.addParam("wbsId", wbs.getWbsId());
        if (allocationId != null) {
            context.addParam("allocationId", allocationId);
        }

        businessRuleEngine.validate(501, context); // Start/end validation
        businessRuleEngine.validate(601, context); // WBS date window validation
        businessRuleEngine.validate(101, context); // Backdate allowed till
        businessRuleEngine.validate(102, context); // Backdate after lock
        businessRuleEngine.validate(602, context); // Overlap prevention
    }

    private long computeDuration(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private BigDecimal safeRate(BigDecimal rate) {
        return rate != null ? rate : BigDecimal.ZERO;
    }

    private Map<String, Object> toAuditMap(ManpowerAllocationEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("allocationId", entity.getAllocationId());
        map.put("wbsId", entity.getWbs().getWbsId());
        map.put("employeeId", entity.getEmployee().getEmployeeId());
        map.put("startDate", entity.getStartDate());
        map.put("endDate", entity.getEndDate());
        map.put("hoursAssigned", entity.getHoursAssigned());
        map.put("durationDays", entity.getDurationDays());
        map.put("dailyRate", entity.getDailyRate());
        map.put("totalCost", entity.getTotalCost());
        map.put("remarks", entity.getRemarks());
        return map;
    }

    private Map<String, Object> toAuditMap(EquipmentAllocationEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("allocationId", entity.getAllocationId());
        map.put("wbsId", entity.getWbs().getWbsId());
        map.put("equipmentId", entity.getEquipment().getEquipmentId());
        map.put("startDate", entity.getStartDate());
        map.put("endDate", entity.getEndDate());
        map.put("hoursAssigned", entity.getHoursAssigned());
        map.put("durationDays", entity.getDurationDays());
        map.put("dailyRate", entity.getDailyRate());
        map.put("totalCost", entity.getTotalCost());
        map.put("remarks", entity.getRemarks());
        return map;
    }

    private ManpowerAllocationResponseDTO toDto(ManpowerAllocationEntity entity) {
        ManpowerAllocationResponseDTO dto = new ManpowerAllocationResponseDTO();
        dto.setAllocationId(entity.getAllocationId());
        dto.setWbsId(entity.getWbs().getWbsId());
        dto.setWbsCode(entity.getWbs().getWbsCode());
        dto.setEmployeeId(entity.getEmployee().getEmployeeId());
        dto.setEmployeeName(entity.getEmployee().getName());
        dto.setSkillLevel(entity.getEmployee().getSkillLevel());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDurationDays(entity.getDurationDays());
        dto.setHoursPerDay(entity.getHoursAssigned());
        dto.setRatePerDay(entity.getDailyRate());
        dto.setTotalCost(entity.getTotalCost());
        dto.setRemarks(entity.getRemarks());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    private EquipmentAllocationResponseDTO toDto(EquipmentAllocationEntity entity) {
        EquipmentAllocationResponseDTO dto = new EquipmentAllocationResponseDTO();
        dto.setAllocationId(entity.getAllocationId());
        dto.setWbsId(entity.getWbs().getWbsId());
        dto.setWbsCode(entity.getWbs().getWbsCode());
        dto.setEquipmentId(entity.getEquipment().getEquipmentId());
        dto.setEquipmentName(entity.getEquipment().getEquipmentName());
        dto.setEquipmentType(entity.getEquipment().getEquipmentType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDurationDays(entity.getDurationDays());
        dto.setHoursPerDay(entity.getHoursAssigned());
        dto.setRatePerDay(entity.getDailyRate());
        dto.setTotalCost(entity.getTotalCost());
        dto.setRemarks(entity.getRemarks());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    private void populateAuditFields(Object entity, Long userId) {
        if (entity instanceof ManpowerAllocationEntity manpower) {
            manpower.setUpdatedBy(userId);
            if (manpower.getCreatedBy() == null) {
                manpower.setCreatedBy(userId);
            }
        } else if (entity instanceof EquipmentAllocationEntity equipment) {
            equipment.setUpdatedBy(userId);
            if (equipment.getCreatedBy() == null) {
                equipment.setCreatedBy(userId);
            }
        }
    }

    @Transactional
    public ManpowerAllocationResponseDTO allocateEmployeeToWbs(ManpowerAllocationRequestDTO dto) {
        Long userId = getCurrentUserId();
        Wbs wbs = getWbsOrThrow(dto.getWbsId());
        EmployeeEntity employee = getEmployeeOrThrow(dto.getEmployeeId());

        if (!Boolean.TRUE.equals(employee.getActivateFlag())) {
            throw new IllegalStateException("Employee is inactive");
        }

        validateAllocationRules(wbs, dto.getStartDate(), dto.getEndDate(), "MANPOWER", employee.getEmployeeId(), null);

        ManpowerAllocationEntity entity = new ManpowerAllocationEntity();
        entity.setTenant(wbs.getTenant());
        entity.setWbs(wbs);
        entity.setEmployee(employee);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setHoursAssigned(dto.getHoursPerDay());
        entity.setDurationDays(computeDuration(dto.getStartDate(), dto.getEndDate()));
        entity.setDailyRate(safeRate(employee.getRatePerDay()));
        entity.setTotalCost(entity.getDailyRate().multiply(BigDecimal.valueOf(entity.getDurationDays())));
        entity.setRemarks(dto.getRemarks());
        entity.setActivateFlag(true);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        ManpowerAllocationEntity saved = manpowerAllocationRepository.save(entity);
        auditLogService.writeAuditLog("MANPOWER_ALLOCATIONS", saved.getAllocationId(), "INSERT", null, toAuditMap(saved));
        logger.info("Allocated employee {} to WBS {} for tenant {}", employee.getEmployeeId(), wbs.getWbsId(), wbs.getTenant().getId());
        return toDto(saved);
    }

    @Transactional
    public ManpowerAllocationResponseDTO updateManpowerAllocation(Long allocationId, ManpowerAllocationRequestDTO dto) {
        Long userId = getCurrentUserId();
        ManpowerAllocationEntity entity = manpowerAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Allocation not found"));
        assertTenantOwnership(entity.getTenant().getId());

        Wbs wbs = getWbsOrThrow(dto.getWbsId());
        EmployeeEntity employee = getEmployeeOrThrow(dto.getEmployeeId());

        Map<String, Object> oldData = toAuditMap(entity);

        validateAllocationRules(wbs, dto.getStartDate(), dto.getEndDate(), "MANPOWER", employee.getEmployeeId(), allocationId);

        entity.setWbs(wbs);
        entity.setEmployee(employee);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setHoursAssigned(dto.getHoursPerDay());
        entity.setDurationDays(computeDuration(dto.getStartDate(), dto.getEndDate()));
        entity.setDailyRate(safeRate(employee.getRatePerDay()));
        entity.setTotalCost(entity.getDailyRate().multiply(BigDecimal.valueOf(entity.getDurationDays())));
        entity.setRemarks(dto.getRemarks());
        populateAuditFields(entity, userId);

        ManpowerAllocationEntity saved = manpowerAllocationRepository.save(entity);
        auditLogService.writeAuditLog("MANPOWER_ALLOCATIONS", saved.getAllocationId(), "UPDATE", oldData, toAuditMap(saved));
        return toDto(saved);
    }

    @Transactional
    public void deleteManpowerAllocation(Long allocationId) {
        ManpowerAllocationEntity entity = manpowerAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Allocation not found"));
        assertTenantOwnership(entity.getTenant().getId());
        Map<String, Object> oldData = toAuditMap(entity);
        manpowerAllocationRepository.delete(entity);
        auditLogService.writeAuditLog("MANPOWER_ALLOCATIONS", allocationId, "DELETE", oldData, null);
    }

    @Transactional
    public EquipmentAllocationResponseDTO allocateEquipmentToWbs(EquipmentAllocationRequestDTO dto) {
        Long userId = getCurrentUserId();
        Wbs wbs = getWbsOrThrow(dto.getWbsId());
        EquipmentEntity equipment = getEquipmentOrThrow(dto.getEquipmentId());

        if (!Boolean.TRUE.equals(equipment.getActivateFlag())) {
            throw new IllegalStateException("Equipment is inactive");
        }

        validateAllocationRules(wbs, dto.getStartDate(), dto.getEndDate(), "EQUIPMENT", equipment.getEquipmentId(), null);

        EquipmentAllocationEntity entity = new EquipmentAllocationEntity();
        entity.setTenant(wbs.getTenant());
        entity.setWbs(wbs);
        entity.setEquipment(equipment);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setHoursAssigned(dto.getHoursPerDay());
        entity.setDurationDays(computeDuration(dto.getStartDate(), dto.getEndDate()));
        entity.setDailyRate(safeRate(equipment.getRatePerDay()));
        entity.setTotalCost(entity.getDailyRate().multiply(BigDecimal.valueOf(entity.getDurationDays())));
        entity.setRemarks(dto.getRemarks());
        entity.setActivateFlag(true);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        EquipmentAllocationEntity saved = equipmentAllocationRepository.save(entity);
        auditLogService.writeAuditLog("EQUIPMENT_ALLOCATIONS", saved.getAllocationId(), "INSERT", null, toAuditMap(saved));
        return toDto(saved);
    }

    @Transactional
    public EquipmentAllocationResponseDTO updateEquipmentAllocation(Long allocationId, EquipmentAllocationRequestDTO dto) {
        Long userId = getCurrentUserId();
        EquipmentAllocationEntity entity = equipmentAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Allocation not found"));
        assertTenantOwnership(entity.getTenant().getId());

        Wbs wbs = getWbsOrThrow(dto.getWbsId());
        EquipmentEntity equipment = getEquipmentOrThrow(dto.getEquipmentId());
        Map<String, Object> oldData = toAuditMap(entity);

        validateAllocationRules(wbs, dto.getStartDate(), dto.getEndDate(), "EQUIPMENT", equipment.getEquipmentId(), allocationId);

        entity.setWbs(wbs);
        entity.setEquipment(equipment);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setHoursAssigned(dto.getHoursPerDay());
        entity.setDurationDays(computeDuration(dto.getStartDate(), dto.getEndDate()));
        entity.setDailyRate(safeRate(equipment.getRatePerDay()));
        entity.setTotalCost(entity.getDailyRate().multiply(BigDecimal.valueOf(entity.getDurationDays())));
        entity.setRemarks(dto.getRemarks());
        populateAuditFields(entity, userId);

        EquipmentAllocationEntity saved = equipmentAllocationRepository.save(entity);
        auditLogService.writeAuditLog("EQUIPMENT_ALLOCATIONS", saved.getAllocationId(), "UPDATE", oldData, toAuditMap(saved));
        return toDto(saved);
    }

    @Transactional
    public void deleteEquipmentAllocation(Long allocationId) {
        EquipmentAllocationEntity entity = equipmentAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Allocation not found"));
        assertTenantOwnership(entity.getTenant().getId());
        Map<String, Object> oldData = toAuditMap(entity);
        equipmentAllocationRepository.delete(entity);
        auditLogService.writeAuditLog("EQUIPMENT_ALLOCATIONS", allocationId, "DELETE", oldData, null);
    }

    @Transactional(readOnly = true)
    public List<ManpowerAllocationResponseDTO> getManpowerAllocationsForWbs(Long wbsId) {
        return manpowerAllocationRepository.findByWbsId(wbsId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EquipmentAllocationResponseDTO> getEquipmentAllocationsForWbs(Long wbsId) {
        return equipmentAllocationRepository.findByWbsId(wbsId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceOptionDTO> getActiveEmployees(String query) {
        return employeeRepository.search(query, true).stream()
                .map(entity -> {
                    ResourceOptionDTO option = new ResourceOptionDTO();
                    option.setId(entity.getEmployeeId());
                    option.setName(entity.getName());
                    option.setCategory("MANPOWER");
                    option.setRatePerDay(entity.getRatePerDay());
                    option.setMetadata(entity.getSkillLevel());
                    return option;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceOptionDTO> getActiveEquipment(String query) {
        return equipmentRepository.search(query, true).stream()
                .map(entity -> {
                    ResourceOptionDTO option = new ResourceOptionDTO();
                    option.setId(entity.getEquipmentId());
                    option.setName(entity.getEquipmentName());
                    option.setCategory("EQUIPMENT");
                    option.setRatePerDay(entity.getRatePerDay());
                    option.setMetadata(entity.getEquipmentType());
                    return option;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AllocationTimelineItemDTO> getTimeline(Long wbsId) {
        List<AllocationTimelineItemDTO> timeline = new ArrayList<>();
        manpowerAllocationRepository.findByWbsId(wbsId).forEach(entity -> {
            AllocationTimelineItemDTO item = new AllocationTimelineItemDTO();
            item.setResourceName(entity.getEmployee().getName());
            item.setResourceType("MANPOWER");
            item.setStartDate(entity.getStartDate());
            item.setEndDate(entity.getEndDate());
            item.setDurationDays(entity.getDurationDays());
            timeline.add(item);
        });
        equipmentAllocationRepository.findByWbsId(wbsId).forEach(entity -> {
            AllocationTimelineItemDTO item = new AllocationTimelineItemDTO();
            item.setResourceName(entity.getEquipment().getEquipmentName());
            item.setResourceType("EQUIPMENT");
            item.setStartDate(entity.getStartDate());
            item.setEndDate(entity.getEndDate());
            item.setDurationDays(entity.getDurationDays());
            timeline.add(item);
        });
        return timeline;
    }

    @Transactional(readOnly = true)
    public ResourceCostSummaryDTO getCostSummaryForWbs(Long wbsId) {
        BigDecimal manpowerCost = Optional.ofNullable(manpowerAllocationRepository.sumCostByWbsId(wbsId)).orElse(BigDecimal.ZERO);
        BigDecimal equipmentCost = Optional.ofNullable(equipmentAllocationRepository.sumCostByWbsId(wbsId)).orElse(BigDecimal.ZERO);
        ResourceCostSummaryDTO summary = new ResourceCostSummaryDTO();
        summary.setWbsId(wbsId);
        summary.setManpowerCost(manpowerCost);
        summary.setEquipmentCost(equipmentCost);
        summary.setTotalCost(manpowerCost.add(equipmentCost));
        return summary;
    }

    @Transactional(readOnly = true)
    public ResourceCostBreakdown previewEmployeeCost(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return resourceCostCalculator.calculateEmployeeCost(employeeId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public ResourceCostBreakdown previewEquipmentCost(Long equipmentId, LocalDate startDate, LocalDate endDate) {
        return resourceCostCalculator.calculateEquipmentCost(equipmentId, startDate, endDate);
    }
}

