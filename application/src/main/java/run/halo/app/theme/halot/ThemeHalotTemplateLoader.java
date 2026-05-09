package run.halo.app.theme.halot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import reactor.core.publisher.Mono;
import run.halo.halot.loader.TemplateLoader;

/**
 * Loads Halot templates from the theme's filesystem directory.
 *
 * @author johnniang
 */
public class ThemeHalotTemplateLoader implements TemplateLoader {

    private static final String TEMPLATES_DIR = "templates";
    private static final String DEFAULT_SUFFIX = ".halot";

    private final Path templatesPath;
    private final String suffix;

    public ThemeHalotTemplateLoader(Path themePath) {
        this(themePath, DEFAULT_SUFFIX);
    }

    public ThemeHalotTemplateLoader(Path themePath, String suffix) {
        this.templatesPath = themePath.resolve(TEMPLATES_DIR);
        this.suffix = suffix;
    }

    @Override
    public Mono<String> load(String name) {
        return Mono.fromCallable(() -> {
            Path templatePath = templatesPath.resolve(name + suffix);
            if (!Files.isRegularFile(templatePath)) {
                return null;
            }
            try {
                return Files.readString(templatePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new TemplateLoadException("Failed to load template: " + templatePath, e);
            }
        });
    }

    /** Check if a Halot template exists for the given view name. */
    public boolean templateExists(String viewName) {
        return Files.isRegularFile(templatesPath.resolve(viewName + suffix));
    }

    public static class TemplateLoadException extends RuntimeException {
        public TemplateLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
