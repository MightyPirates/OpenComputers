/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.blueprints;

import buildcraft.api.core.BuildCraftAPI;
import java.util.ArrayList;
import java.util.LinkedList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 * This class allow to specify specific behavior for blocks stored in blueprints:
 *
 * - what items needs to be used to create that block - how the block has to be built on the world - how to rotate the block - what extra data to store / load
 * in the blueprint
 *
 * Default implementations of this can be seen in the package buildcraft.api.bptblocks. The class BptBlockUtils provide some additional utilities.
 *
 * Blueprints perform "id translation" in case the block ids between a blueprint and the world installation are different. In order to translate block ids,
 * blocks needs to be uniquely identified. By default, this identification is done by:
 *
 * - the block simple class name - the tile simple class name (if any) - the block name
 *
 * In certain circumstances, the above is not enough (e.g. if several blocks share the same class and the same name, with no tile). In this case, additional
 * data may be provided by children of this class:
 *
 * - mod name - custom signature
 *
 * At blueprint load time, BuildCraft will check that each block id of the blueprint corresponds to the block id in the installation. If not, it will perform a
 * search through the block list, and upon matching signature, it will translate all blocks ids of the blueprint to the installation ones. If no such block id
 * is found, BuildCraft will assume that the block is not installed and will not load the blueprint.
 */

@Deprecated
public class BptBlock {

	public final int blockId;

	public BptBlock(int blockId) {
		this.blockId = blockId;

		//BlueprintManager.blockBptProps[blockId] = this;
	}

	/**
	 * Returns the requirements needed to build this block. When the requirements are met, they will be removed all at once from the builder, before calling
	 * buildBlock.
	 */
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		if (slot.block != null) {
			if (slot.storedRequirements.size() != 0) {
				requirements.addAll(slot.storedRequirements);
			} else {
			//	requirements.add(new ItemStack(slot.blockId, 1, slot.meta));
			}
		}
	}

	/**
	 * This is called each time an item matches a reqquirement, that is: (req id == stack id) for damageable items (req id == stack id && req dmg == stack dmg)
	 * for other items by default, it will increase damage of damageable items by the amount of damage of the requirement, and remove the intended amount of non
	 * damageable item.
	 *
	 * Client may override this behavior for default items. Note that this subprogram may be called twice with the same parameters, once with a copy of
	 * requirements and stack to check if the entire requirements can be fullfilled, and once with the real inventory. Implementer is responsible for updating
	 * req (with the remaining requirements if any) and stack (after usage)
	 *
	 * returns: what was used (similer to req, but created from stack, so that any NBT based differences are drawn from the correct source)
	 */
	public ItemStack useItem(BptSlotInfo slot, IBptContext context, ItemStack req, ItemStack stack) {
		ItemStack result = stack.copy();
		if (stack.isItemStackDamageable()) {
			if (req.getItemDamage() + stack.getItemDamage() <= stack.getMaxDamage()) {
				stack.setItemDamage(req.getItemDamage() + stack.getItemDamage());
				result.setItemDamage(req.getItemDamage());
				req.stackSize = 0;
			}

			if (stack.getItemDamage() >= stack.getMaxDamage()) {
				stack.stackSize = 0;
			}
		} else {
			if (stack.stackSize >= req.stackSize) {
				result.stackSize = req.stackSize;
				stack.stackSize -= req.stackSize;
				req.stackSize = 0;
			} else {
				req.stackSize -= stack.stackSize;
				stack.stackSize = 0;
			}
		}

		if (stack.stackSize == 0 && stack.getItem().getContainerItem() != null) {
			Item container = stack.getItem().getContainerItem();

			//stack.itemID = container.itemID;
			stack.stackSize = 1;
			stack.setItemDamage(0);
		}
		return result;
	}

	/**
	 * Return true if the block on the world correspond to the block stored in the blueprint at the location given by the slot. By default, this subprogram is
	 * permissive and doesn't take into account metadata.
	 *
	 * Added metadata sensitivity //Krapht
	 */
	public boolean isValid(BptSlotInfo slot, IBptContext context) {
		//return slot.blockId == context.world().getBlockId(slot.x, slot.y, slot.z) && slot.meta == context.world().getBlockMetadata(slot.x, slot.y, slot.z);
		return false;
	}

	/**
	 * Perform a 90 degree rotation to the slot.
	 */
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {

	}

	/**
	 * Places the block in the world, at the location specified in the slot.
	 */
	public void buildBlock(BptSlotInfo slot, IBptContext context) {
		// Meta needs to be specified twice, depending on the block behavior
		context.world().setBlock(slot.x, slot.y, slot.z, slot.block, slot.meta, 0);
		//context.world().setBlockMetadataWithNotify(slot.x, slot.y, slot.z, slot.meta,3);

		if (slot.block instanceof BlockContainer) {
			TileEntity tile = context.world().getTileEntity(slot.x, slot.y, slot.z);

			//slot.cpt.setInteger("x", slot.x);
			//slot.cpt.setInteger("y", slot.y);
			//slot.cpt.setInteger("z", slot.z);

			//if (tile != null) {
			//	tile.readFromNBT(slot.cpt);
			//}
		}
	}

	/**
	 * Return true if the block should not be placed to the world. Requirements will not be asked on such a block, and building will not be called.
	 */
	public boolean ignoreBuilding(BptSlotInfo slot) {
		return false;
	}

	/**
	 * Initializes a slot from the blueprint according to an objet placed on {x, y, z} on the world. This typically means adding entries in slot.cpt. Note that
	 * "id" and "meta" will be set automatically, corresponding to the block id and meta.
	 *
	 * By default, if the block is a BlockContainer, tile information will be to save / load the block.
	 */
	public void initializeFromWorld(BptSlotInfo slot, IBptContext context, int x, int y, int z) {
		/*if (Block.blocksList[slot.blockId] instanceof BlockContainer) {
			TileEntity tile = context.world().getTileEntity(x, y, z);

			if (tile != null) {
				tile.writeToNBT(slot.cpt);
			}
		}

		if (Block.blocksList[slot.blockId] != null) {
			ArrayList<ItemStack> req = Block.blocksList[slot.blockId].getBlockDropped(context.world(), x, y, z, context.world().getBlockMetadata(x, y, z), 0);

			if (req != null) {
				slot.storedRequirements.addAll(req);
			}
		}*/
	}

	/**
	 * Called on a block when the blueprint has finished to place all the blocks. This may be useful to adjust variable depending on surrounding blocks that may
	 * not be there already at initial building.
	 */
	public void postProcessing(BptSlotInfo slot, IBptContext context) {

	}

	/**
	 * By default, block class name, block tile name and block name are used to define block signature. Overriding this subprogram may allow to replace some of
	 * these with stars, specify the mod that this block kind is coming from or add custom data to the signature.
	 */
	public BlockSignature getSignature(Block block) {
		BlockSignature sig = new BlockSignature();

		/*if (block.blockID > BuildCraftAPI.LAST_ORIGINAL_BLOCK) {
			sig.blockClassName = block.getClass().getSimpleName();

			if (block instanceof BlockContainer) {
				// TODO: Try to see if we can get a world instance to call with instead of null
				TileEntity tile = ((BlockContainer) block).createNewTileEntity(null);

				if (tile != null) {
					sig.tileClassName = tile.getClass().getSimpleName();
				}
			}
		}*/

		sig.blockName = block.getUnlocalizedName();
		sig.replaceNullWithStar();

		return sig;
	}

	/**
	 * By default, block name, block and tile classes, mod name and custom signature are matched to verify if a blueprint block corresponds to the installation
	 * block - except for the default blocks which don't check for classes. For any value, * means match with anything. For compatibilty and evolution reasons,
	 * mods may want to write a different policy, allowing to migrate one format to the other.
	 */
	public boolean match(Block block, BlockSignature sig) {
		if (block == null)
			return false;

		BlockSignature inst = BlueprintManager.getBlockSignature(block);

		return starMatch(sig.blockName, inst.blockName) && starMatch(sig.blockClassName, inst.blockClassName)
				&& starMatch(sig.tileClassName, inst.tileClassName) && starMatch(sig.customField, inst.customField) && starMatch(sig.mod, inst.mod);
	}

	private boolean starMatch(String s1, String s2) {
		return s1.equals("*") || s2.equals("*") || s1.equals(s2);
	}
}
