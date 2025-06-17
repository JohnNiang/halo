package run.halo.app.perf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.util.Set;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

public enum FinalizersConverters {
    ;

    @ReadingConverter
    public enum FinalizersReadingConverter implements Converter<String, Set<String>> {
        INSTANCE;

        private final ObjectMapper mapper = JsonMapper.builder()
            .build();

        @Override
        public Set<String> convert(String source) {
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
    public enum FinalizersWritingConverter implements Converter<Set<String>, String> {

        INSTANCE;

        private final ObjectMapper mapper = JsonMapper.builder()
            .build();

        @Override
        public String convert(Set<String> source) {
            if (source == null) {
                return null;
            }
            try {
                return mapper.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        }

    }

    @ReadingConverter
    public enum FinalizersReadingPostgresConverter implements Converter<Json, Set<String>> {
        INSTANCE;

        @Override
        public Set<String> convert(@NonNull Json source) {
            return FinalizersReadingConverter.INSTANCE.convert(source.asString());
        }
    }

    public enum FinalizersWritingPostgresConverter implements Converter<Set<String>, Json> {
        INSTANCE;

        @Override
        public Json convert(Set<String> source) {
            var json = FinalizersWritingConverter.INSTANCE.convert(source);
            return json == null ? null : Json.of(json);
        }
    }

}
