package run.halo.app.theme.halot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.halo.app.infra.SystemConfigFetcher;
import run.halo.app.infra.ThemeRootGetter;
import run.halo.app.theme.ThemeResolver;
import run.halo.app.theme.finders.FinderRegistry;

/**
 * Spring configuration for Halot template engine integration.
 *
 * @author johnniang
 */
@Configuration
public class HalotConfiguration {

    @Bean
    HalotEngineFactory halotEngineFactory() {
        return new HalotEngineFactory();
    }

    @Bean
    HalotViewResolver halotViewResolver(
            ThemeRootGetter themeRoot,
            SystemConfigFetcher configFetcher,
            FinderRegistry finderRegistry,
            ThemeResolver themeResolver,
            HalotEngineFactory engineFactory) {
        return new HalotViewResolver(themeRoot, configFetcher, finderRegistry, themeResolver, engineFactory);
    }
}
