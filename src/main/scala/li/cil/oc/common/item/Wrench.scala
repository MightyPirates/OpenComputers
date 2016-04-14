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
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

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

  override def doesSneakBypassUse(world: World, x: Int, y: Int, z: Int, player: EntityPlayer): Boolean = true

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.blockExists(x, y, z) && world.canMineBlock(player, x, y, z) && (world.getBlock(x, y, z) match {
      case block: Block if block.rotateBlock(world, x, y, z, ForgeDirection.getOrientation(side)) =>
        block.onNeighborBlockChange(world, x, y, z, Blocks.air)
        player.swingItem()
        !world.isRemote
      case _ =>
        super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    })
  }

  def useWrenchOnBlock(player: EntityPlayer, world: World, x: Int, y: Int, z: Int, simulate: Boolean): Boolean = {
    if (!simulate) player.swingItem()
    true
  }

  // Applied Energistics 2

  def canWrench(stack: ItemStack, player: EntityPlayer, x: Int, y: Int, z: Int): Boolean = true

  // BluePower
  def damage(stack: ItemStack, damage: Int, player: EntityPlayer, simulated: Boolean): Boolean = damage == 0

  // BuildCraft

  def canWrench(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean = true

  def wrenchUsed(player: EntityPlayer, x: Int, y: Int, z: Int): Unit = player.swingItem()

  def canWrench(player: EntityPlayer, entity: Entity): Boolean = true

  def wrenchUsed(player: EntityPlayer, entity: Entity): Unit = player.swingItem()

  // CoFH

  def isUsable(stack: ItemStack, player: EntityLivingBase, x: Int, y: Int, z: Int): Boolean = true

  def toolUsed(stack: ItemStack, player: EntityLivingBase, x: Int, y: Int, z: Int): Unit = player.swingItem()

  // EnderIO

  def canUse(stack: ItemStack, player: EntityPlayer, x: Int, y: Int, z: Int): Boolean = true

  def used(stack: ItemStack, player: EntityPlayer, x: Int, y: Int, z: Int): Unit = {}

  // Mekanism

  def canUseWrench(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean = true

  // Project Red

  def canUse(entityPlayer: EntityPlayer, itemStack: ItemStack): Boolean = true

  // pre v4.7
  def damageScrewdriver(world: World, player: EntityPlayer): Unit = {}

  // v4.7+
  def damageScrewdriver(player: EntityPlayer, stack: ItemStack): Unit = {}

  // Railcraft

  def canWhack(player: EntityPlayer, stack: ItemStack, x: Int, y: Int, z: Int): Boolean = true

  def onWhack(player: EntityPlayer, stack: ItemStack, x: Int, y: Int, z: Int): Unit = {}

  def canLink(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Boolean = false

  def onLink(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Unit = {}

  def canBoost(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Boolean = false

  def onBoost(player: EntityPlayer, stack: ItemStack, cart: EntityMinecart): Unit = {}

  // IndustrialCraft 2

  def canBeStoredInToolbox(stack: ItemStack): Boolean = true
}
