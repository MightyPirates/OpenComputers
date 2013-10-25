package li.cil.oc.api.detail;

import net.minecraft.world.World;

public interface NetworkAPI {
    void joinOrCreateNetwork(World world, int x, int y, int z);
}