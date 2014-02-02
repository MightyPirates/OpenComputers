package universalelectricity.api.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.world.World;
import universalelectricity.api.CompatibilityModule;
import universalelectricity.api.UniversalElectricity;
import universalelectricity.api.energy.UnitDisplay;
import universalelectricity.api.energy.UnitDisplay.Unit;

/** Extend from this class if your item requires electricity or to be charged. Optionally, you can
 * implement IItemElectric instead.
 * 
 * @author Calclavia */
public abstract class ItemElectric extends Item implements IEnergyItem, IVoltageItem
{
    private static final String ENERGY_NBT = "electricity";

    public ItemElectric(int id)
    {
        super(id);
        setMaxStackSize(1);
        setMaxDamage(100);
        setNoRepair();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
        String color = "";
        long joules = getEnergy(itemStack);

        if (joules <= getEnergyCapacity(itemStack) / 3)
        {
            color = "\u00a74";
        }
        else if (joules > getEnergyCapacity(itemStack) * 2 / 3)
        {
            color = "\u00a72";
        }
        else
        {
            color = "\u00a76";
        }

        list.add(color + UnitDisplay.getDisplayShort(joules, Unit.JOULES) + "/" + UnitDisplay.getDisplayShort(getEnergyCapacity(itemStack), Unit.JOULES));
    }

    /** Makes sure the item is uncharged when it is crafted and not charged. Change this if you do
     * not want this to happen! */
    @Override
    public void onCreated(ItemStack itemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        setEnergy(itemStack, 0);
    }

    @Override
    public long recharge(ItemStack itemStack, long energy, boolean doReceive)
    {
        long energyReceived = Math.min(getEnergyCapacity(itemStack) - getEnergy(itemStack), Math.min(getTransferRate(itemStack), energy));

        if (doReceive)
        {
            setEnergy(itemStack, getEnergy(itemStack) + energyReceived);
        }

        return energyReceived;
    }

    public long getTransferRate(ItemStack itemStack)
    {
        return getEnergyCapacity(itemStack) / 100;
    }

    @Override
    public long discharge(ItemStack itemStack, long energy, boolean doTransfer)
    {
        long energyExtracted = Math.min(getEnergy(itemStack), Math.min(getTransferRate(itemStack), energy));

        if (doTransfer)
        {
            setEnergy(itemStack, getEnergy(itemStack) - energyExtracted);
        }

        return energyExtracted;
    }

    @Override
    public long getVoltage(ItemStack itemStack)
    {
        return UniversalElectricity.DEFAULT_VOLTAGE;
    }

    @Override
    public void setEnergy(ItemStack itemStack, long joules)
    {
        if (itemStack.getTagCompound() == null)
        {
            itemStack.setTagCompound(new NBTTagCompound());
        }

        long electricityStored = Math.max(Math.min(joules, getEnergyCapacity(itemStack)), 0);
        itemStack.getTagCompound().setLong(ENERGY_NBT, electricityStored);
        itemStack.setItemDamage((int) (100 - ((double) electricityStored / (double) getEnergyCapacity(itemStack)) * 100));
    }

    public long getTransfer(ItemStack itemStack)
    {
        return getEnergyCapacity(itemStack) - getEnergy(itemStack);
    }

    @Override
    public long getEnergy(ItemStack itemStack)
    {
        if (itemStack.getTagCompound() == null)
        {
            itemStack.setTagCompound(new NBTTagCompound());
        }

        long energyStored = 0;

        if (itemStack.getTagCompound().hasKey(ENERGY_NBT))
        {
            // Backwards compatibility
            NBTBase obj = itemStack.getTagCompound().getTag(ENERGY_NBT);

            if (obj instanceof NBTTagFloat)
            {
                energyStored = (long) ((NBTTagFloat) obj).data;
            }
            else if (obj instanceof NBTTagLong)
            {
                energyStored = (long) ((NBTTagLong) obj).data;
            }
        }

        itemStack.setItemDamage((int) (100 - ((double) energyStored / (double) getEnergyCapacity(itemStack)) * 100));
        return energyStored;
    }

    @Override
    public void getSubItems(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        par3List.add(CompatibilityModule.getItemWithCharge(new ItemStack(this), 0));
        par3List.add(CompatibilityModule.getItemWithCharge(new ItemStack(this), getEnergyCapacity(new ItemStack(this))));
    }
}
