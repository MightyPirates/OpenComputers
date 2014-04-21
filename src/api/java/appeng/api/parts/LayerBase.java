package appeng.api.parts;

import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * All Layers must extends this, this get part implementation is provided to interface with the parts, however a real
 * implementation will be used at runtime.
 */
public class LayerBase extends TileEntity // implements IPartHost
{

	/**
	 * Grants access for the layer to the parts of the host.
	 * 
	 * This Method looks silly, thats because its not used at runtime, a real implementation will be used instead.
	 * 
	 * @param side
	 * @return the part for the requested side.
	 */
	public IPart getPart(ForgeDirection side)
	{
		return null; // place holder.
	}

	/**
	 * called when the parts change in the container, YOU MUST CALL super.PartChanged();
	 */
	public void partChanged()
	{
	}

	/**
	 * @return a mutable list of flags you can adjust to track state.
	 */
	public Set<LayerFlags> getLayerFlags()
	{
		return null; // place holder.
	}

	@Override
	public void markDirty()
	{
		// something!
	}
}
