package run.halo.app.perf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public enum AnnotationsWritingConverter implements Converter<Map<String, String>, String> {

    INSTANCE;

    private final ObjectMapper mapper = JsonMapper.builder()
        .build();

    @Override
    public String convert(Map<String, String> source) {
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
