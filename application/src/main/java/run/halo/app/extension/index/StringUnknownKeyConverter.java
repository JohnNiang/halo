package run.halo.app.extension.index;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class StringUnknownKeyConverter implements Converter<String, IndexAttributeFactory.UnknownKey> {

    @Override
    public IndexAttributeFactory.UnknownKey convert(String source) {
        return new IndexAttributeFactory.UnknownKey(source);
    }

}
