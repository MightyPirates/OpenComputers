package li.cil.oc.api.detail;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.world.World;

public interface NetworkAPI {
    void joinOrCreateNetwork(World world, int x, int y, int z);

    void joinNewNetwork(Node node);

    Builder.NodeBuilder newNode(Environment host, Visibility visibility);
}