package com.clinicalai.gateway.web;

import com.clinicalai.gateway.dto.UsageResponse;
import com.clinicalai.gateway.service.UsageMeteringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read surface for per tenant usage and cost observability. Tenant is read from the JWT. */
@RestController
@RequestMapping("/api/v1/usage")
@Tag(name = "Usage", description = "Per tenant usage and cost observability")
public class UsageController {

  private final UsageMeteringService usageMeteringService;

  public UsageController(UsageMeteringService usageMeteringService) {
    this.usageMeteringService = usageMeteringService;
  }

  @GetMapping
  @Operation(summary = "Current usage snapshot for the caller tenant")
  public UsageResponse usage(@AuthenticationPrincipal Jwt jwt) {
    String tenantId = tenant(jwt);
    return new UsageResponse(
        tenantId,
        usageMeteringService.mode(),
        usageMeteringService.requestBudgetPerMinute(),
        usageMeteringService.currentRequestCount(tenantId),
        usageMeteringService.tokensToday(tenantId));
  }

  private String tenant(Jwt jwt) {
    String tenant = jwt.getClaimAsString("tenant_id");
    if (tenant == null || tenant.isBlank()) {
      throw new IllegalStateException("token is missing the tenant_id claim");
    }
    return tenant;
  }
}
