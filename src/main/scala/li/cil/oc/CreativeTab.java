package li.cil.oc;

import li.cil.oc.api.Items;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class CreativeTab extends CreativeTabs {
    public static final CreativeTab INSTANCE = new CreativeTab();

    private CreativeTab() {
        super(OpenComputers.ID());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ItemStack getTabIconItem() {
        return Items.get(Constants.BlockName.CaseTier1()).createItemStack(1);
    }
}
