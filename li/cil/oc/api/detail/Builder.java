package li.cil.oc.api.detail;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;

public interface Builder<T extends Node> {
    T create();

    public static interface NodeBuilder extends Builder<Node> {
        ComponentBuilder withComponent(String name);

        ConnectorBuilder withConnector(double bufferSize);
    }

    static interface ComponentBuilder extends Builder<Component> {
        ComponentConnectorBuilder withConnector(double bufferSize);
    }

    static interface ConnectorBuilder extends Builder<Connector> {
        ComponentConnectorBuilder withComponent(String name);
    }

    static interface ComponentConnectorBuilder extends Builder<ComponentConnector> {
    }
}
