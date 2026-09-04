package com.issueflow.config;

import org.junit.jupiter.api.Test;
import org.sqlite.JDBC;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteIssueHistoryConstraintMigratorTest {

    @Test
    void rebuildsStaleLowercaseCheckConstraint() throws Exception {
        Path database = Files.createTempFile("issueflow-history", ".db");
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(JDBC.class);
        dataSource.setUrl("jdbc:sqlite:" + database.toAbsolutePath());

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE issue_history (
                        id integer,
                        created_at timestamp not null,
                        description varchar(1000),
                        event_type varchar(40) not null check (event_type in ('ISSUE_CREATED','STATUS_CHANGED')),
                        new_value varchar(255),
                        old_value varchar(255),
                        issue_id bigint not null,
                        primary key (id)
                    )
                    """);
            statement.execute("""
                    INSERT INTO issue_history (id, created_at, event_type, issue_id)
                    VALUES (1, '2026-09-04T12:00:00Z', 'ISSUE_CREATED', 10)
                    """);
        }

        new SqliteIssueHistoryConstraintMigrator(dataSource).run(null);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO issue_history (id, created_at, event_type, issue_id)
                    VALUES (2, '2026-09-04T12:01:00Z', 'ESCALATION_NOTIFICATION_QUEUED', 10)
                    """);
            try (ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM issue_history")) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isEqualTo(2);
            }
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
