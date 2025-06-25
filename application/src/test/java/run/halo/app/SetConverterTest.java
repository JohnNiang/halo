package run.halo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Set;
import org.junit.jupiter.api.Test;
import run.halo.app.perf.converter.SetConverters;

class SetConverterTest {

    @Test
    void deserializeTest() throws JsonProcessingException {
        var dependencies = SetConverters.MAPPER.readValue("""
                ["permission-a"]
                """,
            new TypeReference<Set<String>>() {
            });
        System.out.println(dependencies);
    }
}
