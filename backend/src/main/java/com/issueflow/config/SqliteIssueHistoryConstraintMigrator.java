package com.issueflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SqliteIssueHistoryConstraintMigrator implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqliteIssueHistoryConstraintMigrator.class);

    private final DataSource dataSource;

    public SqliteIssueHistoryConstraintMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            String tableSql = tableSql(statement);
            if (tableSql == null) {
                return;
            }
            String normalized = tableSql.toUpperCase(Locale.ROOT);
            if (!normalized.contains("CHECK") || normalized.contains("ESCALATION_NOTIFICATION_QUEUED")) {
                return;
            }

            LOGGER.info("Updating issue_history to accept escalation notification history events");
            statement.execute("PRAGMA foreign_keys=off");
            statement.execute("""
                    CREATE TABLE issue_history_migrated (
                        created_at timestamp not null,
                        id integer,
                        issue_id bigint not null,
                        event_type varchar(60) not null,
                        description varchar(1000),
                        new_value varchar(255),
                        old_value varchar(255),
                        primary key (id)
                    )
                    """);
            statement.execute("""
                    INSERT INTO issue_history_migrated (
                        created_at, id, issue_id, event_type, description, new_value, old_value
                    )
                    SELECT created_at, id, issue_id, event_type, description, new_value, old_value
                    FROM issue_history
                    """);
            statement.execute("DROP TABLE issue_history");
            statement.execute("ALTER TABLE issue_history_migrated RENAME TO issue_history");
            statement.execute("PRAGMA foreign_keys=on");
        }
    }

    private static String tableSql(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'issue_history'"
        )) {
            if (!resultSet.next()) {
                return null;
            }
            return resultSet.getString("sql");
        }
    }
}
