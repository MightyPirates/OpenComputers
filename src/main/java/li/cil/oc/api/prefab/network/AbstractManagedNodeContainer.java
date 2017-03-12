package li.cil.oc.api.prefab.network;

import li.cil.oc.api.network.NodeContainerItem;

public abstract class AbstractManagedNodeContainer extends AbstractNodeContainer implements NodeContainerItem {
    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }
}
