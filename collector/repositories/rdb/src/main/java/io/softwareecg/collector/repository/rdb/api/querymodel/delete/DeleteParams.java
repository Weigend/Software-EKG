package io.softwareecg.collector.repository.rdb.api.querymodel.delete;

import io.softwareecg.collector.repository.rdb.api.querymodel.expr.Expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * todo insert comment here
 *
 * @since 06.08.2018
 */
public class DeleteParams {

    private final List<Expression> filterExpressions = new ArrayList<>();

    public DeleteParams addFilter(Expression filterExpression) {
        filterExpressions.add(filterExpression);
        return this;
    }

    public Collection<Expression> getFilterQueries() {
        return filterExpressions;
    }
}