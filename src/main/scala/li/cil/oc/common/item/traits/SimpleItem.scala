package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.Settings
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUseContext
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IWorldReader
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.WrapAsScala._

trait SimpleItem extends Item {
  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  @Deprecated
  private var unlocalizedName = super.getDescriptionId()

  @Deprecated
  private[oc] def setUnlocalizedName(name: String): Unit = unlocalizedName = name

  override def getDescriptionId = unlocalizedName

  override def doesSneakBypassUse(stack: ItemStack, world: IWorldReader, pos: BlockPos, player: PlayerEntity): Boolean = {
    world.getBlockEntity(pos) match {
      case drive: tileentity.DiskDrive => true
      case _ => super.doesSneakBypassUse(stack, world, pos, player)
    }
  }

  @Deprecated
  override def onItemUseFirst(stack: ItemStack, ctx: ItemUseContext): ActionResultType = {
    val pos = ctx.getClickedPos
    val hitPos = ctx.getClickLocation
    onItemUseFirst(ctx.getPlayer, ctx.getPlayer.level, pos, ctx.getClickedFace,
      (hitPos.x - pos.getX).toFloat, (hitPos.y - pos.getY).toFloat, (hitPos.z - pos.getZ).toFloat, ctx.getHand)
  }

  @Deprecated
  def onItemUseFirst(player: PlayerEntity, world: World, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: Hand) = ActionResultType.PASS

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase)) {
      tooltip.add(new StringTextComponent(curr))
    }

    if (stack.hasTag && stack.getTag.contains(Settings.namespace + "data")) {
      val data = stack.getTag.getCompound(Settings.namespace + "data")
      if (data.contains("node") && data.getCompound("node").contains("address")) {
        tooltip.add(new StringTextComponent("§8" + data.getCompound("node").getString("address").substring(0, 13) + "...§7"))
      }
    }
  }
}
