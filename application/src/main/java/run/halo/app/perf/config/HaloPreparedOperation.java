package run.halo.app.perf.config;

import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.r2dbc.core.binding.Bindings;

/**
 * A custom implementation of {@link PreparedOperation} for {@link Select} statements.
 *
 * @author johnniang
 */
public class HaloPreparedOperation implements PreparedOperation<Select> {

    private final Select source;

    private final RenderContext renderContext;

    private final Bindings bindings;

    public HaloPreparedOperation(Select source, RenderContext renderContext, Bindings bindings) {
        this.source = source;
        this.renderContext = renderContext;
        this.bindings = bindings;
    }

    @Override
    public Select getSource() {
        return this.source;
    }

    @Override
    public void bindTo(BindTarget target) {
        this.bindings.apply(target);
    }

    @Override
    public String toQuery() {
        return SqlRenderer.create(this.renderContext).render(this.source);
    }
}
