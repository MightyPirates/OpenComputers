package li.cil.oc.integration.rotarycraft;

import cpw.mods.fml.common.registry.GameRegistry;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;

public class ConverterJetpackItem implements Converter {
    final Item BedrockJetPack = GameRegistry.findItem("RotaryCraft", "rotarycraft_item_bedpack");
    final Item SteelJetPack = GameRegistry.findItem("RotaryCraft", "rotarycraft_item_steelpack");
    final Item JetPack = GameRegistry.findItem("RotaryCraft", "rotarycraft_item_jetpack");

    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item != null && item == BedrockJetPack || item == SteelJetPack || item == JetPack) {
                final NBTTagCompound tag = stack.getTagCompound();

                if (tag != null && tag.hasKey("fuel")) {
                    output.put("fuel", stack.stackTagCompound.getInteger("fuel"));
                } else {
                    output.put("fuel", 0);
                }

                if (tag != null && tag.hasKey("liquid")) {
                    output.put("fuelType", tag.getString("liquid"));
                } else {
                    output.put("fuelType", "empty");
                }

                if (item == BedrockJetPack) {
                    output.put("chestplateMaterial", "bedrock");
                } else if (item == SteelJetPack) {
                    output.put("chestplateMaterial", "steel");
                } else {
                    output.put("chestplateMaterial", "none");
                }

                final HashMap<String, Boolean> upgrades = new HashMap<String, Boolean>();
                upgrades.put("cooling", tag != null && tag.getBoolean("cooling"));
                upgrades.put("thrustBoost", tag != null && tag.getBoolean("jet"));
                upgrades.put("winged", tag != null && tag.getBoolean("wing"));
                output.put("upgrades", upgrades);
            }
        }
    }
}
