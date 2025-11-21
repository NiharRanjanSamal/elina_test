package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.dto.MasterCodeCreateDTO;
import com.elina.authorization.dto.MasterCodeDTO;
import com.elina.authorization.entity.MasterCode;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.MasterCodeRepository;
import com.elina.authorization.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MasterCodeService.
 */
@ExtendWith(MockitoExtension.class)
class MasterCodeServiceTest {

    @Mock
    private MasterCodeRepository masterCodeRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MasterCodeService masterCodeService;

    private Tenant tenant;
    private MasterCode masterCode;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);

        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantCode("DEFAULT");
        tenant.setName("Default Tenant");
        tenant.setIsActive(true);

        masterCode = new MasterCode();
        masterCode.setCodeId(1L);
        masterCode.setTenant(tenant);
        masterCode.setCodeType("WORK_CENTER");
        masterCode.setCodeValue("WC_SITE");
        masterCode.setShortDescription("Site Work Center");
        masterCode.setLongDescription("Primary work center for site operations");
        masterCode.setActivateFlag(true);
        masterCode.setCreatedBy(1L);
        masterCode.setCreatedOn(LocalDateTime.now());
        masterCode.setUpdatedBy(1L);
        masterCode.setUpdatedOn(LocalDateTime.now());

        // SecurityContext stubbings are only set up in tests that need them
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testListMasterCodes() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<MasterCode> page = new PageImpl<>(Arrays.asList(masterCode), pageable, 1);
        // Match actual method signature: codeType, activeOnly, search, pageable
        when(masterCodeRepository.findWithFilters(eq("WORK_CENTER"), eq(true), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act
        Page<MasterCodeDTO> result = masterCodeService.listMasterCodes("WORK_CENTER", null, true, 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("WC_SITE", result.getContent().get(0).getCodeValue());
    }

    @Test
    void testGetMasterCode() {
        // Arrange
        when(masterCodeRepository.findById(1L)).thenReturn(Optional.of(masterCode));

        // Act
        MasterCodeDTO result = masterCodeService.getMasterCode(1L);

        // Assert
        assertNotNull(result);
        assertEquals("WC_SITE", result.getCodeValue());
        assertEquals("WORK_CENTER", result.getCodeType());
    }

    @Test
    void testGetMasterCodeNotFound() {
        // Arrange
        when(masterCodeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> masterCodeService.getMasterCode(1L));
    }

    @Test
    void testCreateMasterCode() {
        // Arrange
        MasterCodeCreateDTO dto = new MasterCodeCreateDTO();
        dto.setCodeType("WORK_CENTER");
        dto.setCodeValue("WC_NEW");
        dto.setShortDescription("New Work Center");
        dto.setActivateFlag(true);

        // Set up SecurityContext for this test
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(1L);

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(masterCodeRepository.existsByCodeTypeAndCodeValue("WORK_CENTER", "WC_NEW")).thenReturn(false);
        when(masterCodeRepository.save(any(MasterCode.class))).thenReturn(masterCode);

        // Act
        MasterCodeDTO result = masterCodeService.createMasterCode(dto);

        // Assert
        assertNotNull(result);
        verify(masterCodeRepository).save(any(MasterCode.class));
    }

    @Test
    void testCreateMasterCodeDuplicate() {
        // Arrange
        MasterCodeCreateDTO dto = new MasterCodeCreateDTO();
        dto.setCodeType("WORK_CENTER");
        dto.setCodeValue("WC_SITE");
        dto.setShortDescription("Site Work Center");

        when(masterCodeRepository.existsByCodeTypeAndCodeValue("WORK_CENTER", "WC_SITE")).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> masterCodeService.createMasterCode(dto));
    }

    @Test
    void testUpdateMasterCode() {
        // Arrange
        MasterCodeCreateDTO dto = new MasterCodeCreateDTO();
        dto.setCodeType("WORK_CENTER");
        dto.setCodeValue("WC_SITE");
        dto.setShortDescription("Updated Description");
        dto.setActivateFlag(true);

        // Set up SecurityContext for this test
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(1L);

        when(masterCodeRepository.findById(1L)).thenReturn(Optional.of(masterCode));
        // The code value doesn't change, so existsByCodeTypeAndCodeValue won't be called
        // Only called if code value changes, so we don't need to stub it here
        when(masterCodeRepository.save(any(MasterCode.class))).thenReturn(masterCode);

        // Act
        MasterCodeDTO result = masterCodeService.updateMasterCode(1L, dto);

        // Assert
        assertNotNull(result);
        verify(masterCodeRepository).save(any(MasterCode.class));
    }

    @Test
    void testDeleteMasterCode() {
        // Arrange
        when(masterCodeRepository.findById(1L)).thenReturn(Optional.of(masterCode));

        // Act
        masterCodeService.deleteMasterCode(1L);

        // Assert
        verify(masterCodeRepository).delete(masterCode);
    }

    @Test
    void testGetActiveCountByType() {
        // Arrange
        when(masterCodeRepository.countActiveByCodeType("WORK_CENTER")).thenReturn(2L);

        // Act
        var result = masterCodeService.getActiveCountByType("WORK_CENTER", 3);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getActiveCount());
        assertTrue(result.isUseRadio()); // 2 <= 3
    }

    @Test
    void testGetActiveMasterCodesByType() {
        // Arrange
        when(masterCodeRepository.findActiveByCodeType("WORK_CENTER"))
                .thenReturn(Arrays.asList(masterCode));

        // Act
        List<MasterCodeDTO> result = masterCodeService.getActiveMasterCodesByType("WORK_CENTER");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("WC_SITE", result.get(0).getCodeValue());
    }
}

