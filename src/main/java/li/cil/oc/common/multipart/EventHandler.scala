package li.cil.oc.common.multipart

import codechicken.lib.packet.PacketCustom
import codechicken.lib.raytracer.RayTracer
import codechicken.lib.vec.{Vector3, BlockCoord}
import codechicken.multipart.{TileMultipart, TMultiPart}
import li.cil.oc.Blocks
import li.cil.oc.client.PacketSender
import li.cil.oc.common.block.Delegator
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.packet.Packet15Place
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action
import net.minecraftforge.event.entity.player.{PlayerDestroyItemEvent, PlayerInteractEvent}

object EventHandler {
  @ForgeSubscribe
  def playerInteract(event: PlayerInteractEvent) {
    val player = event.entityPlayer
    if (event.action == Action.RIGHT_CLICK_BLOCK && player.getEntityWorld.isRemote) {
      if (place(player)) {
        event.setCanceled(true)
      }
    }
  }

  def place(player: EntityPlayer): Boolean = {
    val world = player.getEntityWorld
    val hit = RayTracer.reTrace(world, player)
    if (hit == null)
      return false

    val held = player.getHeldItem
    var part: TMultiPart = null
    if (held == null)
      return false

    Delegator.subBlock(held) match {
      case Some(subBlock) if subBlock == Blocks.cable =>
        part = new CablePart()
      case _ =>
    }

    if (part == null)
      return false

    // attempt to use block activated like normal and tell the server the right stuff
    if (world.isRemote && !player.isSneaking) {
      val f = new Vector3(hit.hitVec).add(-hit.blockX, -hit.blockY, -hit.blockZ)
      val block = Block.blocksList(world.getBlockId(hit.blockX, hit.blockY, hit.blockZ))
      if (block != null && block.onBlockActivated(world, hit.blockX, hit.blockY, hit.blockZ, player, hit.sideHit, f.x.toFloat, f.y.toFloat, f.z.toFloat)) {
        player.swingItem()
        PacketCustom.sendToServer(new Packet15Place(
          hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
          player.inventory.getCurrentItem,
          f.x.toFloat, f.y.toFloat, f.z.toFloat))
        return false
      }
    }

    val pos = new BlockCoord(hit.blockX, hit.blockY, hit.blockZ)
    val posOutside = pos.copy().offset(hit.sideHit)
    val inside = Option(TileMultipart.getOrConvertTile(world, pos))
    val outside = Option(TileMultipart.getOrConvertTile(world, posOutside))
    inside match {
      case Some(t) if t.canAddPart(part) => placeMultiPart(player, part, pos)
      case _ => outside match {
        case Some(t) if t.canAddPart(part) => placeMultiPart(player, part, posOutside)
        case _ => false
      }
    }
  }

  protected def placeMultiPart(player: EntityPlayer, part: TMultiPart, pos: BlockCoord) = {
    val world = player.getEntityWorld
    if (!world.isRemote) {
      TileMultipart.addPart(world, pos, part)
      world.playSoundEffect(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
        Blocks.cable.parent.stepSound.getPlaceSound,
        (Blocks.cable.parent.stepSound.getVolume + 1.0F) / 2.0F,
        Blocks.cable.parent.stepSound.getPitch * 0.8F)
      if (!player.capabilities.isCreativeMode) {
        val held = player.getHeldItem
        held.stackSize -= 1
        if (held.stackSize == 0) {
          player.inventory.mainInventory(player.inventory.currentItem) = null
          MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, held))
        }
      }
    }
    else {
      player.swingItem()
      PacketSender.sendMultiPlace()
    }
    true
  }
}
