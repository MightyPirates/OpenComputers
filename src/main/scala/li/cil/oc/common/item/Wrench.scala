package li.cil.oc.common.item

import li.cil.oc.api
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

@Injectable.InterfaceList(Array(
  new Injectable.Interface(value = "appeng.api.implementations.items.IAEWrench", modid = Mods.IDs.AppliedEnergistics2),
  new Injectable.Interface(value = "buildcraft.api.tools.IToolWrench", modid = Mods.IDs.BuildCraftTools),
  new Injectable.Interface(value = "com.bluepowermod.api.misc.IScrewdriver", modid = Mods.IDs.BluePower),
  new Injectable.Interface(value = "cofh.api.item.IToolHammer", modid = Mods.IDs.CoFHItem),
  new Injectable.Interface(value = "crazypants.enderio.api.tool.ITool", modid = Mods.IDs.EnderIO),
  new Injectable.Interface(value = "mekanism.api.IMekWrench", modid = Mods.IDs.Mekanism),
  new Injectable.Interface(value = "powercrystals.minefactoryreloaded.api.IMFRHammer", modid = Mods.IDs.MineFactoryReloaded),
  new Injectable.Interface(value = "mrtjp.projectred.api.IScrewdriver", modid = Mods.IDs.ProjectRedCore),
  new Injectable.Interface(value = "mods.railcraft.api.core.items.IToolCrowbar", modid = Mods.IDs.Railcraft),
  new Injectable.Interface(value = "ic2.api.item.IBoxable", modid = Mods.IDs.IndustrialCraft2)
))
class Wrench extends traits.SimpleItem with api.internal.Wrench {
  setHarvestLevel("wrench", 1)
  setMaxStackSize(1)

  override def doesSneakBypassUse(stack: ItemStack, world: IBlockAccess, pos: BlockPos, player: EntityPlayer): Boolean = true

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, hand: EnumHand): EnumActionResult = {
    if (world.isBlockLoaded(pos) && world.isBlockModifiable(player, pos)) world.getBlockState(pos).getBlock match {
      case block: Block if block.rotateBlock(world, pos, side) =>
        block.neighborChanged(world.getBlockState(pos), world, pos, Blocks.AIR)
        player.swingArm(hand)
        if (!world.isRemote) EnumActionResult.SUCCESS else EnumActionResult.PASS
      case _ =>
        super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand)
    }
    else super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  def useWrenchOnBlock(player: EntityPlayer, world: World, pos: BlockPos, simulate: Boolean): Boolean = {
    if (!simulate) player.swingArm(EnumHand.MAIN_HAND)
    true
  }

  // Applied Energistics 2

  def canWrench(stack: ItemStack, player: EntityPlayer, pos: BlockPos): Boolean = true

  // BluePower

  def damage(stack: ItemStack, damage: Int, player: EntityPlayer, simulated: Boolean): Boolean = damage == 0

  // BuildCraft

  def canWrench(player: EntityPlayer, pos: BlockPos): Boolean = true

  def wrenchUsed(player: EntityPlayer, pos: BlockPos): Unit = player.swingArm(EnumHand.MAIN_HAND)

  def canWrench(player: EntityPlayer, entity: Entity): Boolean = true

  def wrenchUsed(player: EntityPlayer, entity: Entity): Unit = player.swingArm(EnumHand.MAIN_HAND)

  // CoFH

  def isUsable(stack: ItemStack, player: EntityLivingBase, pos: BlockPos): Boolean = true

  def isUsable(stack: ItemStack, player: EntityLivingBase, entity: Entity): Boolean = true

  def toolUsed(stack: ItemStack, player: EntityLivingBase, pos: BlockPos): Unit = player.swingArm(EnumHand.MAIN_HAND)

  def toolUsed(stack: ItemStack, player: EntityLivingBase, entity: Entity): Unit = player.swingArm(EnumHand.MAIN_HAND)

  // Compat for people shipping unofficial CoFH APIs... -.-

  def isUsable(stack: ItemStack, player: EntityLivingBase, x: Int, y: Int, z: Int): Boolean = true

  def toolUsed(stack: ItemStack, player: EntityLivingBase, x: Int, y: Int, z: Int): Unit = player.swingArm(EnumHand.MAIN_HAND)

  // EnderIO

  def canUse(stack: ItemStack, player: EntityPlayer, pos: BlockPos): Boolean = true

  def used(stack: ItemStack, player: EntityPlayer, pos: BlockPos): Unit = {}

  // Mekanism

  def canUseWrench(player: EntityPlayer, pos: BlockPos): Boolean = true

  def canUseWrench(stack: ItemStack, player: EntityPlayer, pos: BlockPos): Boolean = true

  // Project Red

  def canUse(entityPlayer: EntityPlayer, itemStack: ItemStack): Boolean = true

  // pre v4.7
  def damageScrewdriver(world: World, player: EntityPlayer): Unit = {}

  // v4.7+
  def damageScrewdriver(player: EntityPlayer, stack: ItemStack): Unit = {}

  // Railcraft

  def canWhack(player: EntityPlayer, stack: ItemStack, pos: BlockPos): Boolean = true

  def onWhack(player: EntityPlayer, stack: ItemStack, pos: BlockPos): Unit = {}

  def canLink(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Boolean = false

  def onLink(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Unit = {}

  def canBoost(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Boolean = false

  def onBoost(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Unit = {}

  // IndustrialCraft 2

  def canBeStoredInToolbox(stack: ItemStack): Boolean = true
}
