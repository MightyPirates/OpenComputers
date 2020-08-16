package li.cil.oc.integration.appeng;

import appeng.helpers.PatternHelper;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class ConverterPattern  implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            ItemStack is = (ItemStack)value;
            try
            {
                PatternHelper p = new PatternHelper( is, null );
                Map[] inputs = new Map[p.getInputs().length];
                for (int i = 0; i < p.getInputs().length; ++i)
                {
                    inputs[i] = new HashMap<Object, Object>();
                    if (p.getInputs()[i] == null)
                        continue;
                    ItemStack input = p.getInputs()[i].getItemStack();
                    inputs[i].put("name", input.getItem().getItemStackDisplayName(input));
                    inputs[i].put("count", input.stackSize);
                }
                output.put("inputs", inputs);
                Map[] results = new Map[p.getInputs().length];
                for (int i = 0; i < p.getOutputs().length; ++i)
                {
                    results[i] = new HashMap<Object, Object>();
                    if (p.getOutputs()[i] == null)
                        continue;
                    ItemStack result = p.getOutputs()[i].getItemStack();
                    results[i].put("name", result.getItem().getItemStackDisplayName(result));
                    results[i].put("count", result.stackSize);
                }
                output.put("outputs", results);
                output.put("isCraftable", p.isCraftable());
            }
            catch( final Throwable ignored)
            {
            }
        }
    }
}
