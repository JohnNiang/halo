package run.halo.app.perf.config;

import io.r2dbc.spi.ConnectionFactory;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import run.halo.app.perf.converter.AnnotationsReadingConverter;
import run.halo.app.perf.converter.AnnotationsWritingConverter;
import run.halo.app.perf.converter.FinalizersReadingConverter;
import run.halo.app.perf.converter.FinalizersWritingConverter;

@EnableR2dbcAuditing
@Configuration(proxyBeanMethods = false)
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;

    public R2dbcConfiguration(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    @NonNull
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    @Override
    @NonNull
    protected List<Object> getCustomConverters() {
        return List.of(
            AnnotationsWritingConverter.INSTANCE,
            FinalizersWritingConverter.INSTANCE,
            AnnotationsReadingConverter.INSTANCE,
            FinalizersReadingConverter.INSTANCE
        );
    }

    @Bean
    ReactiveAuditorAware<String> haloAuditorAware() {
        var trustResolver = new AuthenticationTrustResolverImpl();
        return new ReactiveAuditorAware<>() {

            @Override
            @NonNull
            public Mono<String> getCurrentAuditor() {
                return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .filter(trustResolver::isAuthenticated)
                    .map(Authentication::getName)
                    .defaultIfEmpty("halo_system");
            }

        };
    }
}
