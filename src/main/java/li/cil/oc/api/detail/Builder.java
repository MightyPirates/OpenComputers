package li.cil.oc.api.detail;

import li.cil.oc.api.network.*;

/**
 * Used for building {@link Node}s via {@link li.cil.oc.api.Network#newNode}.
 *
 * @param <T> the type of the node created by this builder.
 */
public interface Builder<T extends Node> {
    /**
     * Finalizes the construction of the node.
     * <p/>
     * This performs the actual creation of the node, initializes it to the
     * settings defined by the current builder and returns it.
     *
     * @return the final node.
     */
    T create();

    /**
     * Builder for basic nodes. These nodes merely allow network access and
     * take on no special role.
     */
    public static interface NodeBuilder extends Builder<Node> {
        /**
         * Makes the node a component.
         * <p/>
         * Nodes that are components can be accessed from computers, methods
         * declared in them marked using the {@link Callback} annotation can
         * be invoked from computers that can see the component.
         *
         * @param name       the name of the component.
         * @param visibility the visibility of the component.
         * @return a builder for a node that is also a component.
         * @see li.cil.oc.api.network.Component
         */
        ComponentBuilder withComponent(String name, Visibility visibility);

        /**
         * Makes the node a component.
         * <p/>
         * Like {@link #withComponent(String, Visibility)}, but with a default
         * visibility set to the <em>reachability</em> of the node.
         *
         * @param name the name of the component.
         * @return a builder for a node that is also a component.
         * @see li.cil.oc.api.network.Component
         */
        ComponentBuilder withComponent(String name);

        /**
         * Makes the node a connector.
         * <p/>
         * A connector node can feed power into the network and extract power
         * from the network. This is used both for passive energy drain (such
         * as running screens and computers) and for active power consumption
         * (such as wireless message sending or robot actions).
         *
         * @param bufferSize the size of the local energy buffer.
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        ConnectorBuilder withConnector(double bufferSize);

        /**
         * Makes the node a connector.
         * <p/>
         * Like {@link #withConnector(double)}, but with a default buffer size
         * of zero.
         *
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        ConnectorBuilder withConnector();
    }

    /**
     * Builder for component nodes. These node can be interacted with from
     * computers in the same network, that can <em>see</em> the component.
     */
    public static interface ComponentBuilder extends Builder<Component> {
        /**
         * Makes the node a connector.
         * <p/>
         * A connector node can feed power into the network and extract power
         * from the network. This is used both for passive energy drain (such
         * as running screens and computers) and for active power consumption
         * (such as wireless message sending or robot actions).
         *
         * @param bufferSize the size of the local energy buffer.
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        ComponentConnectorBuilder withConnector(double bufferSize);

        /**
         * Makes the node a connector.
         * <p/>
         * Like {@link #withConnector(double)}, but with a default buffer size
         * of zero.
         *
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        ComponentConnectorBuilder withConnector();
    }

    /**
     * Builder for connector nodes. These nodes can interact with the energy
     * stored in the network, i.e. increase or reduce it.
     */
    public static interface ConnectorBuilder extends Builder<Connector> {
        /**
         * Makes the node a component.
         * <p/>
         * Nodes that are components can be accessed from computers, methods
         * declared in them marked using the {@link Callback} annotation can
         * be invoked from computers that can see the component.
         *
         * @param name       the name of the component.
         * @param visibility the visibility of the component.
         * @return a builder for a node that is also a component.
         * @see li.cil.oc.api.network.Component
         */
        ComponentConnectorBuilder withComponent(String name, Visibility visibility);

        /**
         * Makes the node a component.
         * <p/>
         * Like {@link #withComponent(String, Visibility)}, but with a default
         * visibility set to the <em>reachability</em> of the node.
         *
         * @param name the name of the component.
         * @return a builder for a node that is also a component.
         * @see li.cil.oc.api.network.Component
         */
        ComponentConnectorBuilder withComponent(String name);
    }

    /**
     * Builder for nodes that are both component <em>and</em> connector node.
     */
    public static interface ComponentConnectorBuilder extends Builder<ComponentConnector> {
    }
}
