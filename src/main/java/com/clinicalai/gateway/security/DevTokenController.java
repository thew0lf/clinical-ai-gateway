package com.clinicalai.gateway.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development only helper that issues signed JWTs so the secured API can be exercised locally
 * without standing up an identity provider. Active only under the dev profile. The signing key is a
 * throwaway key committed for convenience and must never be used outside local development.
 */
@RestController
@Profile("dev")
@RequestMapping("/dev")
public class DevTokenController {

  private final JwtMinter minter = new JwtMinter(PemKeys.loadPrivateKey("keys/app.key"));

  @GetMapping("/token")
  public Map<String, String> token(
      @RequestParam(defaultValue = "clinician@demo") String subject,
      @RequestParam(defaultValue = "tenant-alpha") String tenant,
      @RequestParam(defaultValue = "suggestion:create suggestion:approve suggestion:execute")
          String scope) {
    String token =
        minter.mint(subject, tenant, Arrays.asList(scope.trim().split("\\s+")), Duration.ofHours(2));
    Map<String, String> body = new LinkedHashMap<>();
    body.put("access_token", token);
    body.put("token_type", "Bearer");
    body.put("tenant_id", tenant);
    body.put("scope", scope);
    return body;
  }
}
