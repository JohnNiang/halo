package run.halo.app.security.authentication.mfa;

import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import run.halo.app.security.authentication.SecurityConfigurer;

@Component
public class MfaSecurityConfigurer implements SecurityConfigurer {

    private final ServerSecurityContextRepository securityContextRepository;

    public MfaSecurityConfigurer(ServerSecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public void configure(ServerHttpSecurity http) {
        http.addFilterAfter(new TotpAuthenticationFilter(securityContextRepository), SecurityWebFiltersOrder.AUTHENTICATION);
    }

}
