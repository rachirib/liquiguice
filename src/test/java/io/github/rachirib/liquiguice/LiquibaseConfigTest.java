package io.github.rachirib.liquiguice;

import static io.github.rachirib.liquiguice.LiquibaseConfig.Builder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.resource.ResourceAccessor;
import liquibase.sdk.resource.MockResourceAccessor;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.hsqldb.jdbc.JDBCDataSource;
import org.hsqldb.jdbc.JDBCPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LiquibaseConfigTest {

  @Test
  public void shouldCreateConfigByBuilder() throws Exception {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH)
        .withDropFirst(Fixtures.DROP_FIRST)
        .withShouldRun(Fixtures.SHOULD_RUN)
        .withResourceAccessor(Fixtures.RESOURCE_ACCESSOR)
        .withContexts(Fixtures.CONTEXT)
        .withLabels(Fixtures.LABELS)
        .withParameters(Fixtures.PARAMETERS);

    LiquibaseConfig config = builder.build();

    assertThat(config, not(sameInstance(builder.build())));
    assertThat(config, is(builder.build()));
    assertThat(config.getDataSource(), is(Fixtures.DATA_SOURCE));
    assertThat(config.getChangeLogPath(), is(Fixtures.CHANGELOG_PATH));
    assertThat(config.getResourceAccessor(), is(Fixtures.RESOURCE_ACCESSOR));
    assertThat(config.getContexts(), hasSize(Fixtures.CONTEXT.size()));
    assertThat(config.getContexts(), containsInAnyOrder(Fixtures.CONTEXT.toArray(new String[0])));
    assertThat(config.getLabels(), hasSize(Fixtures.LABELS.size()));
    assertThat(config.getLabels(), containsInAnyOrder(Fixtures.LABELS.toArray(new String[0])));
    assertThat(config.getParameters().size(), is(Fixtures.PARAMETERS.size()));
    Fixtures.PARAMETERS.forEach(
        (key, value) -> assertThat(config.getParameters(), hasEntry(key, value)));
  }

  @Test
  public void shouldCreateDifferentBuilders() throws Exception {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH)
        .withDropFirst(Fixtures.DROP_FIRST)
        .withShouldRun(Fixtures.SHOULD_RUN)
        .withResourceAccessor(Fixtures.RESOURCE_ACCESSOR)
        .withContexts(Fixtures.CONTEXT)
        .withLabels(Fixtures.LABELS)
        .withParameters(Fixtures.PARAMETERS);

    assertThat(builder, is(Builder.of(builder)));
    assertThat(builder, not(sameInstance(Builder.of(builder))));
    assertThat(builder, is(Builder.of(builder).withContext("")));
    assertThat(builder, not(is(Builder.of(builder).withContext("X"))));
    assertThat(builder, is(Builder.of(builder).withLabel("")));
    assertThat(builder, not(is(Builder.of(builder).withLabel("X"))));
    assertThat(builder, is(Builder.of(builder).withParameter("", "")));
    assertThat(builder, not(is(Builder.of(builder).withParameter("k", "v"))));
  }

  @Test
  public void shouldThrowExceptionForBuilderWithEmptyChangeLogPath() {
    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Builder.of(Fixtures.DATA_SOURCE)
            .withChangeLogPath("")
            .build());

    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("changeLogPath must be defined."));
  }

  @Test
  public void shouldThrowExceptionForBuilderWithNotDefinedChangeLogPath() {

    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
            () -> Builder.of(Fixtures.DATA_SOURCE)
                    .withChangeLogPath(null)
                    .build());

    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("changeLogPath must be defined."));
  }

  @Test
  public void shouldThrowExceptionForBuilderWithNotDefinedResourceAccessor() {
    NullPointerException e = Assertions.assertThrows(NullPointerException.class,
            () -> Builder.of(Fixtures.DATA_SOURCE)
            .withResourceAccessor(null)
            .build());
    assertThat(e, notNullValue());
    assertThat(e.getMessage(), containsString("resourceAccessor must be defined."));
  }

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(LiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

  @SuppressFBWarnings("EC_NULL_ARG")
  @Test
  public void shouldPassEqualsAndHashCodeContractsForBuilder() throws Exception {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE);
    Builder builderClone = Builder.of(builder)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH);

    assertThat(builder.hashCode(), is(builder.hashCode()));
    assertThat(builder, is(builder));
    assertThat(builder.hashCode(), is(builderClone.hashCode()));
    assertThat(builder.hashCode(), not(is(Builder.of(new JDBCPool()))));
    assertThat(builder, not(is(builderClone)));
    assertThat(builder.equals(null), is(false));
  }

  @Test
  public void shouldPassHashCodeForBuilder() throws Exception {
    DataSource firstDataSource = mock(DataSource.class);
    DataSource secondDataSource = mock(DataSource.class);

    assertThat(Builder.of(firstDataSource).hashCode(),
        not(is(Builder.of(secondDataSource).hashCode())));
    assertThat(Builder.of(firstDataSource).hashCode(), is(Builder.of(firstDataSource).hashCode()));
  }

  @Test
  public void verifyToString() throws Exception {
    ToStringVerifier.forClass(LiquibaseConfig.class).withClassName(NameStyle.SIMPLE_NAME)
        .withIgnoredFields("dataSource", "resourceAccessor").verify();
  }

  private static final class Fixtures {

    private static final DataSource DATA_SOURCE = new JDBCDataSource();
    private static final String CHANGELOG_PATH = "changelog/path";
    private static final boolean DROP_FIRST = true;
    private static final boolean SHOULD_RUN = true;
    private static final ResourceAccessor RESOURCE_ACCESSOR = new MockResourceAccessor();
    private static final Collection<String> CONTEXT = Lists.newArrayList("context1", "context2");
    private static final Collection<String> LABELS = Lists.newArrayList("label1", "label2");
    private static final Map<String, String> PARAMETERS = ImmutableMap.of("k1", "v1", "k2", "v2");
  }
}
