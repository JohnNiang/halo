package run.halo.app.theme.dialect;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import reactor.core.publisher.Mono;

@Component
class ContextPathRewriteElementTagPostProcessor implements ElementTagPostProcessor {

    @Override
    public Mono<IProcessableElementTag> process(ITemplateContext context,
        IProcessableElementTag tag) {
        if (!Contexts.isWebContext(context)) {
            return Mono.empty();
        }
        var webContext = Contexts.asWebContext(context);
        var contextPath = webContext.getExchange().getRequest().getApplicationPath();
        var srcAttr = tag.getAttribute("src");
        var modelFactory = context.getModelFactory();
        if (srcAttr != null) {
            var srcValue = srcAttr.getValue();
            srcValue = refineUri(srcValue, contextPath);
            tag = modelFactory.setAttribute(tag, "src", srcValue);
        }
        var hrefAttr = tag.getAttribute("href");
        if (hrefAttr != null) {
            var hrefValue = hrefAttr.getValue();
            hrefValue = refineUri(hrefValue, contextPath);
            tag = modelFactory.setAttribute(tag, "href", hrefValue);
        }
        return Mono.just(tag);
    }

    private static String refineUri(String uri, String contextPath) {
        if (uri.startsWith(contextPath) || "/".equals(contextPath)) {
            return uri;
        }
        return contextPath + "/" + StringUtils.removeStart(uri, "/");
    }

}
