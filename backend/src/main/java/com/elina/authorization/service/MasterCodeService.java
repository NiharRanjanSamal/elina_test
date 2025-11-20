package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.dto.BulkUploadResult;
import com.elina.authorization.dto.MasterCodeCountDTO;
import com.elina.authorization.dto.MasterCodeCreateDTO;
import com.elina.authorization.dto.MasterCodeDTO;
import com.elina.authorization.entity.MasterCode;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.MasterCodeRepository;
import com.elina.authorization.repository.TenantRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for master code management with tenant-aware operations and Redis caching.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Master codes can only be managed within their own tenant.
 * 
 * Caching: Master codes are cached in Redis with tenant and code type scoping.
 * Cache is invalidated on create/update/delete operations.
 */
@Service
public class MasterCodeService {

    private static final Logger logger = LoggerFactory.getLogger(MasterCodeService.class);
    private static final int DEFAULT_RADIO_LIMIT = 3;

    private final MasterCodeRepository masterCodeRepository;
    private final TenantRepository tenantRepository;

    @Value("${master-data.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${master-data.cache.ttl-minutes:30}")
    private int cacheTtlMinutes;

    public MasterCodeService(MasterCodeRepository masterCodeRepository, TenantRepository tenantRepository) {
        this.masterCodeRepository = masterCodeRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Get current user ID from SecurityContext.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Convert entity to DTO.
     */
    private MasterCodeDTO toDTO(MasterCode entity) {
        MasterCodeDTO dto = new MasterCodeDTO();
        dto.setCodeId(entity.getCodeId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setCodeType(entity.getCodeType());
        dto.setCodeValue(entity.getCodeValue());
        dto.setShortDescription(entity.getShortDescription());
        dto.setLongDescription(entity.getLongDescription());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    /**
     * List master codes with filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<MasterCodeDTO> listMasterCodes(String codeType, String search, Boolean activeOnly, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<MasterCode> entities = masterCodeRepository.findWithFilters(codeType, activeOnly, search, pageable);
        return entities.map(this::toDTO);
    }

    /**
     * Get master code by ID.
     */
    @Transactional(readOnly = true)
    public MasterCodeDTO getMasterCode(Long id) {
        Long tenantId = TenantContext.getTenantId();
        MasterCode entity = masterCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Master code not found"));
        
        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Master code not found");
        }
        
        return toDTO(entity);
    }

    /**
     * Get master code by code type and value.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "masterCodes", key = "'master_codes:' + T(com.elina.authorization.context.TenantContext).getTenantId() + ':' + #codeType + ':' + #codeValue")
    public MasterCodeDTO getMasterCodeByTypeAndValue(String codeType, String codeValue) {
        Long tenantId = TenantContext.getTenantId();
        MasterCode entity = masterCodeRepository.findByCodeTypeAndCodeValue(codeType, codeValue)
                .orElseThrow(() -> new RuntimeException("Master code not found"));
        return toDTO(entity);
    }

    /**
     * Get all active master codes by code type (cached).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "masterCodes", key = "'master_codes:' + T(com.elina.authorization.context.TenantContext).getTenantId() + ':' + #codeType + ':active'")
    public List<MasterCodeDTO> getActiveMasterCodesByType(String codeType) {
        List<MasterCode> entities = masterCodeRepository.findActiveByCodeType(codeType);
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get count of active codes by type.
     */
    @Transactional(readOnly = true)
    public MasterCodeCountDTO getActiveCountByType(String codeType, int radioLimit) {
        long count = masterCodeRepository.countActiveByCodeType(codeType);
        MasterCodeCountDTO dto = new MasterCodeCountDTO();
        dto.setCodeType(codeType);
        dto.setActiveCount(count);
        dto.setUseRadio(count <= radioLimit);
        return dto;
    }

    /**
     * Create a new master code.
     */
    @Transactional
    @CacheEvict(value = "masterCodes", allEntries = true)
    public MasterCodeDTO createMasterCode(MasterCodeCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Check if code already exists
        if (masterCodeRepository.existsByCodeTypeAndCodeValue(dto.getCodeType(), dto.getCodeValue())) {
            throw new RuntimeException("Master code already exists for code type: " + dto.getCodeType() + ", value: " + dto.getCodeValue());
        }

        // Validate critical code types require short description
        if (isCriticalCodeType(dto.getCodeType()) && (dto.getShortDescription() == null || dto.getShortDescription().trim().isEmpty())) {
            throw new RuntimeException("Short description is required for code type: " + dto.getCodeType());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        MasterCode entity = new MasterCode();
        entity.setTenant(tenant);
        entity.setCodeType(dto.getCodeType());
        entity.setCodeValue(dto.getCodeValue());
        entity.setShortDescription(dto.getShortDescription());
        entity.setLongDescription(dto.getLongDescription());
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : true);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        MasterCode saved = masterCodeRepository.save(entity);
        logger.info("Created master code: {} / {} for tenant {}", dto.getCodeType(), dto.getCodeValue(), tenantId);
        return toDTO(saved);
    }

    /**
     * Update an existing master code.
     */
    @Transactional
    @CacheEvict(value = "masterCodes", allEntries = true)
    public MasterCodeDTO updateMasterCode(Long id, MasterCodeCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        MasterCode entity = masterCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Master code not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Master code not found");
        }

        // Check if code value change would create duplicate
        if (!entity.getCodeValue().equals(dto.getCodeValue()) && 
            masterCodeRepository.existsByCodeTypeAndCodeValue(dto.getCodeType(), dto.getCodeValue())) {
            throw new RuntimeException("Master code already exists for code type: " + dto.getCodeType() + ", value: " + dto.getCodeValue());
        }

        // Validate critical code types
        if (isCriticalCodeType(dto.getCodeType()) && (dto.getShortDescription() == null || dto.getShortDescription().trim().isEmpty())) {
            throw new RuntimeException("Short description is required for code type: " + dto.getCodeType());
        }

        entity.setCodeType(dto.getCodeType());
        entity.setCodeValue(dto.getCodeValue());
        entity.setShortDescription(dto.getShortDescription());
        entity.setLongDescription(dto.getLongDescription());
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : entity.getActivateFlag());
        entity.setUpdatedBy(userId);

        MasterCode saved = masterCodeRepository.save(entity);
        logger.info("Updated master code: {} / {} for tenant {}", dto.getCodeType(), dto.getCodeValue(), tenantId);
        return toDTO(saved);
    }

    /**
     * Delete a master code.
     */
    @Transactional
    @CacheEvict(value = "masterCodes", allEntries = true)
    public void deleteMasterCode(Long id) {
        Long tenantId = TenantContext.getTenantId();

        MasterCode entity = masterCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Master code not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Master code not found");
        }

        masterCodeRepository.delete(entity);
        logger.info("Deleted master code: {} for tenant {}", id, tenantId);
    }

    /**
     * Get all distinct code types for the tenant.
     */
    @Transactional(readOnly = true)
    public List<String> getAllCodeTypes() {
        return masterCodeRepository.findDistinctCodeTypes();
    }

    /**
     * Bulk upload master codes from CSV or Excel file.
     */
    @Transactional
    @CacheEvict(value = "masterCodes", allEntries = true)
    public BulkUploadResult bulkUploadMasterCodes(MultipartFile file, boolean dryRun) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        BulkUploadResult result = new BulkUploadResult();
        result.setDryRun(dryRun);

        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new RuntimeException("File name is required");
            }

            List<BulkUploadResult.BulkUploadRowResult> rows = new ArrayList<>();

            if (filename.endsWith(".csv")) {
                rows = parseCSV(file.getInputStream(), tenantId, userId);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                rows = parseExcel(file.getInputStream(), tenantId, userId);
            } else {
                throw new RuntimeException("Unsupported file format. Please use CSV or Excel (.xlsx, .xls)");
            }

            result.setTotalRows(rows.size());
            result.setValidRows((int) rows.stream().filter(r -> r.isValid()).count());
            result.setInvalidRows((int) rows.stream().filter(r -> !r.isValid()).count());

            if (!dryRun) {
                // Actually create/update codes
                int created = 0;
                int updated = 0;
                int skipped = 0;

                for (BulkUploadResult.BulkUploadRowResult row : rows) {
                    if (!row.isValid()) {
                        skipped++;
                        continue;
                    }

                    try {
                        MasterCodeCreateDTO dto = new MasterCodeCreateDTO();
                        dto.setCodeType(row.getCodeType());
                        dto.setCodeValue(row.getCodeValue());
                        dto.setShortDescription(row.getShortDescription());
                        dto.setActivateFlag(true);

                        Optional<MasterCode> existing = masterCodeRepository.findByCodeTypeAndCodeValue(
                            row.getCodeType(), row.getCodeValue());

                        if (existing.isPresent()) {
                            updateMasterCode(existing.get().getCodeId(), dto);
                            updated++;
                            row.setAction("UPDATE");
                        } else {
                            createMasterCode(dto);
                            created++;
                            row.setAction("CREATE");
                        }
                    } catch (Exception e) {
                        row.setValid(false);
                        row.getErrors().add(e.getMessage());
                        skipped++;
                    }
                }

                result.setCreatedCount(created);
                result.setUpdatedCount(updated);
                result.setSkippedCount(skipped);
            } else {
                // Dry run - just validate
                for (BulkUploadResult.BulkUploadRowResult row : rows) {
                    if (row.isValid()) {
                        Optional<MasterCode> existing = masterCodeRepository.findByCodeTypeAndCodeValue(
                            row.getCodeType(), row.getCodeValue());
                        row.setAction(existing.isPresent() ? "UPDATE" : "CREATE");
                    } else {
                        row.setAction("SKIP");
                    }
                }
            }

            result.setRows(rows);
            return result;

        } catch (Exception e) {
            logger.error("Error processing bulk upload", e);
            throw new RuntimeException("Error processing bulk upload: " + e.getMessage(), e);
        }
    }

    /**
     * Parse CSV file.
     */
    private List<BulkUploadResult.BulkUploadRowResult> parseCSV(InputStream inputStream, Long tenantId, Long userId) throws Exception {
        List<BulkUploadResult.BulkUploadRowResult> rows = new ArrayList<>();

            try (CSVParser parser = new CSVParser(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build())) {

            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                BulkUploadResult.BulkUploadRowResult row = new BulkUploadResult.BulkUploadRowResult();
                row.setRowNumber(rowNumber);

                String codeType = record.get("code_type");
                String codeValue = record.get("code_value");
                String shortDescription = record.get("short_description");
                // longDescription is available but not used in validation - can be used in future
                // String longDescription = record.get("long_description");

                row.setCodeType(codeType);
                row.setCodeValue(codeValue);
                row.setShortDescription(shortDescription);

                // Validate
                List<String> errors = new ArrayList<>();
                if (codeType == null || codeType.trim().isEmpty()) {
                    errors.add("code_type is required");
                }
                if (codeValue == null || codeValue.trim().isEmpty()) {
                    errors.add("code_value is required");
                }
                if (isCriticalCodeType(codeType) && (shortDescription == null || shortDescription.trim().isEmpty())) {
                    errors.add("short_description is required for code type: " + codeType);
                }

                row.setErrors(errors);
                row.setValid(errors.isEmpty());
                rows.add(row);
            }
        }

        return rows;
    }

    /**
     * Parse Excel file.
     */
    private List<BulkUploadResult.BulkUploadRowResult> parseExcel(InputStream inputStream, Long tenantId, Long userId) throws Exception {
        List<BulkUploadResult.BulkUploadRowResult> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new RuntimeException("Excel file must have a header row");
            }

            // Find column indices
            int codeTypeIdx = -1, codeValueIdx = -1, shortDescIdx = -1;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String header = cell.getStringCellValue().trim().toLowerCase();
                    if (header.equals("code_type")) codeTypeIdx = i;
                    else if (header.equals("code_value")) codeValueIdx = i;
                    else if (header.equals("short_description")) shortDescIdx = i;
                    // long_description column is optional and not used in validation
                }
            }

            if (codeTypeIdx == -1 || codeValueIdx == -1) {
                throw new RuntimeException("Excel file must have 'code_type' and 'code_value' columns");
            }

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                BulkUploadResult.BulkUploadRowResult result = new BulkUploadResult.BulkUploadRowResult();
                result.setRowNumber(i + 1);

                String codeType = getCellValue(row.getCell(codeTypeIdx));
                String codeValue = getCellValue(row.getCell(codeValueIdx));
                String shortDescription = shortDescIdx >= 0 ? getCellValue(row.getCell(shortDescIdx)) : null;
                // longDescription is available but not used in validation - can be used in future
                // String longDescription = longDescIdx >= 0 ? getCellValue(row.getCell(longDescIdx)) : null;

                result.setCodeType(codeType);
                result.setCodeValue(codeValue);
                result.setShortDescription(shortDescription);

                // Validate
                List<String> errors = new ArrayList<>();
                if (codeType == null || codeType.trim().isEmpty()) {
                    errors.add("code_type is required");
                }
                if (codeValue == null || codeValue.trim().isEmpty()) {
                    errors.add("code_value is required");
                }
                if (isCriticalCodeType(codeType) && (shortDescription == null || shortDescription.trim().isEmpty())) {
                    errors.add("short_description is required for code type: " + codeType);
                }

                result.setErrors(errors);
                result.setValid(errors.isEmpty());
                rows.add(result);
            }
        }

        return rows;
    }

    /**
     * Get cell value as string.
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * Check if code type is critical (requires short description).
     */
    private boolean isCriticalCodeType(String codeType) {
        if (codeType == null) return false;
        return codeType.equals("WORK_CENTER") || codeType.equals("COST_CENTER");
    }

    /**
     * Refresh cache for a specific code type or all codes.
     */
    @CacheEvict(value = "masterCodes", allEntries = true)
    public void refreshCache(String codeType) {
        logger.info("Cache refreshed for code type: {}", codeType != null ? codeType : "ALL");
    }
}

