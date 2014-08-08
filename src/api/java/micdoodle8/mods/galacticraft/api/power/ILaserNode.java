package micdoodle8.mods.galacticraft.api.power;

import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import net.minecraft.tileentity.TileEntity;

public interface ILaserNode extends IEnergyHandlerGC
{
	public Vector3 getInputPoint();

	public Vector3 getOutputPoint(boolean offset);

	public ILaserNode getTarget();

	public TileEntity getTile();

	public boolean canConnectTo(ILaserNode node);

	public Vector3 getColor();

	public void addNode(ILaserNode node);

	public void removeNode(ILaserNode node);

	public int compareTo(ILaserNode otherNode, BlockVec3 origin);
}
