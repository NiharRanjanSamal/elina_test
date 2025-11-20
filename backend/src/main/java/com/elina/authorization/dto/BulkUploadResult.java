package com.elina.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for bulk upload operations.
 * Contains validation results, preview data, and execution statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResult {
    
    private boolean dryRun;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    
    private List<BulkUploadRowResult> rows = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUploadRowResult {
        private int rowNumber;
        private String codeType;
        private String codeValue;
        private String shortDescription;
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private String action; // "CREATE", "UPDATE", "SKIP"
    }
}

