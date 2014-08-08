package li.cil.oc.common.multipart

import codechicken.lib.packet.PacketCustom
import codechicken.lib.raytracer.RayTracer
import codechicken.lib.vec.{BlockCoord, Vector3}
import codechicken.multipart.TileMultipart
import li.cil.oc.api.Items
import li.cil.oc.client.PacketSender
import li.cil.oc.common.block.Delegator
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.packet.Packet15Place
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action
import net.minecraftforge.event.entity.player.{PlayerDestroyItemEvent, PlayerInteractEvent}

object EventHandler {
  private var currentlyPlacing = false

  @ForgeSubscribe
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
    if (hit != null) Delegator.subBlock(player.getHeldItem) match {
      case Some(subBlock) if subBlock == Delegator.subBlock(Items.get("cable").createItemStack(1)).get => placeDelegatePart(player, hit, new CablePart())
      case _ => false
    }
    else false
  }

  protected def placeDelegatePart(player: EntityPlayer, hit: MovingObjectPosition, part: DelegatePart): Boolean = {
    val world = player.getEntityWorld
    if (world.isRemote && !player.isSneaking) {
      // Attempt to use block activated like normal and tell the server the right stuff
      val f = new Vector3(hit.hitVec.xCoord - hit.blockX, hit.hitVec.yCoord - hit.blockY, hit.hitVec.zCoord - hit.blockZ)
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

  protected def placeMultiPart(player: EntityPlayer, part: DelegatePart, pos: BlockCoord) = {
    val world = player.getEntityWorld
    if (world.isRemote) {
      player.swingItem()
      PacketSender.sendMultiPlace()
    }
    else {
      TileMultipart.addPart(world, pos, part)
      world.playSoundEffect(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
        part.delegate.parent.stepSound.getPlaceSound,
        (part.delegate.parent.stepSound.getVolume + 1.0F) / 2.0F,
        part.delegate.parent.stepSound.getPitch * 0.8F)
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
