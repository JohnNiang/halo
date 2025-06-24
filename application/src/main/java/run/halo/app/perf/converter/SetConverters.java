package run.halo.app.perf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.r2dbc.postgresql.codec.Json;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public enum SetConverters {
    ;

    public static final ObjectMapper MAPPER = JsonMapper.builder()
        .build();

    @ReadingConverter
    public enum SetReadingConverter implements Converter<String, Set<?>> {
        INSTANCE;

        @Override
        public Set<?> convert(String source) {
            try {
                return new ObjectMapper().readValue(source, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @WritingConverter
    public enum SetWritingConverter implements Converter<Set<?>, String> {
        INSTANCE;

        @Override
        public String convert(Set<?> source) {
            try {
                return MAPPER.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        }
    }

    @ReadingConverter
    public enum SetReadingPostgresConverter implements Converter<Json, Set<?>> {
        INSTANCE;

        @Override
        public Set<?> convert(Json source) {
            return SetReadingConverter.INSTANCE.convert(source.asString());
        }
    }

    @WritingConverter
    public enum SetWritingPostgresConverter
        implements Converter<Set<?>, Json> {

        INSTANCE;

        @Override
        public Json convert(Set<?> source) {
            var json = SetWritingConverter.INSTANCE.convert(source);
            return json == null ? null : Json.of(json);
        }
    }
}
