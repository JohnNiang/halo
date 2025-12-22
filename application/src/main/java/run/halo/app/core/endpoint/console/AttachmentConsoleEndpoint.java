package run.halo.app.core.endpoint.console;

import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.halo.app.core.endpoint.AttachmentHandler;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.core.extension.service.AttachmentService;
import run.halo.app.extension.GroupVersion;
import run.halo.app.infra.SystemConfigFetcher;
import run.halo.app.infra.SystemSetting;

@Slf4j
@Component
@RequiredArgsConstructor
class AttachmentConsoleEndpoint implements CustomEndpoint {

    private final SystemConfigFetcher systemConfigFetcher;

    private final AttachmentService attachmentService;

    private final AttachmentHandler attachmentHandler;

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.storage.halo.run/v1alpha1");
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = "AttachmentV1alpha1Console";
        return SpringdocRouteBuilder.route()
            .POST(
                path("/attachments/-/upload")
                    .and(contentType(MediaType.MULTIPART_FORM_DATA)),
                this::handleUpload,
                builder -> builder.operationId("uploadForConsole")
                    .tag(tag)
                    .description("Upload attachment endpoint for console.")
                    .requestBody(requestBodyBuilder()
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().implementation(UploadConsoleForm.class))
                        )
                    )
                    .description("Upload attachments from editor in console.")
                    .response(Builder.responseBuilder()
                        .implementation(Attachment.class)
                    )
            )
            .build();
    }

    private Mono<ServerResponse> handleUpload(ServerRequest serverRequest) {
        var getConfig = systemConfigFetcher.fetch(
                SystemSetting.Attachment.GROUP,
                SystemSetting.Attachment.class
            )
            .mapNotNull(SystemSetting.Attachment::console)
            .filter(ac -> StringUtils.hasText(ac.policyName()))
            .switchIfEmpty(Mono.error(() -> new ServerWebInputException(
                "Attachment system setting is not configured for console"
            )));
        return attachmentHandler.handleUpload(serverRequest, getConfig);
    }


    /**
     * Upload form from console. The file and url are mutually exclusive. If both are provided,
     * the file will be used.
     *
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadConsoleForm {

        /**
         * The file to upload. If not provided, the url will be used.
         */
        @Nullable
        private FilePart file;

        /**
         * The filename to use when uploading from url. If not provided, the filename will be
         * extracted from the url.
         */
        @Nullable
        private String filename;

        /**
         * The url to upload from. If not provided, the file will be used.
         */
        @Nullable
        private String url;

    }
}
