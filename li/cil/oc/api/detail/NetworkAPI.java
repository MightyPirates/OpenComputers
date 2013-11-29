package li.cil.oc.api.detail;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.tileentity.TileEntity;

public interface NetworkAPI {
    void joinOrCreateNetwork(TileEntity tileEntity);

    void joinNewNetwork(Node node);

    Builder.NodeBuilder newNode(Environment host, Visibility reachability);
}