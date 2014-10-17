package li.cil.oc.integration.thaumcraft;

import com.google.common.collect.Maps;
import li.cil.oc.api.driver.Converter;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

import java.util.HashMap;
import java.util.Map;

public class ConverterIAspectContainer implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof IAspectContainer) {
            final IAspectContainer container = (IAspectContainer) value;
            output.put("aspects", container.getAspects());
        }

        if (value instanceof AspectList) {
            final AspectList aspects = (AspectList) value;
            int i = 0;
            for (Aspect aspect : aspects.getAspects()) {
                if (aspect == null) continue;
                final HashMap<Object, Object> aspectMap = Maps.newHashMap();
                aspectMap.put("name", aspect.getName());
                aspectMap.put("amount", aspects.getAmount(aspect));
                output.put(++i, aspectMap);
            }
        }
    }
}
