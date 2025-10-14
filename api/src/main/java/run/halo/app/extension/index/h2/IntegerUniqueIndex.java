package run.halo.app.extension.index.h2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = IntegerUniqueIndex.TABLE_NAME)
public class IntegerUniqueIndex extends AbstractIndex<Integer> {

    public static final String TABLE_NAME = "integer_unique_index";

}
