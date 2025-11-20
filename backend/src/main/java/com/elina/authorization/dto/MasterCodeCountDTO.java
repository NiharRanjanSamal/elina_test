package com.elina.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for master code count by type.
 * Used to determine if radio buttons or dropdown should be rendered.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterCodeCountDTO {
    private String codeType;
    private long activeCount;
    private boolean useRadio; // true if count <= limit (default 3)
}

