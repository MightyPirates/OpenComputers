package li.cil.oc.integration.mystcraft;

import com.google.common.hash.Hashing;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;

public class ConverterLinkbook implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            if ("item.myst.linkbook".equals(stack.getUnlocalizedName()) && stack.hasTagCompound()) {
                final NBTTagCompound tag = stack.getTagCompound();
                if (tag.hasKey("Dimension"))
                    output.put("dimensionId", tag.getInteger("Dimension"));
                if (tag.hasKey("DisplayName"))
                    output.put("dimensionName", tag.getString("DisplayName"));

                if (tag.hasKey("SpawnX") && tag.hasKey("SpawnY") && tag.hasKey("SpawnZ")) {
                    output.put("spawnId", Hashing.murmur3_32().newHasher().
                            putInt(tag.getInteger("SpawnX")).
                            putInt(tag.getInteger("SpawnY")).
                            putInt(tag.getInteger("SpawnZ")).
                            hash().asInt());
                }
            }
        }
    }
}
