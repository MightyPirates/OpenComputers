package mods.railcraft.api.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.world.World;

public interface INetworkedObject
{

    public World getWorld();

    public void writePacketData(DataOutputStream data) throws IOException;

    public void readPacketData(DataInputStream data) throws IOException;
}
