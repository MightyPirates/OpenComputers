package li.cil.oc.api.detail;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.environment.Environment;
import net.minecraft.world.World;

public interface NetworkAPI {
    void joinOrCreateNetwork(World world, int x, int y, int z);

    Node createNode(Environment host, String name, Visibility visibility);

    Component createComponent(Node node);

    Node createProducer(Node node);

    Node createConsumer(Node node);
}