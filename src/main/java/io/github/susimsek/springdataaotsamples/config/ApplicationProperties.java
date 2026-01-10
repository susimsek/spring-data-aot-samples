package io.github.susimsek.springdataaotsamples.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMin;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Validated
@Getter
@Setter
public class ApplicationProperties {

    @Valid private final ApiDocs apiDocs = new ApiDocs();
    @Valid private final Security security = new Security();
    @Valid private final Cache cache = new Cache();

    @Getter
    @Setter
    public static class Security {
        private String contentSecurityPolicy = ApplicationDefaults.Security.contentSecurityPolicy;

        @Valid private final Jwt jwt = new Jwt();
    }

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank private String secret;
        @URL @NotBlank private String issuer;
        @NotEmpty private List<@NotBlank String> audience;

        @DurationMin(seconds = 1)
        @NotNull
        private Duration accessTokenTtl = ApplicationDefaults.Security.Jwt.accessTokenTtl;

        @DurationMin(seconds = 1)
        @NotNull
        private Duration refreshTokenTtl = ApplicationDefaults.Security.Jwt.refreshTokenTtl;

        @DurationMin(seconds = 1)
        @NotNull
        private Duration refreshTokenTtlForRememberMe =
                ApplicationDefaults.Security.Jwt.refreshTokenTtlForRememberMe;
    }

    @Getter
    @Setter
    public static class ApiDocs {
        private String title = ApplicationDefaults.ApiDocs.title;
        private String description = ApplicationDefaults.ApiDocs.description;
        private String version = ApplicationDefaults.ApiDocs.version;

        @URL
        private @Nullable String termsOfServiceUrl = ApplicationDefaults.ApiDocs.termsOfServiceUrl;

        private String contactName = ApplicationDefaults.ApiDocs.contactName;
        @URL private String contactUrl = ApplicationDefaults.ApiDocs.contactUrl;
        @Email private String contactEmail = ApplicationDefaults.ApiDocs.contactEmail;
        private String license = ApplicationDefaults.ApiDocs.license;
        @URL private String licenseUrl = ApplicationDefaults.ApiDocs.licenseUrl;
        @Valid private List<@Valid Server> servers = List.of();

        @Getter
        @Setter
        public static class Server {
            @URL private String url;
            private String description;
        }
    }

    @Getter
    @Setter
    public static class Cache {
        @Valid private final Caffeine caffeine = new Caffeine();
    }

    @Getter
    @Setter
    public static class Caffeine {
        @DurationMin(seconds = 1)
        @NotNull
        private Duration ttl = ApplicationDefaults.Cache.Caffeine.ttl;

        @Min(1)
        private int initialCapacity = ApplicationDefaults.Cache.Caffeine.initialCapacity;

        @Min(1)
        private long maximumSize = ApplicationDefaults.Cache.Caffeine.maximumSize;
    }
}
