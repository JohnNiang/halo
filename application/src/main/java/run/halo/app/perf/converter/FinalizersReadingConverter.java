package run.halo.app.perf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

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
