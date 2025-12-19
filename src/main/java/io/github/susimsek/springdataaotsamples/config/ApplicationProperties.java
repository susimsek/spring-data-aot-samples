package io.github.susimsek.springdataaotsamples.config;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Data
public class ApplicationProperties {

  private final ApiDocs apiDocs = new ApiDocs();
  private final Security security = new Security();
  private final Cache cache = new Cache();

  @Data
  public static class Security {
    private String contentSecurityPolicy = ApplicationDefaults.Security.contentSecurityPolicy;

    private final Jwt jwt = new Jwt();
  }

  @Data
  public static class Jwt {
    private String secret;
    private String issuer;
    private List<String> audience = ApplicationDefaults.Security.Jwt.audience;
    private Duration accessTokenTtl = ApplicationDefaults.Security.Jwt.accessTokenTtl;
    private Duration refreshTokenTtl = ApplicationDefaults.Security.Jwt.refreshTokenTtl;
    private Duration refreshTokenTtlForRememberMe =
        ApplicationDefaults.Security.Jwt.refreshTokenTtlForRememberMe;
  }

  @Getter
  @Setter
  public static class ApiDocs {
    private String title = ApplicationDefaults.ApiDocs.title;
    private String description = ApplicationDefaults.ApiDocs.description;
    private String version = ApplicationDefaults.ApiDocs.version;
    private String termsOfServiceUrl = ApplicationDefaults.ApiDocs.termsOfServiceUrl;
    private String contactName = ApplicationDefaults.ApiDocs.contactName;
    private String contactUrl = ApplicationDefaults.ApiDocs.contactUrl;
    private String contactEmail = ApplicationDefaults.ApiDocs.contactEmail;
    private String license = ApplicationDefaults.ApiDocs.license;
    private String licenseUrl = ApplicationDefaults.ApiDocs.licenseUrl;
    private Server[] servers = {};

    @Getter
    @Setter
    public static class Server {
      private String url;
      private String description;
    }
  }

  @Data
  public static class Cache {
    private final Caffeine caffeine = new Caffeine();
  }

  @Data
  public static class Caffeine {
    private Duration ttl = ApplicationDefaults.Cache.Caffeine.ttl;
    private int initialCapacity = ApplicationDefaults.Cache.Caffeine.initialCapacity;
    private long maximumSize = ApplicationDefaults.Cache.Caffeine.maximumSize;
  }
}
