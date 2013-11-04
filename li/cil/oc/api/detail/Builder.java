package li.cil.oc.api.detail;

import li.cil.oc.api.network.*;

public interface Builder<T extends Node> {
    T create();

    public static interface NodeBuilder extends Builder<Node> {
        ComponentBuilder withComponent(String name);

        ComponentBuilder withComponent(String name, Visibility visibility);

        ConnectorBuilder withConnector(double bufferSize);
    }

    static interface ComponentBuilder extends Builder<Component> {
        ComponentConnectorBuilder withConnector(double bufferSize);
    }

    static interface ConnectorBuilder extends Builder<Connector> {
        ComponentConnectorBuilder withComponent(String name);

        ComponentConnectorBuilder withComponent(String name, Visibility visibility);
    }

    static interface ComponentConnectorBuilder extends Builder<ComponentConnector> {
    }
}
