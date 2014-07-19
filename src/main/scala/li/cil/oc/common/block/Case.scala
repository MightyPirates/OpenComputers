package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.mods.{BuildCraft, Mods}
import li.cil.oc.util.{Color, Tooltip}
import li.cil.oc.{Localization, OpenComputers, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class Case(val parent: SimpleDelegator, val tier: Int) extends RedstoneAware with SimpleDelegate {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def customTextures = Array(
    Some("CaseTop"),
    Some("CaseTop"),
    Some("CaseBack"),
    Some("CaseFront"),
    Some("CaseSide"),
    Some("CaseSide")
  )

  private val iconsOn = new Array[Icon](6)

  override def rarity = Array(EnumRarity.common, EnumRarity.uncommon, EnumRarity.rare, EnumRarity.epic).apply(tier)

  @SideOnly(Side.CLIENT)
  override def color = Color.byTier(tier)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    val slots = tier match {
      case 0 => "2/1/1"
      case 1 => "2/2/2"
      case 2 | 3 => "3/2/3"
      case _ => "0/0/0"
    }
    tooltip.addAll(Tooltip.get("Case", slots))
  }

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val nbt = accessor.getNBTData
    val node = nbt.getCompoundTag(Settings.namespace + "computer").getCompoundTag("node")
    if (node.hasKey("address")) {
      tooltip.add(Localization.Analyzer.Address(node.getString("address")).toString)
    }
  }

  override def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) = {
    getIcon(localSide, world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Case => computer.isRunning
      case _ => false
    })
  }

  override def icon(side: ForgeDirection) = getIcon(side, isOn = false)

  private def getIcon(side: ForgeDirection, isOn: Boolean) =
    if (isOn) Some(iconsOn(side.ordinal)) else super.icon(side)

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    System.arraycopy(icons, 0, iconsOn, 0, icons.length)
    iconsOn(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":CaseBackOn")
    iconsOn(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":CaseSideOn")
    iconsOn(ForgeDirection.EAST.ordinal) = iconsOn(ForgeDirection.WEST.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World) = Some(new tileentity.Case(tier))

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking && !BuildCraft.holdsApplicableWrench(player, x, y, z)) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Case.id, world, x, y, z)
      }
      true
    }
    else if (player.getCurrentEquippedItem == null) {
      if (!world.isRemote) {
        world.getBlockTileEntity(x, y, z) match {
          case computer: tileentity.Case if !computer.isRunning => computer.start()
          case _ =>
        }
      }
      true
    }
    else false
  }

  override def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) =
    world.getBlockTileEntity(x, y, z) match {
      case c: tileentity.Case => c.canInteract(player.getCommandSenderName)
      case _ => super.removedByEntity(world, x, y, z, player)
    }
}
