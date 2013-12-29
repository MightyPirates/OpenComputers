package li.cil.oc.api.example.component;


import li.cil.oc.api.Network;
import li.cil.oc.api.network.*;
import net.minecraft.nbt.NBTTagCompound;

public class ExampleItemEnvironment implements ManagedEnvironment {
    /**
     * ********************************************Default methods***************************
     */
    private Node node;

    public ExampleItemEnvironment() {
        //for a more detailed description read the documentation of each parameter
        node = Network
                //Visibility defines who can reach the component
                .newNode(this, Visibility.Network)
                        //if the component shall be reached in the component network,
                        // Visibility defines from where the component can be accessed with lua (
                .withComponent("ExampleItem", Visibility.Neighbors)
                        //if you want to have power
                .withConnector()
                        //finally creates the node required!
                .create();
    }


    @Override
    public Node node() {
        return node;
    }

    @Override
    public void onConnect(Node node) {
        //do stuff
    }

    @Override
    public void onDisconnect(Node node) {
        //do stuff

    }
    @Override
    public void onMessage(Message message) {
        //do stuff
    }

    @Override
    public boolean canUpdate() {
        //used to indicate if the component shall be updated every tick only called once!
        return false;
    }

    @Override
    public void update() {
        //do stuff on update (only called if canUpdate returns true)
    }

    @Override
    public void load(NBTTagCompound nbt) {
        //example if you want different implementation do so!
        if (node != null)
            node.load(nbt.getCompoundTag("node"));
    }

    @Override
    public void save(NBTTagCompound nbt) {
        //example if you want different implementation do so!
        if (node != null) {
            NBTTagCompound compound = new NBTTagCompound();
            node.save(compound);
            nbt.setCompoundTag("node", compound);
        }

    }
    /**
     * ********************************************Custom methods***************************
     */
    //This is a example method that can be called from lua. The lua name and java name don't need
    //to be the same
    @LuaCallback("test")
    public Object[] testName(Context computer, Arguments arguments) {
        return new Object[]{"Hello", " Lua"};
    }
}
