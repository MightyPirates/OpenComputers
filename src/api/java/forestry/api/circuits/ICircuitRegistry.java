package forestry.api.circuits;

import java.util.HashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface ICircuitRegistry {

	/* CIRCUITS */
	HashMap<String, ICircuit> getRegisteredCircuits();

	void registerCircuit(ICircuit circuit);

	ICircuit getCircuit(String uid);

	ICircuitLibrary getCircuitLibrary(World world, String playername);

	void registerLegacyMapping(int id, String uid);

	ICircuit getFromLegacyMap(int id);

	/* LAYOUTS */
	HashMap<String, ICircuitLayout> getRegisteredLayouts();

	void registerLayout(ICircuitLayout layout);

	ICircuitLayout getLayout(String uid);

	ICircuitLayout getDefaultLayout();
	
	
	ICircuitBoard getCircuitboard(ItemStack itemstack);
	
	boolean isChipset(ItemStack itemstack);

}
