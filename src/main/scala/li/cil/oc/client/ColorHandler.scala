package li.cil.oc.client

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.internal.Colored
import li.cil.oc.common.block
import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import li.cil.oc.util.ItemUtils
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.color.IBlockColor
import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess

object ColorHandler {
  def init(): Unit = {
    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Cable => block.colorMultiplierOverride.getOrElse(0xFFFFFFFF)
      case _ => 0xFFFFFFFF
    },
      api.Items.get(Constants.BlockName.Cable).block())

    register((state, world, pos, tintIndex) => world.getTileEntity(pos) match {
      case colored: Colored => colored.getColor
      case _ => state.getBlock match {
        case block: block.Case => Color.rgbValues(Color.byTier(block.tier))
        case _ => 0xFFFFFFFF
      }
    },
      api.Items.get(Constants.BlockName.CaseTier1).block(),
      api.Items.get(Constants.BlockName.CaseTier2).block(),
      api.Items.get(Constants.BlockName.CaseTier3).block(),
      api.Items.get(Constants.BlockName.CaseCreative).block())

    register((state, world, pos, tintIndex) => Color.rgbValues(Color.byOreName(Color.dyes(state.getBlock.getMetaFromState(state) max 0 min Color.dyes.length))),
      api.Items.get(Constants.BlockName.ChameliumBlock).block())

    register((state, world, pos, tintIndex) => tintIndex,
      api.Items.get(Constants.BlockName.Print).block())

    register((state, world, pos, tintIndex) => state.getBlock match {
      case block: block.Screen => Color.rgbValues(Color.byTier(block.tier))
      case _ => 0xFFFFFFFF
    },
      api.Items.get(Constants.BlockName.ScreenTier1).block(),
      api.Items.get(Constants.BlockName.ScreenTier2).block(),
      api.Items.get(Constants.BlockName.ScreenTier3).block())

    register((stack, tintIndex) => if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else tintIndex,
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.Cable).block()))

    register((stack, tintIndex) => Color.rgbValues(Color.byTier(ItemUtils.caseTier(stack))),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.CaseTier1).block()),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.CaseTier2).block()),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.CaseTier3).block()),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.CaseCreative).block()))

    register((stack, tintIndex) => Color.rgbValues(EnumDyeColor.byDyeDamage(stack.getItemDamage)),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.ChameliumBlock).block()))

    register((stack, tintIndex) => tintIndex,
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.ScreenTier1).block()),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.ScreenTier2).block()),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.ScreenTier3).block()),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.Print).block()),
      Item.getItemFromBlock(api.Items.get(Constants.BlockName.Robot).block()))
  }

  def register(handler: (IBlockState, IBlockAccess, BlockPos, Int) => Int, blocks: Block*): Unit = {
    Minecraft.getMinecraft.getBlockColors.registerBlockColorHandler(new IBlockColor {
      override def colorMultiplier(state: IBlockState, world: IBlockAccess, pos: BlockPos, tintIndex: Int): Int = handler(state, world, pos, tintIndex)
    }, blocks: _*)
  }

  def register(handler: (ItemStack, Int) => Int, items: Item*): Unit = {
    Minecraft.getMinecraft.getItemColors.registerItemColorHandler(new IItemColor {
      override def getColorFromItemstack(stack: ItemStack, tintIndex: Int): Int = handler(stack, tintIndex)
    }, items: _*)
  }
}
