package io.softwareecg.awb.repository.adapter.clickhouse;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ClickHouseRepositoryNamingTest {

    @Test
    public void shouldKeepConfiguredTableName() {
        assertThat(ClickHouseRepositoryNaming.resolveTableName("metrics_table", "My Repo", "repo-1"),
                equalTo("metrics_table"));
    }

    @Test
    public void shouldDeriveTableNameFromRepositoryName() {
        assertThat(ClickHouseRepositoryNaming.resolveTableName(null, "My ClickHouse Repo 01", "repo-1"),
                equalTo("my_clickhouse_repo_01"));
    }

    @Test
    public void shouldUseFallbackPrefixIfNameStartsWithDigit() {
        assertThat(ClickHouseRepositoryNaming.resolveTableName(null, "123 Repo", "repo-1"),
                equalTo("ekg_123_repo"));
    }
}
