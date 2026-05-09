package run.halo.app.theme.halot;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.infra.SystemConfigFetcher;
import run.halo.app.infra.SystemSetting;
import run.halo.app.infra.ThemeRootGetter;
import run.halo.app.theme.ThemeResolver;
import run.halo.app.theme.finders.FinderRegistry;
import run.halo.halot.HalotEngine;

/**
 * Spring {@link ViewResolver} that resolves views to Halot templates when a matching {@code .halot} file exists in the
 * active theme's {@code templates/} directory.
 *
 * <p>Falls through to Thymeleaf (by returning {@code Mono.empty()}) when no Halot template is found, preserving full
 * backward compatibility.
 *
 * @author johnniang
 */
public class HalotViewResolver implements ViewResolver, Ordered {

    private static final String HALOT_SUFFIX = ".halot";

    private final ThemeRootGetter themeRoot;
    private final SystemConfigFetcher configFetcher;
    private final FinderRegistry finderRegistry;
    private final ThemeResolver themeResolver;
    private final HalotEngineFactory engineFactory;

    public HalotViewResolver(
            ThemeRootGetter themeRoot,
            SystemConfigFetcher configFetcher,
            FinderRegistry finderRegistry,
            ThemeResolver themeResolver,
            HalotEngineFactory engineFactory) {
        this.themeRoot = themeRoot;
        this.configFetcher = configFetcher;
        this.finderRegistry = finderRegistry;
        this.themeResolver = themeResolver;
        this.engineFactory = engineFactory;
    }

    @Override
    public Mono<View> resolveViewName(String viewName, Locale locale) {
        return configFetcher
                .fetch(SystemSetting.Theme.GROUP, SystemSetting.Theme.class)
                .mapNotNull(SystemSetting.Theme::getActive)
                .flatMap(activeTheme -> {
                    Path themeDir = themeRoot.get().resolve(activeTheme);
                    var loader = new ThemeHalotTemplateLoader(themeDir, HALOT_SUFFIX);
                    if (loader.templateExists(viewName)) {
                        return Mono.<View>just(new HalotView(viewName, finderRegistry, themeResolver, engineFactory));
                    }
                    return Mono.empty();
                });
    }

    @Override
    public int getOrder() {
        // Run before Thymeleaf's HaloViewResolver (LOWEST_PRECEDENCE - 5)
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /** A Spring {@link View} that renders Halot templates. */
    private static class HalotView implements View {

        private static final DefaultDataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();

        private final String viewName;
        private final FinderRegistry finderRegistry;
        private final ThemeResolver themeResolver;
        private final HalotEngineFactory engineFactory;

        HalotView(
                String viewName,
                FinderRegistry finderRegistry,
                ThemeResolver themeResolver,
                HalotEngineFactory engineFactory) {
            this.viewName = viewName;
            this.finderRegistry = finderRegistry;
            this.themeResolver = themeResolver;
            this.engineFactory = engineFactory;
        }

        @Override
        public Mono<Void> render(Map<String, ?> model, MediaType contentType, ServerWebExchange exchange) {
            return themeResolver.getTheme(exchange).flatMap(theme -> {
                HalotEngine engine = engineFactory.getEngine(theme);

                // Merge Finder objects into the model — Halot accesses them as
                // {{ postFinder.getByName('hello') }} etc.
                Map<String, Object> mergedModel = new HashMap<>(model);
                finderRegistry.getFinders().forEach(mergedModel::putIfAbsent);

                Flux<DataBuffer> buffers = engine.render(viewName, mergedModel)
                        .map(content -> BUFFER_FACTORY.wrap(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

                if (contentType != null) {
                    exchange.getResponse().getHeaders().setContentType(contentType);
                } else {
                    exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_HTML);
                }
                return exchange.getResponse().writeWith(buffers);
            });
        }
    }
}
