package com.clinicalai.gateway.security;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.ClassPathResource;

/** Loads a PKCS8 RSA private key from a classpath PEM file. Used only to sign dev and test tokens. */
public final class PemKeys {

  private PemKeys() {}

  public static RSAPrivateKey loadPrivateKey(String classpathLocation) {
    try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
      String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      String base64 =
          pem.replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s", "");
      byte[] der = Base64.getDecoder().decode(base64);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
      return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    } catch (Exception e) {
      throw new IllegalStateException("failed to load private key from " + classpathLocation, e);
    }
  }
}
