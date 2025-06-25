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
    public static class SetReadingConverter<T> implements Converter<String, Set<T>> {

        private final Class<T> elementType;

        public SetReadingConverter(Class<T> elementType) {
            this.elementType = elementType;
        }

        @Override
        public Set<T> convert(String source) {
            try {
                // var setType =
                //     MAPPER.getTypeFactory().constructCollectionType(Set.class, elementType);
                // return MAPPER.readValue(source, setType);
                return MAPPER.readValue(source, new TypeReference<Set<T>>() {
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
    public static class SetReadingPostgresConverter<T> implements Converter<Json, Set<T>> {

        private final SetReadingConverter<T> delegate;

        public SetReadingPostgresConverter(Class<T> elementType) {
            this.delegate = new SetReadingConverter<>(elementType);
        }

        @Override
        public Set<T> convert(Json source) {
            return delegate.convert(source.asString());
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
