package run.halo.app.extension.indexer.h2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(StringUniqueIndex.TABLE_NAME)
public class StringUniqueIndex extends AbstractIndex<String> {

    public static final String TABLE_NAME = "string_unique_index";

}
