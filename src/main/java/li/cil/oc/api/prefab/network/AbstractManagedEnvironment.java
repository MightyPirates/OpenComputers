package li.cil.oc.api.prefab.network;

import li.cil.oc.api.network.EnvironmentItem;

public abstract class AbstractManagedEnvironment extends AbstractEnvironment implements EnvironmentItem {
    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }
}
