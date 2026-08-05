package py.com.logixone.plugin.api;

/**
 * Neutral SPI implemented by a physically present plugin and adapted by runtime discovery.
 *
 * <p>A deployable implementation is a public concrete type with a public no-argument
 * constructor. Its JAR exposes the same type to CDI and through the standard
 * {@code META-INF/services/py.com.logixone.plugin.api.PluginDefinition} provider file.
 */
@FunctionalInterface
public interface PluginDefinition {

    PluginDescriptor descriptor();
}
