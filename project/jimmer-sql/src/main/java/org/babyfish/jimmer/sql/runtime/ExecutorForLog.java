package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;

class ExecutorForLog implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorForLog.class);

    private static final String REQUEST = "===>";

    private static final String RESPONSE = "<===";

    private final Executor raw;

    static Executor wrap(Executor raw) {
        if (raw == null) {
            return new ExecutorForLog(DefaultExecutor.INSTANCE);
        }
        if (raw instanceof ExecutorForLog) {
            return raw;
        }
        return new ExecutorForLog(raw);
    }

    private ExecutorForLog(Executor raw) {
        this.raw = raw;
    }

    @Override
    public <R> R execute(@NotNull Args<R> args) {
        if (!LOGGER.isInfoEnabled()) {
            return raw.execute(args);
        }
        if (args.sqlClient.getSqlFormatter().isPretty()) {
            return prettyLog(args);
        }
        return simpleLog(args);
    }

    @Override
    public BatchContext executeBatch(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            @Nullable ImmutableProp generatedIdProp
    ) {
        return new BatchContextWrapper(
                raw.executeBatch(sqlClient, con, sql, generatedIdProp)
        );
    }

    @Override
    public void openCursor(
            long cursorId,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions,
            ExecutionPurpose purpose,
            @Nullable ExecutorContext ctx,
            JSqlClientImplementor sqlClient
    ) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Open cursor(").append(cursorId).append(')').append(REQUEST).append('\n');
        appendPrettyRequest(
                builder,
                sql,
                variables,
                variablePositions,
                purpose,
                ctx,
                sqlClient
        );
        LOGGER.info(builder.toString());
    }

    private <R> R simpleLog(Args<R> args) {
        ExecutorContext ctx = args.ctx;
        String sql = args.sql;
        List<Object> variables = args.variables;
        if (ctx == null) {
            LOGGER.info(
                    "jimmer> sql: " +
                            sql +
                            ", variables: " +
                            variables +
                            ", purpose: " +
                            args.purpose
            );
        } else {
            Logger logger = LoggerFactory.getLogger(ctx.getPrimaryElement().getClassName());
            logger.info(
                    "jimmer> sql: " +
                            sql +
                            ", variables: " +
                            variables +
                            ", purpose: " +
                            args.purpose
            );
            for (StackTraceElement element : ctx.getMatchedElements()) {
                logger.info(
                        "jimmer stacktrace-element)> {}",
                        element
                );
            }
        }
        return raw.execute(args);
    }

    private <R> R prettyLog(Args<R> args) {
        R result = null;
        Throwable throwable = null;
        long millis = System.currentTimeMillis();
        try {
            result = raw.execute(args);
        } catch (RuntimeException | Error ex) {
            throwable = ex;
        }
        millis = System.currentTimeMillis() - millis;
        int affectedRowCount = -1;
        char ch = args.sql.charAt(0);
        if ((ch == 'i' || ch == 'u' || ch == 'd') && result instanceof Integer) {
            affectedRowCount = (Integer)result;
        }

        StringBuilder builder = new StringBuilder();
        if (args.closingCursorId == null) {
            builder.append("Execute SQL").append(REQUEST).append('\n');
            appendPrettyRequest(
                    builder,
                    args.sql,
                    args.variables,
                    args.variablePositions,
                    args.purpose,
                    args.ctx,
                    args.sqlClient
            );
        }
        appendPrettyResponse(
                builder,
                affectedRowCount,
                throwable,
                millis
        );
        if (args.closingCursorId != null) {
            builder.append(RESPONSE).append("Close cursor(").append(args.closingCursorId).append(')');
        } else {
            Long currentCourseId = Cursors.currentCursorId();
            if (currentCourseId != null) {
                builder.append("CursorId: ").append(currentCourseId).append('\n');
            }
            builder.append(RESPONSE).append("Execute SQL");
        }

        LOGGER.info(builder.toString());

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException)throwable;
        }
        if (throwable != null) {
            throw (Error)throwable;
        }
        return result;
    }

    private void appendPrettyRequest(
            StringBuilder builder,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions,
            ExecutionPurpose purpose,
            ExecutorContext ctx,
            JSqlClientImplementor sqlClient
    ) {
        if (ctx != null) {
            builder.append("--- Business related stack trace information ---\n");
            for (StackTraceElement element : ctx.getMatchedElements()) {
                builder.append(element).append('\n');
            }
        }

        builder.append("Purpose: ").append(purpose).append('\n');

        builder.append("SQL: ");
        if (variablePositions == null) {
            builder.append(sql);
        } else {
            sqlClient.getSqlFormatter().append(
                    builder,
                    sql,
                    variables,
                    variablePositions
            );
        }
        builder.append('\n');
    }

    private void appendPrettyResponse(
            StringBuilder builder,
            int affectedRowCount,
            Throwable throwable,
            long millis
    ) {
        if (affectedRowCount != -1) {
            builder.append("Affected row count: ").append(affectedRowCount).append('\n');
        }
        if (throwable == null) {
            builder.append("JDBC response status: success\n");
        } else {
            builder.append("JDBC response status: failed<").append(throwable.getClass().getName()).append(">\n");
        }
        builder.append("Time cost: ").append(millis).append("ms\n");
    }

    private static class BatchContextWrapper implements BatchContext {

        private final BatchContext raw;

        private final StringBuilder builder;

        BatchContextWrapper(BatchContext raw) {
            this.raw = raw;
            this.builder = new StringBuilder(raw.sql());
        }

        @Override
        public String sql() {
            return raw.sql();
        }

        @Override
        public void add(List<Object> variables) {
            raw.add(variables);
            this.builder.append("batch variables: ").append(variables);
        }

        @Override
        public int[] execute() {
            LOGGER.info(builder.toString());
            return raw.execute();
        }

        @Override
        public Object[] generatedIds() {
            return raw.generatedIds();
        }

        @Override
        public void close() {
            raw.close();
        }
    }
}
