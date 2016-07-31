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
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

@Injectable.InterfaceList(Array(
  new Injectable.Interface(value = "appeng.api.implementations.items.IAEWrench", modid = Mods.IDs.AppliedEnergistics2),
  new Injectable.Interface(value = "buildcraft.api.tools.IToolWrench", modid = Mods.IDs.BuildCraftTools),
  new Injectable.Interface(value = "com.bluepowermod.api.misc.IScrewdriver", modid = Mods.IDs.BluePower),
  new Injectable.Interface(value = "cofh.api.item.IToolHammer", modid = Mods.IDs.CoFHItem),
  new Injectable.Interface(value = "crazypants.enderio.tool.ITool", modid = Mods.IDs.EnderIO),
  new Injectable.Interface(value = "mekanism.api.IMekWrench", modid = Mods.IDs.Mekanism),
  new Injectable.Interface(value = "powercrystals.minefactoryreloaded.api.IMFRHammer", modid = Mods.IDs.MineFactoryReloaded),
  new Injectable.Interface(value = "mrtjp.projectred.api.IScrewdriver", modid = Mods.IDs.ProjectRedCore),
  new Injectable.Interface(value = "mods.railcraft.api.core.items.IToolCrowbar", modid = Mods.IDs.Railcraft),
  new Injectable.Interface(value = "ic2.api.item.IBoxable", modid = Mods.IDs.IndustrialCraft2)
))
class Wrench extends traits.SimpleItem with api.internal.Wrench {
  setHarvestLevel("wrench", 1)
  setMaxStackSize(1)

  override def doesSneakBypassUse(world: World, pos: BlockPos, player: EntityPlayer): Boolean = true

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.isBlockLoaded(pos) && world.isBlockModifiable(player, pos) && (world.getBlockState(pos).getBlock match {
      case block: Block if block.rotateBlock(world, pos, side) =>
        block.onNeighborBlockChange(world, pos, world.getBlockState(pos), Blocks.air)
        player.swingItem()
        !world.isRemote
      case _ =>
        super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ)
    })
  }

  def useWrenchOnBlock(player: EntityPlayer, world: World, pos: BlockPos, simulate: Boolean): Boolean = {
    if (!simulate) player.swingItem()
    true
  }

  // Applied Energistics 2

  def canWrench(stack: ItemStack, player: EntityPlayer, pos: BlockPos): Boolean = true

  // BluePower
  def damage(stack: ItemStack, damage: Int, player: EntityPlayer, simulated: Boolean): Boolean = damage == 0

  // BuildCraft

  def canWrench(player: EntityPlayer, pos: BlockPos): Boolean = true

  def wrenchUsed(player: EntityPlayer, pos: BlockPos): Unit = player.swingItem()

  def canWrench(player: EntityPlayer, entity: Entity): Boolean = true

  def wrenchUsed(player: EntityPlayer, entity: Entity): Unit = player.swingItem()

  // CoFH

  def isUsable(stack: ItemStack, player: EntityLivingBase, pos: BlockPos): Boolean = true

  def toolUsed(stack: ItemStack, player: EntityLivingBase, pos: BlockPos): Unit = player.swingItem()

  // EnderIO

  def canUse(stack: ItemStack, player: EntityPlayer, pos: BlockPos): Boolean = true

  def used(stack: ItemStack, player: EntityPlayer, pos: BlockPos): Unit = {}

  // Mekanism

  def canUseWrench(player: EntityPlayer, pos: BlockPos): Boolean = true

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
