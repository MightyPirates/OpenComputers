package li.cil.oc.api.prefab.network;

import li.cil.oc.api.network.ManagedEnvironment;

public abstract class AbstractManagedEnvironment extends AbstractEnvironment implements ManagedEnvironment {
    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }
}
