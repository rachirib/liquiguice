package io.github.rachirib.liquiguice;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.sql.DataSource;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

public final class LiquibaseConfig {

  private final DataSource dataSource;
  private final String changeLogPath;
  private final ResourceAccessor resourceAccessor;
  private final boolean dropFirst;
  private final boolean shouldRun;
  private final Set<String> contexts;
  private final Set<String> labels;
  private final Map<String, String> parameters;

  /**
   * Creates new <code>LiquiBaseConfig</code> for defined DataSource, changelog file path and its
   * resource accessor and dropFirst switch.
   * <br>
   * http://www.liquibase.org/documentation/databasechangelog.html
   *
   * @param dataSource       DataSource where Liquibase will be running
   * @param changeLogPath    Liquibase changelog with all changesets
   * @param resourceAccessor Liquibase {@link ResourceAccessor} used for changelog file loading
   * @param dropFirst        Liquibase switch to drop all schemes and data in database
   * @param shouldRun        Liquibase switch to disable liquibase run
   * @param contexts         Liquibase contexts which will be used for changelog
   * @param labels           Liquibase labels
   * @param parameters       Liquibase parameters
   * @throws NullPointerException     when <code>dataSource</code>/<code>resourceAccessor</code> are
   *                                  null
   * @throws IllegalArgumentException when <code>changeLogPath</code> is null or empty
   */
  private LiquibaseConfig(DataSource dataSource,
                          String changeLogPath,
                          ResourceAccessor resourceAccessor,
                          boolean dropFirst,
                          boolean shouldRun, Collection<String> contexts,
                          Collection<String> labels,
                          Map<String, String> parameters) {
    this.dataSource = checkNotNull(dataSource, "dataSource must be defined.");
    this.resourceAccessor =
        checkNotNull(resourceAccessor, "resourceAccessor must be defined.");
    this.changeLogPath =
        checkNotNull(changeLogPath, "changeLogPath must be defined.");
    this.dropFirst = dropFirst;
    this.shouldRun = shouldRun;
    this.contexts = ImmutableSet.copyOf(checkNotNull(contexts));
    this.labels = ImmutableSet.copyOf(checkNotNull(labels));
    this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
  }

  DataSource getDataSource() {
    return dataSource;
  }

  public String getChangeLogPath() {
    return changeLogPath;
  }

  public ResourceAccessor getResourceAccessor() {
    return resourceAccessor;
  }

  public boolean isDropFirst() {
    return dropFirst;
  }

  public boolean isShouldRun() {
    return shouldRun;
  }

  public Set<String> getContexts() {
    return new HashSet<>(contexts);
  }

  public Set<String> getLabels() {
    return new HashSet<>(labels);
  }

  public Map<String, String> getParameters() {
    return new HashMap<>(parameters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LiquibaseConfig that = (LiquibaseConfig) obj;
    return Objects.equals(dataSource, that.dataSource)
        && Objects.equals(changeLogPath, that.changeLogPath)
        && Objects.equals(resourceAccessor, that.resourceAccessor)
        && (dropFirst == that.dropFirst) && (shouldRun == that.shouldRun)
        && Objects.equals(contexts, that.contexts)
        && Objects.equals(labels, that.labels)
        && Objects.equals(parameters, that.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.dataSource, this.changeLogPath, this.resourceAccessor, this.dropFirst,
            this.shouldRun, this.contexts, this.labels, this.parameters);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", LiquibaseConfig.class.getSimpleName() + "[", "]")
        .add("changeLogPath='" + changeLogPath + "'")
        .add("dropFirst=" + dropFirst)
        .add("shouldRun=" + shouldRun)
        .add("contexts=" + contexts)
        .add("labels=" + labels)
        .add("parameters=" + parameters)
        .toString();
  }

  /**
   * Builder for <code>LiquibaseConfig</code>.
   */
  public static final class Builder {

    private static final String DEFAULT_CHANGE_LOG_PATH = "liquibase/changeLog.xml";
    private static final Splitter CONTEXT_AND_LABEL_SPLITTER =
        Splitter.on(',').omitEmptyStrings().trimResults();
    private final DataSource dataSource;
    private String changeLogPath;
    private ResourceAccessor resourceAccessor;
    private boolean dropFirst;
    private boolean shouldRun;
    private Set<String> contexts;
    private Set<String> labels;
    private Map<String, String> parameters;

    private Builder(DataSource dataSource) {
      this.dataSource = dataSource;
      this.changeLogPath = DEFAULT_CHANGE_LOG_PATH;
      this.resourceAccessor = new ClassLoaderResourceAccessor(this.getClass().getClassLoader());
      this.dropFirst = false;
      this.shouldRun = true;
      this.contexts = new HashSet<>();
      this.labels = new HashSet<>();
      this.parameters = new HashMap<>();
    }

    /**
     * Creates new builder for <code>LiquibaseConfig</code> with defined DataSource element.
     *
     * @param dataSource <code>DataSource</code> used for Liquibase connection
     * @return new Builder instance
     * @throws NullPointerException when dataSource is null
     */
    public static Builder of(DataSource dataSource) {
      return new Builder(checkNotNull(dataSource, "dataSource must be defined."));
    }

    /**
     * Creates new builder for <code>LiquibaseConfig</code> as a copy of passed builder instance.
     *
     * @param builder instance used as a source for new builder
     * @return new Builder instance with all properties from passed one
     * @throws NullPointerException when builder argument is null
     */
    @VisibleForTesting
    static Builder of(Builder builder) {
      Builder copy = of(checkNotNull(builder, "builder cannot be null.").dataSource)
          .withChangeLogPath(builder.changeLogPath)
          .withDropFirst(builder.dropFirst)
          .withShouldRun(builder.shouldRun)
          .withResourceAccessor(builder.resourceAccessor);
      builder.contexts.forEach(copy::withContext);
      builder.labels.forEach(copy::withLabel);
      builder.parameters.forEach(copy::withParameter);
      return copy;
    }

    /**
     * Sets path to ChangeLog file used by Liquibase.
     *
     * @param value path as a String to ChangeLog file
     * @return itself
     */
    public final Builder withChangeLogPath(String value) {
      this.changeLogPath = value;
      return this;
    }

    /**
     * Sets <code>ResourceAccessor</code> used to find and load ChangeLog file.
     *
     * @param value <code>ResourceAccessor</code> instance
     * @return itself
     */
    public final Builder withResourceAccessor(ResourceAccessor value) {
      this.resourceAccessor = value;
      return this;
    }

    /**
     * Should drop all tables for used database at the beginning of Liquibase execution.
     *
     * @param value true/false flag
     * @return itself
     */
    public final Builder withDropFirst(boolean value) {
      this.dropFirst = value;
      return this;
    }

    /**
     * Default value to true, set to it to false to disable liquibase execution.
     *
     * @param value true/false flag
     * @return itself
     */
    public Builder withShouldRun(boolean value) {
      this.shouldRun = value;
      return this;
    }

    /**
     * Adds context which will be used in Liquibase changeSets execution. It will create a set of
     * contexts and pass it to Liquibase. Context can contains a set of contexts in format like
     * 'context1, context2, context3'. It will be split and added to the set. Null string will be
     * converted to empty one.
     *
     * @param value context
     * @return itself
     */
    public final Builder withContext(String value) {
      this.contexts.addAll(CONTEXT_AND_LABEL_SPLITTER.splitToList(Strings.nullToEmpty(value)));
      return this;
    }

    /**
     * Adds contexts from passed collection. Internally is using <code>withContext</code> for each
     * element of the collection.
     *
     * @param value contexts
     * @return itself
     * @throws NullPointerException when passed argument is null
     * @see LiquibaseConfig.Builder#withContext(String)
     */
    public final Builder withContexts(Collection<String> value) {
      checkNotNull(value, "contexts must be defined.")
          .forEach(this::withContext);
      return this;
    }

    /**
     * Adds label which will be used in Liquibase changeSets execution. It will create a set of
     * labels and pass it to Liquibase. Label can contains a set of labels in format like 'lab1,
     * lab2, lab3'. It will be split and added to the set. Null string will be converted to empty
     * one.
     *
     * @param value label
     * @return itself
     */
    public final Builder withLabel(String value) {
      this.labels.addAll(CONTEXT_AND_LABEL_SPLITTER.splitToList(Strings.nullToEmpty(value)));
      return this;
    }

    /**
     * Adds labels from passed collection. Internally is using <code>withLabel</code> for each
     * element of the collection.
     *
     * @param value labels
     * @return itself
     * @throws NullPointerException when passed argument is null
     * @see LiquibaseConfig.Builder#withLabel(String)
     */
    public final Builder withLabels(Collection<String> value) {
      checkNotNull(value, "labels must be defined.")
          .forEach(this::withLabel);
      return this;
    }

    /**
     * Adds parameter which will be used in Liquibase changeSets execution. It will create a map of
     * parameters that will be passed to Liquibase. Key as a null will be converted to empty
     * string.
     *
     * @param key   key of the parameter
     * @param value value of the parameter
     * @return itself
     */
    public final Builder withParameter(String key, String value) {
      if (!Strings.isNullOrEmpty(key)) {
        parameters.put(key, value);
      }
      return this;
    }

    /**
     * Adds parameters from map. Internally is using <code>withParameter</code> for each pair of the
     * map (key, value).
     *
     * @param map parameters
     * @return itself
     * @throws NullPointerException when passed map is null
     * @see LiquibaseConfig.Builder#withParameter(String, String)
     */
    public final Builder withParameters(Map<String, String> map) {
      checkNotNull(map, "parameters must be defined.")
          .forEach(this::withParameter);
      return this;
    }

    /**
     * Creates new <code>LiquibaseConfig</code> object for defined properties.
     *
     * @return new <code>LiquibaseConfig</code> object
     * @throws IllegalArgumentException when changeLogPath is null or empty
     * @throws NullPointerException     when resourceAccessor is null
     */
    public final LiquibaseConfig build() {
      checkArgument(
          !Strings.isNullOrEmpty(this.changeLogPath), "changeLogPath must be defined.");
      checkNotNull(this.resourceAccessor, "resourceAccessor must be defined.");
      return new LiquibaseConfig(
          this.dataSource,
          this.changeLogPath,
          this.resourceAccessor,
          this.dropFirst,
          this.shouldRun, this.contexts,
          this.labels,
          this.parameters);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Builder builder = (Builder) obj;
      return dropFirst == builder.dropFirst && shouldRun == builder.shouldRun
          && Objects.equals(dataSource, builder.dataSource)
          && Objects.equals(changeLogPath, builder.changeLogPath)
          && Objects.equals(resourceAccessor, builder.resourceAccessor)
          && Objects.equals(contexts, builder.contexts)
          && Objects.equals(labels, builder.labels)
          && Objects.equals(parameters, builder.parameters);
    }

    @Override
    public int hashCode() {
      return Objects.hash(dataSource);
    }
  }
}
