package li.cil.oc.common.inventory;

public class ItemHandlerComponents extends ItemHandlerHosted {
    public ItemHandlerComponents(final ItemHandlerHost host, final int size) {
        super(host, size);
    }

    // ----------------------------------------------------------------------- //
    // ItemHandlerImpl

    @Override
    public int getSlotLimit(final int slot) {
        return 1;
    }
}
