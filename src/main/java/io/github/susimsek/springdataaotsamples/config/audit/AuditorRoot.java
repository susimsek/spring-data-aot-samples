package io.github.susimsek.springdataaotsamples.config.audit;

public class AuditorRoot {

    private final String username;

    public AuditorRoot(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
