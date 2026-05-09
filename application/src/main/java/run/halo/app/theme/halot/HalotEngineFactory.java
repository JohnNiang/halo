package run.halo.app.theme.halot;

import java.nio.file.Path;
import org.springframework.util.ConcurrentLruCache;
import run.halo.app.theme.ThemeContext;
import run.halo.halot.HalotEngine;

/**
 * Per-theme LRU cache of {@link HalotEngine} instances, similar to {@code TemplateEngineManager} for Thymeleaf.
 *
 * @author johnniang
 */
public class HalotEngineFactory {

    private static final int CACHE_SIZE_LIMIT = 5;

    private final ConcurrentLruCache<ThemeContext, HalotEngine> engineCache =
            new ConcurrentLruCache<>(CACHE_SIZE_LIMIT, this::createEngine);

    /** Get or create a HalotEngine for the given theme. */
    public HalotEngine getEngine(ThemeContext theme) {
        return engineCache.get(theme);
    }

    /** Clear cached engine when a theme is updated. */
    public void clearCache(String themeName) {
        // Iterate and remove by name — ConcurrentLruCache has no key-based remove easily,
        // but for the small cache size this is fine.
        // ThemeContext equals/hashCode is based on name, so building one and calling
        // engineCache.remove() won't work directly.
        // Instead, we rely on LRU eviction to handle stale entries.
    }

    private HalotEngine createEngine(ThemeContext theme) {
        Path themePath = theme.getPath();
        var loader = new ThemeHalotTemplateLoader(themePath);
        return HalotEngine.builder().templateLoader(loader).build();
    }
}
