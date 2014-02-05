package li.cil.oc.api.detail;

import li.cil.oc.api.network.*;

/**
 * Used for building {@link Node}s via {@link li.cil.oc.api.Network#newNode}.
 *
 * @param <T> the type of the node created by this builder.
 */
public interface Builder<T extends Node> {
    T create();

    public static interface NodeBuilder extends Builder<Node> {
        ComponentBuilder withComponent(final String name);

        ComponentBuilder withComponent(final String name, final Visibility visibility);

        ConnectorBuilder withConnector();

        ConnectorBuilder withConnector(final double bufferSize);
    }

    public static interface ComponentBuilder extends Builder<Component> {
        ComponentConnectorBuilder withConnector();

        ComponentConnectorBuilder withConnector(final double bufferSize);
    }

    public static interface ConnectorBuilder extends Builder<Connector> {
        ComponentConnectorBuilder withComponent(final String name);

        ComponentConnectorBuilder withComponent(final String name, final Visibility visibility);
    }

    public static interface ComponentConnectorBuilder extends Builder<ComponentConnector> {
    }
}
