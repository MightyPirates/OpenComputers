package li.cil.oc.integration.fmp

import java.lang

import codechicken.lib.data.MCDataInput
import codechicken.lib.data.MCDataOutput
import codechicken.lib.raytracer.ExtendedMOP
import codechicken.lib.vec.Cuboid6
import codechicken.lib.vec.Vector3
import codechicken.multipart.IRedstonePart
import codechicken.multipart.TCuboidPart
import codechicken.multipart.TNormalOcclusion
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.Items
import li.cil.oc.client.renderer.block.Print
import li.cil.oc.common.block.Print
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import mods.immibis.redlogic.api.wiring.IRedstoneEmitter
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._

class PrintPart(val original: Option[tileentity.Print] = None) extends SimpleBlockPart with TCuboidPart with TNormalOcclusion with IRedstonePart {
  var facing = ForgeDirection.SOUTH
  var data = new PrintData()

  var boundsOff = ExtendedAABB.unitBounds
  var boundsOn = ExtendedAABB.unitBounds
  var state = false

  original.foreach(print => {
    facing = print.facing
    data = print.data
    boundsOff = print.boundsOff
    boundsOn = print.boundsOn
    state = print.state
  })

  // ----------------------------------------------------------------------- //

  override def simpleBlock = Items.get("print").block().asInstanceOf[Print]

  def getType = Settings.namespace + "print"

  override def doesTick = false

  override def getBounds = new Cuboid6(if (state) boundsOn else boundsOff)

  override def getOcclusionBoxes = {
    val shapes = if (state) data.stateOn else data.stateOff
    asJavaIterable(shapes.map(shape => new Cuboid6(shape.bounds.rotateTowards(facing))))
  }

  override def getCollisionBoxes = getOcclusionBoxes

  override def getRenderBounds = getBounds

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(side: Int): Boolean = true

  override def strongPowerLevel(side: Int): Int = weakPowerLevel(side)

  override def weakPowerLevel(side: Int): Int = if (data.emitRedstone && state) 15 else 0

  // ----------------------------------------------------------------------- //

  override def activate(player: EntityPlayer, hit: MovingObjectPosition, item: ItemStack): Boolean = {
    if (data.stateOn.size > 0) {
      if (!state || !data.isButtonMode) {
        toggleState()
        return true
      }
    }
    false
  }

  def toggleState(): Unit = {
    if (canToggle) {
      state = !state
      world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.click", 0.3F, if (state) 0.6F else 0.5F)
      world.notifyBlocksOfNeighborChange(x, y, z, tile.getBlockType)
      world.markBlockForUpdate(x, y, z)
      if (!world.isRemote) sendDescUpdate()
      tile.partList.foreach {
        case print: PrintPart if print != this => print.onNeighborChanged()
        case _ =>
      }
      if (state && data.isButtonMode) {
        scheduleTick(simpleBlock.tickRate(world))
      }
    }
  }

  def canToggle = {
    val toggled = new PrintPart()
    toggled.facing = facing
    toggled.data = data
    toggled.state = !state
    toggled.boundsOff = boundsOff
    toggled.boundsOn = boundsOn
    tile.canReplacePart(this, toggled)
  }

  // ----------------------------------------------------------------------- //

  override def scheduledTick(): Unit = if (state) toggleState()

  override def pickItem(hit: MovingObjectPosition): ItemStack = data.createItemStack()

  override def getDrops: lang.Iterable[ItemStack] = asJavaIterable(Iterable(data.createItemStack()))

  override def collisionRayTrace(start: Vec3, end: Vec3): ExtendedMOP = {
    val shapes = if (state) data.stateOn else data.stateOff
    var closestDistance = Double.PositiveInfinity
    var closest: Option[MovingObjectPosition] = None
    for (shape <- shapes) {
      val bounds = shape.bounds.rotateTowards(facing).offset(x, y, z)
      val hit = bounds.calculateIntercept(start, end)
      if (hit != null) {
        val distance = hit.hitVec.distanceTo(start)
        if (distance < closestDistance) {
          closestDistance = distance
          hit.blockX = x
          hit.blockY = y
          hit.blockZ = z
          closest = Option(hit)
        }
      }
    }
    closest.fold(if (shapes.isEmpty) new ExtendedMOP(x, y, z, 0, Vec3.createVectorHelper(0.5, 0.5, 0.5), null) else null)(hit => new ExtendedMOP(hit, null, closestDistance))
  }

  @SideOnly(Side.CLIENT)
  override def drawHighlight(hit: MovingObjectPosition, player: EntityPlayer, frame: Float): Boolean = {
    val pos = player.getPosition(frame)
    val expansion = 0.002f

    // See RenderGlobal.drawSelectionBox.
    GL11.glEnable(GL11.GL_BLEND)
    OpenGlHelper.glBlendFunc(770, 771, 1, 0)
    GL11.glColor4f(0, 0, 0, 0.4f)
    GL11.glLineWidth(2)
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    GL11.glDepthMask(false)

    for (shape <- if (state) data.stateOn else data.stateOff) {
      val bounds = shape.bounds.rotateTowards(facing)
      RenderGlobal.drawOutlinedBoundingBox(bounds.copy().expand(expansion, expansion, expansion)
        .offset(hit.blockX, hit.blockY, hit.blockZ)
        .offset(-pos.xCoord, -pos.yCoord, -pos.zCoord), -1)
    }

    GL11.glDepthMask(true)
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glDisable(GL11.GL_BLEND)

    true
  }

  override def onNeighborChanged(): Unit = {
    super.onNeighborChanged()
    val inner = tile.partList.foldLeft(false)((powered, part) => part match {
      case print: PrintPart => powered || (print.state && print.data.emitRedstone)
      case _ => powered
    })
    val newMaxValue = if (inner) 15 else ForgeDirection.VALID_DIRECTIONS.map(computeInput).max
    if (!data.emitRedstone && data.stateOn.size > 0) {
      val newState = newMaxValue > 0
      if (state != newState) {
        toggleState()
      }
    }
  }

  protected def computeInput(side: ForgeDirection) = {
    val blockPos = BlockPosition(x, y, z).offset(side)
    if (!world.blockExists(blockPos)) 0
    else {
      // See BlockRedstoneLogic.getInputStrength() for reference.
      val vanilla = math.max(world.getIndirectPowerLevelTo(blockPos, side),
        if (world.getBlock(blockPos) == Blocks.redstone_wire) world.getBlockMetadata(blockPos) else 0)
      val redLogic = if (Mods.RedLogic.isAvailable) {
        world.getTileEntity(blockPos) match {
          case emitter: IRedstoneEmitter =>
            var strength = 0
            for (i <- -1 to 5) {
              strength = math.max(strength, emitter.getEmittedSignalStrength(i, side.getOpposite.ordinal()))
            }
            strength
          case _ => 0
        }
      }
      else 0
      math.max(vanilla, redLogic)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    facing = nbt.getDirection("facing").getOrElse(facing)
    data.load(nbt.getCompoundTag("data"))
    state = nbt.getBoolean("state")
    updateBounds()
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDirection("facing", Option(facing))
    nbt.setNewCompoundTag("data", data.save)
    nbt.setBoolean("state", state)
  }

  override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    facing = ForgeDirection.getOrientation(packet.readUByte())
    data.load(packet.readNBTTagCompound())
    state = packet.readBoolean()
    updateBounds()
    if (world != null) {
      world.markBlockForUpdate(x, y, z)
    }
  }

  override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    packet.writeByte(facing.ordinal().toByte)
    val nbt = new NBTTagCompound()
    data.save(nbt)
    packet.writeNBTTagCompound(nbt)
    packet.writeBoolean(state)
  }

  def updateBounds(): Unit = {
    boundsOff = data.stateOff.drop(1).foldLeft(data.stateOff.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.func_111270_a(b.bounds))
    if (boundsOff.volume == 0) boundsOff = ExtendedAABB.unitBounds
    else boundsOff = boundsOff.rotateTowards(facing)
    boundsOn = data.stateOn.drop(1).foldLeft(data.stateOn.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.func_111270_a(b.bounds))
    if (boundsOn.volume == 0) boundsOn = ExtendedAABB.unitBounds
    else boundsOn = boundsOn.rotateTowards(facing)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def renderStatic(pos: Vector3, pass: Int) = {
    val (x, y, z) = (pos.x.toInt, pos.y.toInt, pos.z.toInt)
    val renderer = RenderBlocks.getInstance
    renderer.blockAccess = world
    Print.render(data, state, facing, x, y, z, simpleBlock, renderer)
    true
  }

}
