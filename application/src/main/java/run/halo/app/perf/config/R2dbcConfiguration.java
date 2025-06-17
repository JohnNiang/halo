package run.halo.app.perf.config;

import io.r2dbc.spi.ConnectionFactory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import run.halo.app.perf.converter.AnnotationsConverters;
import run.halo.app.perf.converter.FinalizersConverters;

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
        var dialect = getDialect(connectionFactory);
        var converters = new ArrayList<>();
        if (dialect instanceof PostgresDialect) {
            converters.add(AnnotationsConverters.AnnotationsReadingPostgresConverter.INSTANCE);
            converters.add(AnnotationsConverters.AnnotationsWritingPostgresConverter.INSTANCE);
            converters.add(FinalizersConverters.FinalizersReadingPostgresConverter.INSTANCE);
            converters.add(FinalizersConverters.FinalizersWritingPostgresConverter.INSTANCE);
        } else {
            converters.add(AnnotationsConverters.AnnotationsWritingConverter.INSTANCE);
            converters.add(AnnotationsConverters.AnnotationsReadingConverter.INSTANCE);
            converters.add(FinalizersConverters.FinalizersWritingConverter.INSTANCE);
            converters.add(FinalizersConverters.FinalizersReadingConverter.INSTANCE);
        }
        return converters;
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
