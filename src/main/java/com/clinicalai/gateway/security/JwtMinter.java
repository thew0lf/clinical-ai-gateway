package com.clinicalai.gateway.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Mints RS256 signed JWTs for local development and integration tests. The matching public key is
 * bundled at classpath keys/app.pub and used by the resource server to verify signatures, so the
 * tokens this produces are verified by exactly the same path a real identity provider would use.
 */
public class JwtMinter {

  private final RSASSASigner signer;

  public JwtMinter(RSAPrivateKey privateKey) {
    this.signer = new RSASSASigner(privateKey);
  }

  public String mint(String subject, String tenantId, List<String> scopes, Duration ttl) {
    try {
      Instant now = Instant.now();
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(subject)
              .issuer("clinical-ai-gateway-dev")
              .claim("tenant_id", tenantId)
              .claim("scope", String.join(" ", scopes))
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(ttl)))
              .build();
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256)
                  .keyID("dev-key")
                  .type(JOSEObjectType.JWT)
                  .build(),
              claims);
      jwt.sign(signer);
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("failed to mint token", e);
    }
  }
}
