package forestry.api.circuits;

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import forestry.api.core.INBTTagable;

public interface ICircuitBoard extends INBTTagable {

	int getPrimaryColor();

	int getSecondaryColor();

	void addTooltip(List<String> list);

	void onInsertion(TileEntity tile);

	void onLoad(TileEntity tile);

	void onRemoval(TileEntity tile);

	void onTick(TileEntity tile);
	
	ICircuit[] getCircuits();

}
