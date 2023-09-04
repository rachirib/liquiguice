package io.github.rachirib.liquiguice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import io.github.rachirib.liquiguice.annotation.LiquiGuiceConfiguration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.inject.Singleton;
import javax.sql.DataSource;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LiquiGuiceModuleTest {

  private DatabaseFactory orgDatabaseFactory;

  @BeforeAll
  public static void beforeClass() {
    try {
      Class.forName("org.hsqldb.jdbc.JDBCDriver");
    } catch (ClassNotFoundException exception) {
      throw new NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    orgDatabaseFactory = DatabaseFactory.getInstance();
  }

  @AfterEach
  public void tearDown() throws Exception {
    DatabaseFactory.setInstance(orgDatabaseFactory);
  }

  @Test
  public void shouldExecuteLiquibaseUpdateWithSingleConfiguration() throws Exception {
    Guice.createInjector(
        new LiquiGuiceModule(),
        Fixtures.SINGLE_DATA_SOURCE_MODULE);

    try (Connection connection = Fixtures.SINGLE_DATA_SOURCE.getConnection()) {
      try (PreparedStatement preparedStatement =
               connection.prepareStatement(Fixtures.GET_ALL_FROM_TABLE_FOR_TEST_QUERY)) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          assertThat(resultSet.next(), is(true));
          assertThat(resultSet.getInt(Fixtures.ID_COLUMN_NAME), is(Fixtures.EXPECTED_ID));
          assertThat(resultSet.getString(Fixtures.NAME_COLUMN_NAME),
              is(Fixtures.EXPECTED_NAME));
          assertThat(resultSet.getBoolean(Fixtures.ACTIVE_COLUMN_NAME),
              is(Fixtures.EXPECTED_ACTIVE));
          assertThat(resultSet.next(), is(false));
        }
      }
    }
  }

  @Test
  public void shouldExecuteLiquibaseUpdateWithMultipleConfigurations() throws Exception {
    Guice.createInjector(
        new LiquiGuiceModule(),
        Fixtures.MULTI_DATA_SOURCE_MODULE);

    try (Connection connection = Fixtures.MULTI_DATA_SOURCE.getConnection()) {
      try (PreparedStatement preparedStatement = connection.prepareStatement(
          Fixtures.GET_ALL_FROM_TABLE_FOR_MULTI_TESTS_QUERY)) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          assertThat(resultSet.next(), is(true));
          assertThat(resultSet.getInt(Fixtures.ID_COLUMN_NAME), is(Fixtures.EXPECTED_ID));
          assertThat(resultSet.getString(Fixtures.NAME_COLUMN_NAME),
              is(Fixtures.EXPECTED_NAME));
          assertThat(resultSet.next(), is(false));
        }
      }
    }
  }

  @Test
  public void shouldNotExecuteUpdateWhenShouldRunIsDisabled() {
    DataSource dataSource = mock(DataSource.class);

    Guice.createInjector(
        new LiquiGuiceModule(),
        new AbstractModule() {

          @Override
          protected void configure() {
            bind(LiquiGuiceConfig.class)
                .annotatedWith(LiquiGuiceConfiguration.class)
                .toInstance(LiquiGuiceConfig.Builder
                    .of(LiquibaseConfig.Builder
                        .of(dataSource).withShouldRun(false)
                        .build())
                    .build());
          }
        });

    verifyNoMoreInteractions(dataSource);
  }

  @Test
  public void shouldNotExecuteUpdateSecondTime() throws Exception {
    Injector injector = Guice.createInjector(
        Stage.DEVELOPMENT, new LiquiGuiceModule(), Fixtures.DATA_SOURCE_MODULE);

    injector.getInstance(LiquiGuiceModule.LiquibaseEngine.class)
        .process();

    injector.getInstance(Key.get(LiquiGuiceConfig.class, LiquiGuiceConfiguration.class))
        .getConfigs()
        .forEach(liquibaseConfig -> {
          try {
            DataSource dataSource = liquibaseConfig.getDataSource();
            verify(dataSource, only()).getConnection();
          } catch (SQLException ex) {
            fail();
          }
        });
  }

  @Test
  public void shouldThrowExceptionForNotDefinedRequiredBinding() {
    CreationException e = assertThrows(CreationException.class,
            () -> Guice.createInjector(new LiquiGuiceModule()));
    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("Unable to create injector"));
    assertThat(e.getMessage(), containsString("No implementation for"
            + " LiquiGuiceConfig annotated with"
            + " @LiquiGuiceConfiguration() was bound"));
  }

  @Test
  public void shouldThrowExceptionForNullConfigValue() {
    CreationException e = assertThrows(CreationException.class, () -> {
      Guice.createInjector(new LiquiGuiceModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(LiquiGuiceConfig.class)
                    .annotatedWith(LiquiGuiceConfiguration.class)
                    .toInstance(null);
          }
        });
    });
    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("Unable to create injector"));
    assertThat(e.getMessage(), containsString("Binding to null instances is not allowed."));
  }

  @Test
  public void shouldThrowExceptionForEmptyConfigurationSet() {
    CreationException e = assertThrows(CreationException.class, () -> {
      Guice.createInjector(new LiquiGuiceModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(LiquiGuiceConfig.class)
                    .annotatedWith(LiquiGuiceConfiguration.class)
                    .toInstance(
                            LiquiGuiceConfig.Builder
                                    .of()
                                    .build());
          }
        });
    });
    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("Injected configuration set is empty."));
    assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
  }

  @Test
  public void shouldThrowExceptionForNotDefinedDataSourceConnection() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(null);

    CreationException e = assertThrows(CreationException.class, () -> {
      Guice.createInjector(new LiquiGuiceModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(LiquiGuiceConfig.class)
                    .annotatedWith(LiquiGuiceConfiguration.class)
                    .toInstance(LiquiGuiceConfig.Builder.of()
                            .withLiquibaseConfig(
                                    LiquibaseConfig.Builder.of(dataSource).build())
                            .build());
          }
        });
    });
    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("Unable to create injector"));
    assertThat(e.getMessage(), containsString("DataSource returns null connection instance."));
    assertThat(e.getCause(), instanceOf(NullPointerException.class));
  }

  @Test
  public void shouldThrowExceptionWhenProblemOccurredDuringDatabaseCreation() {
    CreationException e = assertThrows(CreationException.class, () -> {
      DataSource dataSource = mock(DataSource.class);
      Connection connection = mock(Connection.class);
      when(dataSource.getConnection()).thenReturn(connection);
      when(connection.getMetaData()).thenThrow(new SQLException("My SQLException."));

      Guice.createInjector(
        new LiquiGuiceModule(),
        new AbstractModule() {

          @Override
          protected void configure() {
            bind(LiquiGuiceConfig.class)
                    .annotatedWith(LiquiGuiceConfiguration.class)
                    .toInstance(LiquiGuiceConfig.Builder
                            .of(LiquibaseConfig.Builder
                                    .of(dataSource)
                                    .build())
                            .build());
          }
        });

      verify(dataSource).getConnection();
      verify(connection).rollback();
      verify(connection).close();
      verifyNoMoreInteractions(connection, dataSource);
    });

    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("My SQLException."));
    assertThat(e.getCause(), instanceOf(UnexpectedLiquibaseException.class));
  }

  @Test
  public void shouldThrowExceptionWhenProblemOccurredDuringLiquibaseUpdate() throws Exception {

    CreationException e = assertThrows(CreationException.class, () -> {
      DataSource dataSource = mock(DataSource.class);
      Connection connection = mock(Connection.class);
      DatabaseFactory databaseFactory = mock(DatabaseFactory.class);
      Database database = mock(Database.class);
      when(dataSource.getConnection()).thenReturn(connection);
      when(databaseFactory.findCorrectDatabaseImplementation(any())).thenReturn(database);
      doThrow(new DatabaseException("Problem - Liquibase.")).when(database).rollback();
      DatabaseFactory.setInstance(databaseFactory);

      Guice.createInjector(new LiquiGuiceModule(), new AbstractModule() {
        @Override
        protected void configure() {
          bind(LiquiGuiceConfig.class).annotatedWith(LiquiGuiceConfiguration.class)
                  .toInstance(LiquiGuiceConfig.Builder
                          .of(LiquibaseConfig.Builder.of(dataSource).build())
                          .build());
        }
      });

      verify(dataSource).getConnection();
      verify(connection).rollback();
      verify(connection).close();
      verify(database).close();
      verifyNoMoreInteractions(connection, dataSource, database);
    });
    assertThat(e, notNullValue());
  }

  @Test
  public void shouldPassEqualsAndHashCodeContractsInGuiceLiquibaseEngine() {
    EqualsVerifier.forClass(getGuiceLiquibaseEngineClass())
        .usingGetClass()
        .withPrefabValues(
            LiquiGuiceConfig.class,
            LiquiGuiceConfig.Builder.of().build(),
            LiquiGuiceConfig.Builder.of(
                LiquibaseConfig.Builder.of(Fixtures.SINGLE_DATA_SOURCE)
                    .build())
                .build())
        .withPrefabValues(
            Monitor.class,
            new Monitor(true),
            new Monitor(false))
        .verify();
  }

  @Test
  public void verifyToStringInGuiceLiquibaseEngine() {
    ToStringVerifier.forClass(getGuiceLiquibaseEngineClass()).withClassName(NameStyle.SIMPLE_NAME)
        .withIgnoredFields("dataSource", "resourceAccessor").verify();
  }

  @SuppressWarnings("unchecked")
  private Class<LiquiGuiceModule.LiquibaseEngine> getGuiceLiquibaseEngineClass() {
    try {
      return (Class<LiquiGuiceModule.LiquibaseEngine>)
          Class.forName("io.github.rachirib.liquiguice.LiquiGuiceModule$GuiceLiquibaseEngine");
    } catch (ClassNotFoundException exception) {
      fail(exception.getMessage());
      throw new IllegalStateException(exception);
    }
  }

  private static final class Fixtures {

    private static final String ID_COLUMN_NAME = "id";
    private static final String NAME_COLUMN_NAME = "name";
    private static final String ACTIVE_COLUMN_NAME = "active";
    private static final String EXPECTED_NAME = "test";
    private static final String GET_ALL_FROM_TABLE_FOR_TEST_QUERY =
        "SELECT * FROM table_for_test";
    private static final String GET_ALL_FROM_TABLE_FOR_MULTI_TESTS_QUERY =
        "SELECT * FROM table_for_multi_test";
    private static final int EXPECTED_ID = 1;
    private static final boolean EXPECTED_ACTIVE = true;
    private static final DataSource SINGLE_DATA_SOURCE = createJdbcDataSource();
    private static final DataSource MULTI_DATA_SOURCE = createJdbcDataSource();
    private static final Module DATA_SOURCE_MODULE = new AbstractModule() {

      @Provides
      @Singleton
      @LiquiGuiceConfiguration
      private LiquiGuiceConfig createConfig() {
        return LiquiGuiceConfig.Builder.of(
            LiquibaseConfig.Builder.of(createJdbcDataSource())
                .build())
            .build();
      }

      @Override
      protected void configure() {
      }
    };
    private static final Module SINGLE_DATA_SOURCE_MODULE = new AbstractModule() {

      @Provides
      @Singleton
      @LiquiGuiceConfiguration
      private LiquiGuiceConfig createConfig() {
        return LiquiGuiceConfig.Builder.of(
            LiquibaseConfig.Builder.of(SINGLE_DATA_SOURCE)
                .build())
            .build();
      }

      @Override
      protected void configure() {
      }
    };
    private static final Module MULTI_DATA_SOURCE_MODULE = new AbstractModule() {

      @Provides
      @Singleton
      @LiquiGuiceConfiguration
      private LiquiGuiceConfig createConfig() {
        ClassLoader classLoader = getClass().getClassLoader();
        return LiquiGuiceConfig.Builder
            .of()
            .withLiquibaseConfig(
                LiquibaseConfig.Builder.of(MULTI_DATA_SOURCE)
                    .withChangeLogPath("liquibase/emptyChangeLog.xml")
                    .withResourceAccessor(new ClassLoaderResourceAccessor(classLoader))
                    .withDropFirst(false)
                    .build())
            .withLiquibaseConfig(
                LiquibaseConfig.Builder.of(MULTI_DATA_SOURCE)
                    .withChangeLogPath("liquibase/changeLogMulti.xml")
                    .withResourceAccessor(new ClassLoaderResourceAccessor(classLoader))
                    .withDropFirst(true)
                    .withParameters(ImmutableMap.of("testParameter", "testValue"))
                    .build())
            .build();
      }

      @Override
      protected void configure() {
      }
    };

    private static JDBCDataSource createJdbcDataSource() {
      JDBCDataSource dataSource = new JDBCDataSource();
      dataSource.setDatabase("jdbc:hsqldb:mem:" + UUID.randomUUID().toString());
      dataSource.setUser("SA");
      return spy(dataSource);
    }
  }
}
