package li.cil.oc.integration.bloodmagic;

import WayofTime.alchemicalWizardry.api.items.interfaces.IBloodOrb;
import WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class ConverterBloodOrb implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item instanceof IBloodOrb && stack.stackTagCompound != null) {
                final IBloodOrb bloodOrb = (IBloodOrb) item;
                final String ownerName = stack.stackTagCompound.getString("ownerName");
                final int maxOrbTier = SoulNetworkHandler.getCurrentMaxOrb(ownerName);
                output.put("ownerName", ownerName);
                output.put("networkOrbTier", maxOrbTier);
                output.put("networkEssence", SoulNetworkHandler.getCurrentEssence(ownerName));
                output.put("maxNetworkEssence", SoulNetworkHandler.getMaximumForOrbTier(maxOrbTier));
                output.put("maxEssence", bloodOrb.getMaxEssence());
                output.put("orbTier", bloodOrb.getOrbLevel());
            }
        }
    }
}
