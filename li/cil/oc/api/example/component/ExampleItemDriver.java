package li.cil.oc.api.example.component;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.Item;
import li.cil.oc.api.driver.Slot;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;


public class ExampleItemDriver implements Item {


    @Override
    public boolean worksWith(ItemStack item) {
        return item.getItem().equals(ExampleItem.thisItem);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, TileEntity container) {
        //return the environment for the item
        return new ExampleItemEnvironment();
    }

    @Override
    public Slot slot(ItemStack stack) {
        return Slot.Upgrade;//This is the part where you define in which slot the item fits.
    }

    /**
     * This is a example implementation of dataTag. If you prefere a different way to retrieve the compund
     * feel free to use it
     * @param stack the item to get the child tag from.
     * @return
     */
    @Override
    public NBTTagCompound dataTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound("tag"));
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey("yournamespace" + "data")) {
            nbt.setCompoundTag("yournamespace" + "data", new NBTTagCompound());
        }
        return nbt.getCompoundTag("yournamespace" + "data");
    }

    //This is supposed to be your proxy where you init your stuff
    public class Proxy{


        public void  init(FMLInitializationEvent e) {
            //in the init methode add this to register the driver
            Driver.add(new ExampleItemDriver());
        }
    }
}
