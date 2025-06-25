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
import run.halo.app.core.extension.Role;

public enum RulesConverters {
    ;

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @ReadingConverter
    public enum RulesReadingConverter implements Converter<String, Set<Role.PolicyRule>> {
        INSTANCE;

        @Override
        public Set<Role.PolicyRule> convert(String source) {
            if (source.isBlank()) {
                return null;
            }
            try {
                return MAPPER.readValue(source, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @WritingConverter
    public enum RulesWritingConverter implements Converter<Set<Role.PolicyRule>, String> {
        INSTANCE;

        @Override
        public String convert(Set<Role.PolicyRule> source) {
            try {
                return MAPPER.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @ReadingConverter
    public enum RulesReadingPostgresConverter implements Converter<Json, Set<Role.PolicyRule>> {
        INSTANCE;

        @Override
        public Set<Role.PolicyRule> convert(Json source) {
            return RulesReadingConverter.INSTANCE.convert(source.asString());
        }
    }

    @WritingConverter
    public enum RulesWritingPostgresConverter implements Converter<Set<Role.PolicyRule>, Json> {
        INSTANCE;

        @Override
        public Json convert(Set<Role.PolicyRule> source) {
            var json = RulesWritingConverter.INSTANCE.convert(source);
            return json == null ? null : Json.of(json);
        }
    }

}
