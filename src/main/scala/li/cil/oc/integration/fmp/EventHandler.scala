package li.cil.oc.integration.fmp

import codechicken.lib.packet.PacketCustom
import codechicken.lib.raytracer.RayTracer
import codechicken.lib.vec.BlockCoord
import codechicken.lib.vec.Vector3
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api.Items
import li.cil.oc.client.PacketSender
import li.cil.oc.common.block.SimpleBlock
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action

object EventHandler {
  private var currentlyPlacing = false

  private val yaw2Direction = Array(ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST)

  @SubscribeEvent
  def playerInteract(event: PlayerInteractEvent) {
    this.synchronized {
      if (currentlyPlacing) return
      try {
        currentlyPlacing = true
        val player = event.entityPlayer
        if (event.action == Action.RIGHT_CLICK_BLOCK && player.getEntityWorld.isRemote) {
          if (place(player)) {
            event.setCanceled(true)
          }
        }
      }
      finally {
        currentlyPlacing = false
      }
    }
  }

  def place(player: EntityPlayer) = {
    val world = player.getEntityWorld
    val hit = RayTracer.reTrace(world, player)
    if (hit != null && player.getHeldItem != null) player.getHeldItem.getItem match {
      case itemBlock: ItemBlock =>
        itemBlock.field_150939_a match {
          case simpleBlock: SimpleBlock =>
            if (simpleBlock == Items.get(Constants.BlockName.Cable).block()) {
              placeDelegatePart(player, hit, new CablePart())
            }
            else if (simpleBlock == Items.get(Constants.BlockName.Print).block()) {
              val part = new PrintPart()
              part.data.load(player.getHeldItem)
              part.facing = yaw2Direction((player.rotationYaw / 360 * 4).round & 3).getOpposite
              placeDelegatePart(player, hit, part)
            }
            else false
          case _ => false
        }
      case _ => false
    }
    else false
  }


  protected def placeDelegatePart(player: EntityPlayer, hit: MovingObjectPosition, part: SimpleBlockPart): Boolean = {
    val world = player.getEntityWorld
    if (world.isRemote && !player.isSneaking) {
      // Attempt to use block activated like normal and tell the server the right stuff
      val f = new Vector3(hit.hitVec.xCoord - hit.blockX, hit.hitVec.yCoord - hit.blockY, hit.hitVec.zCoord - hit.blockZ)
      val block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ)
      if (block != null && block.onBlockActivated(world, hit.blockX, hit.blockY, hit.blockZ, player, hit.sideHit, f.x.toFloat, f.y.toFloat, f.z.toFloat)) {
        player.swingItem()
        PacketCustom.sendToServer(new C08PacketPlayerBlockPlacement(
          hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
          player.inventory.getCurrentItem,
          f.x.toFloat, f.y.toFloat, f.z.toFloat))
        return true
      }
    }

    val pos = new BlockCoord(hit.blockX, hit.blockY, hit.blockZ)
    val posOutside = pos.copy().offset(hit.sideHit)
    val inside = Option(TileMultipart.getOrConvertTile(world, pos))
    val outside = Option(TileMultipart.getOrConvertTile(world, posOutside))
    inside match {
      case Some(t) if t.canAddPart(part) && canAddPrint(t, part) => placeMultiPart(player, part, pos)
      case _ => outside match {
        case Some(t) if t.canAddPart(part) && canAddPrint(t, part) => placeMultiPart(player, part, posOutside)
        case _ => false
      }
    }
  }

  protected def canAddPrint(t: TileMultipart, p: SimpleBlockPart): Boolean = p match {
    case print: PrintPart =>
      val (offSum, onSum) = t.partList.foldLeft((print.data.stateOff.size, print.data.stateOn.size))((acc, part) => {
        val (offAcc, onAcc) = acc
        val (offCount, onCount) = part match {
          case innerPrint: PrintPart => (innerPrint.data.stateOff.size, innerPrint.data.stateOn.size)
          case _ => (0, 0)
        }
        (offAcc + offCount, onAcc + onCount)
      })
      offSum <= Settings.get.maxPrintComplexity && onSum <= Settings.get.maxPrintComplexity
    case _ => true
  }

  protected def placeMultiPart(player: EntityPlayer, part: SimpleBlockPart, pos: BlockCoord) = {
    val world = player.getEntityWorld
    if (world.isRemote) {
      player.swingItem()
      PacketSender.sendMultiPlace()
    }
    else {
      TileMultipart.addPart(world, pos, part)
      world.playSoundEffect(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
        part.simpleBlock.stepSound.func_150496_b,
        (part.simpleBlock.stepSound.getVolume + 1.0F) / 2.0F,
        part.simpleBlock.stepSound.getPitch * 0.8F)
      if (!player.capabilities.isCreativeMode) {
        val held = player.getHeldItem
        held.stackSize -= 1
        if (held.stackSize == 0) {
          player.inventory.mainInventory(player.inventory.currentItem) = null
          MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, held))
        }
      }
    }
    true
  }
}