package io.github.rachirib.liquiguice;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import io.github.rachirib.liquiguice.annotation.LiquiGuiceConfiguration;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class Example {

  @SuppressWarnings("checkstyle:javadocmethod")
  @BeforeAll
  public static void beforeClass() throws Exception {
    try {
      Class.forName("org.hsqldb.jdbc.JDBCDriver");
    } catch (ClassNotFoundException exception) {
      throw new NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
    }
  }

  @Test
  public void shouldExecuteLiquibaseMigration() throws Exception {
    Injector injector = Guice.createInjector(
        new LiquiGuiceModule(), new MyLiquibaseConfigModule());

    Set<String> createdTables = getTablesFromDataSource(injector.getInstance(DataSource.class));
    assertThat(createdTables, hasSize(3));
    assertThat(createdTables, containsInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK",
            "EXAMPLE_TABLE"));
  }

  private Set<String> getTablesFromDataSource(DataSource dataSource) throws SQLException {
    Set<String> createdTables = Sets.newHashSet();
    try (Connection connection = dataSource.getConnection()) {
      try (ResultSet tables = connection.getMetaData()
          .getTables("PUBLIC", "PUBLIC", null, new String[] {"TABLE"})) {
        while (tables.next()) {
          createdTables.add(tables.getString("TABLE_NAME"));
        }
      }
    }
    return createdTables;
  }

  private static final class MyLiquibaseConfigModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Singleton
    @Provides
    private DataSource createDataSource() {
      JDBCDataSource dataSource = new JDBCDataSource();
      dataSource.setDatabase("jdbc:hsqldb:mem:" + UUID.randomUUID().toString());
      dataSource.setUser("SA");
      return dataSource;
    }

    @LiquiGuiceConfiguration
    @Provides
    @Inject
    private LiquiGuiceConfig createLiquibaseConfig(DataSource dataSource) {
      return LiquiGuiceConfig.Builder
          .of(LiquibaseConfig.Builder.of(dataSource)
              .withChangeLogPath("liquibase/exampleChangeLog.xml")
              .withResourceAccessor(new ClassLoaderResourceAccessor(getClass().getClassLoader()))
              .withDropFirst(true)
              .withContext("")
              .withLabel("")
              .withParameter("param", "value")
              .build())
          .build();
    }
  }
}
