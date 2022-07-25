package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DriverItem
import li.cil.oc.common.item.Delegator
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item // Rarity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.UseAction
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToScala._

trait Delegate {
  def parent: Delegator

  def unlocalizedName: String = getClass.getSimpleName.toLowerCase

  protected def tooltipName = Option(unlocalizedName)

  protected def tooltipData = Seq.empty[Any]

  var showInItemList = true

  val itemId: Int = parent.add(this)

  def maxStackSize = 64

  def createItemStack(amount: Int = 1) = {
    val stack = new ItemStack(parent, amount)
    stack.setDamageValue(itemId)
    stack
  }

  // ----------------------------------------------------------------------- //

  def doesSneakBypassUse(world: IBlockReader, pos: BlockPos, player: PlayerEntity) = false

  def onItemUseFirst(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): ActionResultType = ActionResultType.PASS

  @Deprecated
  def onItemUse(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = false

  def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = new ActionResult(ActionResultType.PASS, stack)

  def getUseAnimation(stack: ItemStack): UseAction = UseAction.NONE

  def getMaxItemUseDuration(stack: ItemStack) = 0

  def finishUsingItem(stack: ItemStack, world: World, player: LivingEntity): ItemStack = stack

  def onPlayerStoppedUsing(stack: ItemStack, player: LivingEntity, duration: Int) {}

  def update(stack: ItemStack, world: World, player: Entity, slot: Int, selected: Boolean) {}

  // ----------------------------------------------------------------------- //

  def rarity(stack: ItemStack): item.Rarity = Rarity.byTier(tierFromDriver(stack))

  protected def tierFromDriver(stack: ItemStack): Int =
    api.Driver.driverFor(stack) match {
      case driver: DriverItem => driver.tier(stack)
      case _ => 0
    }

  def color(stack: ItemStack, pass: Int) = 0xFFFFFF

  @Deprecated
  def getCraftingRemainingItem(): Item = null

  @Deprecated
  def hasCraftingRemainingItem(): Boolean = false

  def displayName(stack: ItemStack): Option[String] = None

  @OnlyIn(Dist.CLIENT)
  def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    if (tooltipName.isDefined) {
      for (curr <- Tooltip.get(tooltipName.get, tooltipData: _*)) {
        tooltip.add(new StringTextComponent(curr))
      }
      tooltipExtended(stack, tooltip)
    }
    tooltipCosts(stack, tooltip)
  }

  // For stuff that goes to the normal 'extended' tooltip, before the costs.
  protected def tooltipExtended(stack: ItemStack, tooltip: java.util.List[ITextComponent]) {}

  protected def tooltipCosts(stack: ItemStack, tooltip: java.util.List[ITextComponent]) {
    if (stack.hasTag && stack.getTag.contains(Settings.namespace + "data")) {
      val data = stack.getTag.getCompound(Settings.namespace + "data")
      if (data.contains("node") && data.getCompound("node").contains("address")) {
        tooltip.add(new StringTextComponent("§8" + data.getCompound("node").getString("address").substring(0, 13) + "...§7"))
      }
    }
  }

  def showDurabilityBar(stack: ItemStack) = false

  def durability(stack: ItemStack) = 0.0
}
