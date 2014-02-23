package li.cil.oc.common.block

import java.util
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.BuildCraft
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

abstract class Case(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  val unlocalizedName = "Case" + tier

  def tier: Int

  override def rarity = Array(EnumRarity.common, EnumRarity.uncommon, EnumRarity.rare).apply(tier)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    val slots = tier match {
      case 0 => "2/1/1"
      case 1 => "2/2/2"
      case 2 => "3/2/3"
    }
    tooltip.addAll(Tooltip.get("Case", slots))
  }

  private object Icons {
    val on = Array.fill[IIcon](6)(null)
    val off = Array.fill[IIcon](6)(null)
  }

  override def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) = {
    getIcon(localSide, world.getTileEntity(x, y, z) match {
      case computer: tileentity.Case => computer.isRunning
      case _ => false
    })
  }

  override def icon(side: ForgeDirection) = getIcon(side, isOn = false)

  private def getIcon(side: ForgeDirection, isOn: Boolean) =
    Some(if (isOn) Icons.on(side.ordinal) else Icons.off(side.ordinal))

  override def registerIcons(iconRegister: IIconRegister) = {
    Icons.off(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":case_top")
    Icons.on(ForgeDirection.DOWN.ordinal) = Icons.off(ForgeDirection.DOWN.ordinal)
    Icons.off(ForgeDirection.UP.ordinal) = Icons.off(ForgeDirection.DOWN.ordinal)
    Icons.on(ForgeDirection.UP.ordinal) = Icons.off(ForgeDirection.UP.ordinal)

    Icons.off(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":case_back")
    Icons.on(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":case_back_on")

    Icons.off(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":case_front")
    Icons.on(ForgeDirection.SOUTH.ordinal) = Icons.off(ForgeDirection.SOUTH.ordinal)

    Icons.off(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":case_side")
    Icons.on(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":case_side_on")
    Icons.off(ForgeDirection.EAST.ordinal) = Icons.off(ForgeDirection.WEST.ordinal)
    Icons.on(ForgeDirection.EAST.ordinal) = Icons.on(ForgeDirection.WEST.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World) = Some(new tileentity.Case(tier, world.isRemote))

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking && !BuildCraft.holdsApplicableWrench(player, x, y, z)) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Case.id, world, x, y, z)
      }
      true
    }
    else false
  }

  override def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) =
    world.getTileEntity(x, y, z) match {
      case c: tileentity.Case => c.canInteract(player.getCommandSenderName)
      case _ => super.removedByEntity(world, x, y, z, player)
    }
}

object Case {

  class Tier1(parent: SimpleDelegator) extends Case(parent) {
    def tier = 0

    override def color = 0x7F7F7F
  }

  class Tier2(parent: SimpleDelegator) extends Case(parent) {
    def tier = 1

    override def color = 0xFFFF66
  }

  class Tier3(parent: SimpleDelegator) extends Case(parent) {
    def tier = 2

    override def color = 0x66FFFF
  }

}
