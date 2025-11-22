package com.elina.projects.controller;

import com.elina.projects.dto.WbsConfirmationRequest;
import com.elina.projects.dto.WbsConfirmationResponse;
import com.elina.projects.dto.WbsConfirmationSummaryDTO;
import com.elina.projects.service.ConfirmationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for confirmation + lock orchestration.
 */
@RestController
@RequestMapping("/api/confirmations")
public class ConfirmationController {

    private final ConfirmationService confirmationService;

    public ConfirmationController(ConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @PostMapping("/wbs/{wbsId}")
    @PreAuthorize("hasAuthority('PAGE_CONFIRMATION_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<WbsConfirmationSummaryDTO> confirmWbs(@PathVariable Long wbsId,
                                                                @Valid @RequestBody WbsConfirmationRequest request) {
        WbsConfirmationSummaryDTO summary = confirmationService.confirmWbs(wbsId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/wbs/{wbsId}")
    @PreAuthorize("hasAuthority('PAGE_CONFIRMATION_VIEW') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<List<WbsConfirmationResponse>> listConfirmations(@PathVariable Long wbsId) {
        return ResponseEntity.ok(confirmationService.getWbsConfirmations(wbsId));
    }

    @GetMapping("/wbs/{wbsId}/summary")
    @PreAuthorize("hasAuthority('PAGE_CONFIRMATION_VIEW') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<WbsConfirmationSummaryDTO> getSummary(@PathVariable Long wbsId,
            @RequestParam(value = "previewDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previewDate) {
        return ResponseEntity.ok(confirmationService.getConfirmationSummary(wbsId, previewDate));
    }

    @DeleteMapping("/{confirmationId}")
    @PreAuthorize("hasAuthority('PAGE_CONFIRMATION_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<WbsConfirmationSummaryDTO> undo(@PathVariable Long confirmationId) {
        return ResponseEntity.ok(confirmationService.undoConfirmation(confirmationId));
    }
}

