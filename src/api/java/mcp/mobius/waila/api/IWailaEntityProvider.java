package mcp.mobius.waila.api;

import java.util.List;

import net.minecraft.entity.Entity;

public interface IWailaEntityProvider {
	
	/* A way to get an override on the entity returned by the raytracing */
	Entity getWailaOverride(IWailaEntityAccessor accessor, IWailaConfigHandler config);
	
	/* The classical HEAD/BODY/TAIL text getters */
	List<String> getWailaHead(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config);
	List<String> getWailaBody(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config);
	List<String> getWailaTail(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config);
}
