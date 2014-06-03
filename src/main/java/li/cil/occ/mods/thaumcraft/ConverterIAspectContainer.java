package li.cil.occ.mods.thaumcraft;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.driver.Converter;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import com.google.common.collect.Maps;

public class ConverterIAspectContainer implements Converter {

	@Override
	public void convert(Object value, Map<Object, Object> output) {
		if(value instanceof IAspectContainer){
			IAspectContainer container = (IAspectContainer)value;
			output.put("aspectList", container.getAspects());
		}	
		
		if(value instanceof AspectList){
			AspectList aspectList = (AspectList)value;
			int i=0;
			for(Aspect aspect : aspectList.getAspects()){
				if (aspect == null) continue;
				HashMap<Object, Object> aspectMap = Maps.newHashMap();
				aspectMap.put("name", aspect.getName());
				aspectMap.put("quantity", aspectList.getAmount(aspect));
				output.put(++i, aspectMap);
			}
		}
	}
}
