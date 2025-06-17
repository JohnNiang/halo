package run.halo.app.perf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.r2dbc.postgresql.codec.Json;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

public enum AnnotationsConverters {
    ;

    @ReadingConverter
    public enum AnnotationsReadingConverter implements Converter<String, Map<String, String>> {
        INSTANCE;

        private final ObjectMapper mapper = JsonMapper.builder()
            .build();

        @Override
        public Map<String, String> convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }

            try {
                return mapper.readValue(source, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @WritingConverter
    public enum AnnotationsWritingConverter implements Converter<Map<String, String>, String> {

        INSTANCE;

        private final ObjectMapper mapper = JsonMapper.builder()
            .build();

        @Override
        public String convert(@NonNull Map<String, String> source) {
            try {
                return mapper.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        }
    }

    @WritingConverter
    public enum AnnotationsWritingPostgresConverter
        implements Converter<Map<String, String>, Json> {
        INSTANCE;

        @Override
        public Json convert(@NonNull Map<String, String> source) {
            var json = AnnotationsWritingConverter.INSTANCE.convert(source);
            return json == null ? null : Json.of(json);
        }

    }

    @ReadingConverter
    public enum AnnotationsReadingPostgresConverter
        implements Converter<Json, Map<String, String>> {
        INSTANCE;

        @Override
        public Map<String, String> convert(@NonNull Json source) {
            return AnnotationsReadingConverter.INSTANCE.convert(source.asString());
        }

    }
}
