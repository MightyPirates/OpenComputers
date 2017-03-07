package li.cil.oc.integration;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractManagedEnvironment;

public class ManagedTileEntityEnvironment<T> extends AbstractManagedEnvironment {
    protected final T tileEntity;

    public ManagedTileEntityEnvironment(final T tileEntity, final String name) {
        this.tileEntity = tileEntity;
        setNode(Network.newNode(this, Visibility.NETWORK).
                withComponent(name).
                create());
    }
}
