package li.cil.oc.api.network;

/**
 * This kind of environment is managed by an adapter for environments provided
 * for adjacent blocks by a {@link li.cil.oc.api.driver.DriverBlock}.
 */
public interface EnvironmentBlock extends Environment {
    void onDispose();
}
