package li.cil.oc.integration.rotarycraft;


import cpw.mods.fml.common.registry.GameRegistry;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;

public class ConverterJetpackItem implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item.equals(GameRegistry.findItem("RotaryCraft", "rotarycraft_item_bedpack")) ||
                    item.equals(GameRegistry.findItem("RotaryCraft", "rotarycraft_item_steelpack")) ||
                    item.equals(GameRegistry.findItem("RotaryCraft", "rotarycraft_item_jetpack"))) {
                final NBTTagCompound tag = stack.getTagCompound();

                if (tag != null && tag.hasKey("fuel"))
                    output.put("fuel", stack.stackTagCompound.getInteger("fuel"));
                else
                    output.put("fuel", 0);

                if (tag != null && tag.hasKey("liquid")) {
                    output.put("fuelType", tag.getString("liquid"));
                } else {
                    output.put("fuelType", "empty");
                }

                if (item.equals(GameRegistry.findItem("RotaryCraft", "rotarycraft_item_bedpack")))
                    output.put("chestplateMaterial", "bedrock");
                else if (item.equals(GameRegistry.findItem("RotaryCraft", "rotarycraft_item_steelpack")))
                    output.put("chestplateMaterial", "steel");
                else
                    output.put("chestplateMaterial", "none");

                final HashMap<String, Boolean> upgrades = new HashMap<String, Boolean>();
                upgrades.put("cooling", tag != null ? tag.getBoolean("cooling") : false);
                upgrades.put("thrustBoost", tag != null ? tag.getBoolean("jet") : false);
                upgrades.put("winged", tag != null ? tag.getBoolean("wing") : false);
                output.put("upgrades", upgrades);
            }
        }
    }
}
