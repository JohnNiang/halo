package run.halo.app.extension.index.h2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = IntegerIndex.TABLE_NAME)
public class IntegerIndex extends AbstractIndex<Integer> {

    public static final String TABLE_NAME = "integer_index";

}
