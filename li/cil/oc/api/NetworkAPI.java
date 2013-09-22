package li.cil.oc.api;

import java.lang.reflect.Constructor;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;

/**
 * Provides convenience methods for interacting with component networks.
 */
public class NetworkAPI {
  /** The internally used implementation for networks. */
  private static Constructor<?> networkConstructor;
  static {
    try {
      Class<?> networkClass = Class
          .forName("li.cil.oc.server.computer.Network");
      networkConstructor = networkClass.getConstructor(INetworkNode.class);
    } catch (Exception e) {
    }
  }

  /** No need to create instances of this. */
  private NetworkAPI() {
  }

  public static void joinOrCreateNetwork(IBlockAccess world, int x, int y,
      int z, INetworkNode node) {
    for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
      Block block = Block.blocksList[world.getBlockId(x, y, z)];
      if (block != null && block.hasTileEntity(world.getBlockMetadata(x, y, z))) {
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (tileEntity instanceof INetworkNode) {
          INetworkNode other = (INetworkNode) tileEntity;
          if (other.getNetwork() != null) {
            other.getNetwork().connect(node, other);
          }
        }
      }
    }
    if (node.getNetwork() == null) {
      INetwork network;
      try {
        networkConstructor.newInstance(node);
      } catch (Exception e) {
      }
    }
  }
}
