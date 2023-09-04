package io.github.rachirib.liquiguice;

import static io.github.rachirib.liquiguice.GuiceLiquibaseConfig.Builder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import java.util.List;
import javax.sql.DataSource;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Test;

public class GuiceLiquibaseConfigTest {


  @Test
  public void shouldCreateEmptyConfigs() throws Exception {
    GuiceLiquibaseConfig config = Builder.of()
        .build();

    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), empty());
  }

  @Test
  public void shouldCreateConfigForSingleLiquibaseConfig() throws Exception {
    LiquibaseConfig liquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    GuiceLiquibaseConfig config = Builder.of(liquibaseConfig)
        .build();

    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), containsInAnyOrder(liquibaseConfig));
  }

  @Test
  public void shouldCreateConfigForMultipleLiquibaseConfigs() throws Exception {
    LiquibaseConfig firstLiquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    LiquibaseConfig secondLiquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    List<LiquibaseConfig> configs = Lists.newArrayList(firstLiquibaseConfig, secondLiquibaseConfig);
    GuiceLiquibaseConfig config = Builder.of()
        .withLiquibaseConfigs(configs)
        .build();

    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), hasSize(2));
    assertThat(config.getConfigs(),
            containsInAnyOrder(firstLiquibaseConfig, secondLiquibaseConfig));
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfig() {
    NullPointerException e = assertThrows(NullPointerException.class, () -> Builder.of(null));

    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("config must be defined."));
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfigAddedToBuilder() {
    NullPointerException e = assertThrows(NullPointerException.class, () -> Builder.of()
            .withLiquibaseConfig(null));

    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("config must be defined."));
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfigsAddedToBuilder() {
    NullPointerException e = assertThrows(NullPointerException.class, () -> Builder.of()
            .withLiquibaseConfigs(null));

    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("configs must be defined."));
  }

  @Test
  public void shouldThrowExceptionForConfigsWithNotDefinedElement() {
    NullPointerException e = assertThrows(NullPointerException.class, () -> {
      List<LiquibaseConfig> configs = Lists.newArrayList(
            LiquibaseConfig.Builder.of(new JDBCDataSource()).build(),
            null);
      Builder.of()
            .withLiquibaseConfigs(configs);
    });

    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("config must be defined."));
  }

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(GuiceLiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

  @Test
  public void shouldPassEqualsAndHashCodeContractsForBuilder() throws Exception {
    EqualsVerifier.forClass(Builder.class)
        .usingGetClass()
        .verify();
  }

  @Test
  public void verifyToString() throws Exception {
    ToStringVerifier.forClass(GuiceLiquibaseConfig.class)
            .withClassName(NameStyle.SIMPLE_NAME).verify();
  }
}
