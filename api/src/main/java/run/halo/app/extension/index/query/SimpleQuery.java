package run.halo.app.extension.index.query;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

@Getter
public abstract class SimpleQuery implements Query {

    protected final String fieldName;

    protected final Object value;

    protected SimpleQuery(String fieldName, Object value) {
        Assert.isTrue(StringUtils.isNotBlank(fieldName), "fieldName cannot be blank.");
        this.fieldName = fieldName;
        this.value = value;
    }
}
