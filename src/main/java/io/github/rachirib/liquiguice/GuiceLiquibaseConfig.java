package io.github.rachirib.liquiguice;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public final class GuiceLiquibaseConfig {

  private final Set<LiquibaseConfig> configs;

  private GuiceLiquibaseConfig(Collection<LiquibaseConfig> configs) {
    this.configs = ImmutableSet.copyOf(configs);
  }

  Set<LiquibaseConfig> getConfigs() {
    return configs;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    GuiceLiquibaseConfig that = (GuiceLiquibaseConfig) obj;
    return Objects.equals(configs, that.configs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configs);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GuiceLiquibaseConfig.class.getSimpleName() + "[", "]")
        .add("configs=" + configs)
        .toString();
  }

  /**
   * Builder for <code>GuiceLiquibaseConfig</code>.
   */
  public static final class Builder {

    private final Set<LiquibaseConfig> configs;

    private Builder(Set<LiquibaseConfig> configs) {
      this.configs = configs;
    }

    /**
     * Creates new builder for <code>GuiceLiquibaseConfig</code>.
     *
     * @return new Builder instance
     */
    public static Builder of() {
      return new Builder(Sets.newHashSet());
    }

    /**
     * Creates new builder for <code>GuiceLiquibaseConfig</code> with defined config element.
     *
     * @param config <code>LiquibaseConfig</code> instance added at the beginning
     * @return new Builder instance
     * @throws NullPointerException when config is null
     */
    public static Builder of(LiquibaseConfig config) {
      return new Builder(
          Sets.newHashSet(checkNotNull(config, "config must be defined.")));
    }

    /**
     * Adds <code>LiquibaseConfig</code> instance to the set of configuration.
     *
     * @param config <code>LiquibaseConfig</code> object - cannot be null
     * @return itself
     * @throws NullPointerException when config is null
     */
    public final Builder withLiquibaseConfig(LiquibaseConfig config) {
      configs.add(checkNotNull(config, "config must be defined."));
      return this;
    }

    /**
     * Adds <code>LiquibaseConfig</code> instances to the set of configuration.
     *
     * @param configs <code>LiquibaseConfig</code> objects collection without null elements
     * @return itself
     * @throws NullPointerException when null element is in the collection or collection is null
     */
    public final Builder withLiquibaseConfigs(Collection<LiquibaseConfig> configs) {
      checkNotNull(configs, "configs must be defined.")
          .forEach(this::withLiquibaseConfig);
      return this;
    }

    /**
     * Creates new <code>GuiceLiquibaseConfig</code> object from defined
     * <code>LiquibaseConfig</code> objects.
     *
     * @return new <code>GuiceLiquibaseConfig</code> object
     */
    public final GuiceLiquibaseConfig build() {
      return new GuiceLiquibaseConfig(configs);
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
      return Objects.equals(configs, builder.configs);
    }

    @Override
    public int hashCode() {
      return Objects.hash(configs);
    }
  }
}
