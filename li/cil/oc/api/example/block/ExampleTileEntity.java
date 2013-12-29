package li.cil.oc.api.example.block;


import li.cil.oc.api.Network;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.network.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class ExampleTileEntity extends TileEntity implements Environment/*or ManagedEnvironment*/, Persistable { //if you don't need any of these you can ignore them

    /**
     * ********************************************Environment***************************
     */
    private Node node;

    public ExampleTileEntity() {
        node = Network.newNode(this, Visibility.Network)
                .withConnector()
                .withComponent("ExampleComponent")
                .create();
    }

    protected boolean addedToNetwork = false;

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!addedToNetwork) {
            addedToNetwork = true;
            Network.joinOrCreateNetwork(this);
        }
    }


    // ----------------------------------------------------------------------- //


    @Override
    public Node node() {
        return node;
    }

    @Override
    public void onConnect(Node node) {

    }

    @Override
    public void onDisconnect(Node node) {

    }

    @Override
    public void onMessage(Message message) {

    }

    /**
     * ********************************************Persistable***************************
     */
    @Override
    public void load(NBTTagCompound nbt) {

    }

    @Override
    public void save(NBTTagCompound nbt) {

    }


    /**
     * ********************************************Custom methods***************************
     */
    //This is a example method that can be called from lua. The lua name and java name don't need
    //to be the same
    @LuaCallback("test")
    public Object[] testName(Context computer, Arguments arguments) {
        return new Object[]{"Hello", " Lua ", " Block "};
    }
}
