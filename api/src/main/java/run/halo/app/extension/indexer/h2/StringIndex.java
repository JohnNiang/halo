package run.halo.app.extension.indexer.h2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(StringIndex.TABLE_NAME)
public class StringIndex extends AbstractIndex<String> {

    public static final String TABLE_NAME = "string_index";

}
