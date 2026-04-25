package com.bank.cebos.controller.portal;

import com.bank.cebos.dto.portal.BatchUploadResponse;
import com.bank.cebos.dto.portal.CorrectionUploadResponse;
import com.bank.cebos.dto.portal.PortalBatchDetailResponse;
import com.bank.cebos.dto.portal.PortalBatchEmployeeRowResponse;
import com.bank.cebos.dto.portal.PortalBatchInviteDispatchResponse;
import com.bank.cebos.dto.portal.PortalBatchListItemResponse;
import com.bank.cebos.dto.portal.PortalEmployeeDetailResponse;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.batch.BatchIngestService;
import com.bank.cebos.service.batch.CorrectionIngestService;
import com.bank.cebos.service.invite.InviteDispatchService;
import com.bank.cebos.service.kyc.EmployeeKycImageService;
import com.bank.cebos.service.kyc.KycImageKind;
import com.bank.cebos.service.portal.PortalBatchEmployeeQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/portal/batches")
public class PortalBatchController {

  private static final String PORTAL_ROLE_AND_KIND_SECURITY =
      "hasAnyRole('ADMIN','VIEWER') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)";

  private static final String PORTAL_ADMIN_UPLOAD_SECURITY =
      "hasRole('ADMIN') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)"
          + " and @principalAccess.portalClientMatches(#corporateClientId)";

  private static final String PORTAL_ADMIN_CORRECTION_SECURITY =
      "hasRole('ADMIN') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)"
          + " and @principalAccess.portalUserOwnsUploadBatch(#batchRef)";

  private static final String PORTAL_ADMIN_INVITE_DISPATCH_SECURITY =
      "hasRole('ADMIN') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)"
          + " and @principalAccess.portalUserOwnsUploadBatch(#batchRef)";

  private static final String PORTAL_ADMIN_BATCH_IMAGE_SECURITY =
      "hasRole('ADMIN') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)"
          + " and @principalAccess.portalEmployeeInOwnedBatch(#batchRef, #employeeRef)";

  private final UploadBatchRepository uploadBatchRepository;
  private final BatchIngestService batchIngestService;
  private final CorrectionIngestService correctionIngestService;
  private final InviteDispatchService inviteDispatchService;
  private final PortalBatchEmployeeQueryService portalBatchEmployeeQueryService;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final EmployeeKycImageService employeeKycImageService;

  public PortalBatchController(
      UploadBatchRepository uploadBatchRepository,
      BatchIngestService batchIngestService,
      CorrectionIngestService correctionIngestService,
      InviteDispatchService inviteDispatchService,
      PortalBatchEmployeeQueryService portalBatchEmployeeQueryService,
      EmployeeOnboardingRepository employeeOnboardingRepository,
      EmployeeKycImageService employeeKycImageService) {
    this.uploadBatchRepository = uploadBatchRepository;
    this.batchIngestService = batchIngestService;
    this.correctionIngestService = correctionIngestService;
    this.inviteDispatchService = inviteDispatchService;
    this.portalBatchEmployeeQueryService = portalBatchEmployeeQueryService;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.employeeKycImageService = employeeKycImageService;
  }

  @GetMapping
  @PreAuthorize(PORTAL_ROLE_AND_KIND_SECURITY)
  public ResponseEntity<Page<PortalBatchListItemResponse>> listBatches(
      @AuthenticationPrincipal CebosUserDetails principal, Pageable pageable) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.ok(Page.empty(pageable));
    }
    Page<PortalBatchListItemResponse> page =
        uploadBatchRepository.findByCorporateClientId(clientId, pageable).map(this::toListItem);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/{batchRef}")
  @PreAuthorize(
      PORTAL_ROLE_AND_KIND_SECURITY + " and @principalAccess.portalUserOwnsUploadBatch(#batchRef)")
  public ResponseEntity<PortalBatchDetailResponse> getBatch(
      @AuthenticationPrincipal CebosUserDetails principal, @PathVariable("batchRef") String batchRef) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.notFound().build();
    }
    return uploadBatchRepository
        .findByBatchReferenceAndCorporateClientId(batchRef, clientId)
        .map(this::toDetail)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{batchRef}/employees")
  @PreAuthorize(
      PORTAL_ROLE_AND_KIND_SECURITY + " and @principalAccess.portalUserOwnsUploadBatch(#batchRef)")
  public ResponseEntity<Page<PortalBatchEmployeeRowResponse>> listBatchEmployees(
      @AuthenticationPrincipal CebosUserDetails principal,
      @PathVariable("batchRef") String batchRef,
      Pageable pageable) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.ok(Page.empty(pageable));
    }
    return portalBatchEmployeeQueryService
        .listEmployeesForBatch(batchRef, clientId, pageable)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{batchRef}/employees/{employeeRef}")
  @PreAuthorize(
      PORTAL_ROLE_AND_KIND_SECURITY
          + " and @principalAccess.portalEmployeeInOwnedBatch(#batchRef, #employeeRef)")
  public ResponseEntity<PortalEmployeeDetailResponse> getBatchEmployeeDetail(
      @AuthenticationPrincipal CebosUserDetails principal,
      @PathVariable("batchRef") String batchRef,
      @PathVariable("employeeRef") String employeeRef) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.notFound().build();
    }
    return portalBatchEmployeeQueryService
        .getEmployeeDetail(batchRef, employeeRef, clientId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{batchRef}/employees/{employeeRef}/images/{kind}")
  @PreAuthorize(PORTAL_ADMIN_BATCH_IMAGE_SECURITY)
  public ResponseEntity<byte[]> getBatchEmployeeImage(
      @PathVariable("batchRef") String batchRef,
      @PathVariable("employeeRef") String employeeRef,
      @PathVariable("kind") String kind) {
    KycImageKind imageKind = KycImageKind.fromPathSegment(kind);
    return employeeOnboardingRepository
        .findByEmployeeRef(employeeRef)
        .flatMap(e -> employeeKycImageService.readImage(e, imageKind))
        .map(
            bytes ->
                ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "private, max-age=60")
                    .body(bytes))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(PORTAL_ADMIN_UPLOAD_SECURITY)
  public ResponseEntity<BatchUploadResponse> uploadBatch(
      @AuthenticationPrincipal CebosUserDetails principal,
      @RequestParam("corporateClientId") Long corporateClientId,
      @RequestParam("file") MultipartFile file) {
    if (principal.corporateClientId() == null) {
      return ResponseEntity.badRequest().build();
    }
    BatchUploadResponse body =
        batchIngestService.initiateUpload(file, corporateClientId, principal.id());
    return ResponseEntity.ok(body);
  }

  @PostMapping("/{batchRef}/invites/dispatch")
  @PreAuthorize(PORTAL_ADMIN_INVITE_DISPATCH_SECURITY)
  public ResponseEntity<PortalBatchInviteDispatchResponse> dispatchInvitesForBatch(
      @AuthenticationPrincipal CebosUserDetails principal, @PathVariable("batchRef") String batchRef) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.badRequest().build();
    }
    try {
      PortalBatchInviteDispatchResponse body =
          inviteDispatchService.dispatchValidatedForOwnedBatch(
              batchRef, clientId, "portalUser:" + principal.id());
      return ResponseEntity.ok(body);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping(
      value = "/{batchRef}/corrections/upload",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(PORTAL_ADMIN_CORRECTION_SECURITY)
  public ResponseEntity<CorrectionUploadResponse> uploadCorrections(
      @AuthenticationPrincipal CebosUserDetails principal,
      @PathVariable("batchRef") String batchRef,
      @RequestParam("file") MultipartFile file) {
    Long clientId = principal.corporateClientId();
    if (clientId == null) {
      return ResponseEntity.badRequest().build();
    }
    CorrectionUploadResponse body =
        correctionIngestService.initiateCorrectionUpload(
            file, batchRef, clientId, principal.id());
    return ResponseEntity.ok(body);
  }

  private PortalBatchListItemResponse toListItem(UploadBatch b) {
    return new PortalBatchListItemResponse(
        b.getId(),
        b.getBatchReference(),
        b.getStatus(),
        b.getTotalRows(),
        b.getValidRowCount(),
        b.getInvalidRowCount(),
        b.getCreatedAt());
  }

  private PortalBatchDetailResponse toDetail(UploadBatch b) {
    return new PortalBatchDetailResponse(
        b.getId(),
        b.getBatchReference(),
        b.getOriginalFilename(),
        b.getStatus(),
        b.getTotalRows(),
        b.getValidRowCount(),
        b.getInvalidRowCount(),
        b.getCreatedAt(),
        b.getUpdatedAt());
  }
}
