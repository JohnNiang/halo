package run.halo.app.theme;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.utils.PathUtils;

/**
 * @author guqing
 * @since 2.0.0
 */
public class ThemeLinkBuilder extends StandardLinkBuilder {
    public static final String THEME_ASSETS_PREFIX = "/assets";
    public static final String THEME_PREVIEW_PREFIX = "/themes";

    private final ThemeContext theme;
    private final ExternalUrlSupplier externalUrlSupplier;

    public ThemeLinkBuilder(ThemeContext theme, ExternalUrlSupplier externalUrlSupplier) {
        this.theme = theme;
        this.externalUrlSupplier = externalUrlSupplier;
    }

    @Override
    protected String processLink(IExpressionContext context, String link) {
        // note: the link might be context-relative link and never be null
        if (!Contexts.isWebContext(context)) {
            return link;
        }
        if (link == null || !linkInSite(externalUrlSupplier.get(), link)) {
            return link;
        }

        if (StringUtils.isBlank(link)) {
            link = "/";
        }
        var webContext = Contexts.asWebContext(context);
        var contextPath = webContext.getExchange().getRequest().getApplicationPath();

        if (isAssetsRequest(link, contextPath)) {
            if ("/".equals(contextPath)) {
                return PathUtils.combinePath(THEME_PREVIEW_PREFIX, theme.getName(), link);
            }
            return PathUtils.combinePath(
                contextPath,
                THEME_PREVIEW_PREFIX,
                theme.getName(),
                StringUtils.removeStart(link, contextPath)
            );
        }

        // not assets link
        if (theme.isActive()) {
            return link;
        }

        return UriComponentsBuilder.fromUriString(link)
            .queryParam(ThemeContext.THEME_PREVIEW_PARAM_NAME, theme.getName())
            .build().toString();
    }

    static boolean linkInSite(@NonNull URI externalUri, @NonNull String link) {
        if (!PathUtils.isAbsoluteUri(link)) {
            // relative uri is always in site
            return true;
        }
        try {
            URI requestUri = new URI(link);
            return StringUtils.equals(externalUri.getAuthority(), requestUri.getAuthority());
        } catch (URISyntaxException e) {
            // ignore this link
        }
        return false;
    }

    private boolean isAssetsRequest(String link, String contextPath) {
        var prefix =
            "/".equals(contextPath) ? THEME_ASSETS_PREFIX : contextPath + THEME_ASSETS_PREFIX;
        return link.startsWith(prefix);
    }
}
