package li.cil.oc.api;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;

import java.lang.reflect.Constructor;

/**
 * Provides convenience methods for interacting with component networks.
 */
public final class NetworkAPI {
    private static Constructor<?> networkConstructor;

    static {
        try {
            Class<?> networkClass = Class.forName("li.cil.oc.server.computer.Network");
            networkConstructor = networkClass.getConstructor(INetworkNode.class);
        } catch (Exception ignored) {
        }
    }

    private NetworkAPI() {
    }

    /**
     * Tries to add a tile entity network node at the specified coordinates to adjacent networks.
     *
     * @param world the world the tile entity lives in.
     * @param x     the X coordinate of the tile entity.
     * @param y     the Y coordinate of the tile entity.
     * @param z     the Z coordinate of the tile entity.
     */
    public static void joinOrCreateNetwork(IBlockAccess world, int x, int y, int z) {
        INetworkNode node = getNetworkNode(world, x, y, z);
        if (node == null) {
            return;
        }
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            INetworkNode neighborNode = getNetworkNode(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ);
            if (neighborNode != null && neighborNode.getNetwork() != null) {
                neighborNode.getNetwork().connect(neighborNode, node);
            }
        }
        if (node.getNetwork() == null) {
            try {
                networkConstructor.newInstance(node);
            } catch (Exception ignored) {
            }
        }
    }

    private static INetworkNode getNetworkNode(IBlockAccess world, int x, int y, int z) {
        Block block = Block.blocksList[world.getBlockId(x, y, z)];
        if (block == null || !block.hasTileEntity(world.getBlockMetadata(x, y, z))) {
            return null;
        }
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (tileEntity == null || !(tileEntity instanceof INetworkNode)) {
            return null;
        }
        return (INetworkNode) tileEntity;
    }
}
