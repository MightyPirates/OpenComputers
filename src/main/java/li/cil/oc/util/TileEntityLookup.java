package li.cil.oc.util;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class TileEntityLookup {
    private static Map<BlockData, Class<? extends TileEntity>> cache = new HashMap<BlockData, Class<? extends TileEntity>>();

    private TileEntityLookup() {
    }

    public static Class<? extends TileEntity> get(World world, int blockId, int metadata) {
        BlockData data = new BlockData(blockId, metadata);
        if (!cache.containsKey(data)) {
            Class<? extends TileEntity> clazz = null;
            try {
                boolean isValidBlock = blockId >= 0 && blockId < Block.blocksList.length && Block.blocksList[blockId] != null;
                if (isValidBlock) {
                    Block block = Block.blocksList[blockId];
                    if (block.hasTileEntity(metadata)) {
                        TileEntity tileEntity = block.createTileEntity(world, metadata);
                        if (tileEntity != null) {
                            clazz = tileEntity.getClass();
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            cache.put(data, clazz);
        }
        return cache.get(data);
    }

    public static Class<? extends TileEntity> get(World world, ItemStack stack) {
        if (stack != null) {
            if (stack.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock) stack.getItem();
                int blockId = itemBlock.getBlockID();
                int metadata = itemBlock.getMetadata(stack.getItemDamage());
                return get(world, blockId, metadata);
            }
        }
        return null;
    }

    private static class BlockData {
        public final int id;
        public final int metadata;

        public BlockData(int id, int metadata) {
            this.id = id;
            this.metadata = metadata;
        }

        @Override
        public int hashCode() {
            return (23 * 31 + id) * 31 + metadata;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof BlockData) {
                BlockData other = (BlockData) obj;
                return other.id == id && other.metadata == metadata;
            }
            return false;
        }

        @Override
        public String toString() {
            return "{" + id + ":" + metadata + "}";
        }
    }
}
