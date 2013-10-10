package li.cil.oc.client

import com.google.common.cache.{CacheBuilder, RemovalNotification, RemovalListener}
import cpw.mods.fml.common.{TickType, ITickHandler}
import java.util
import java.util.concurrent.{TimeUnit, Callable}
import li.cil.oc.client.gui.MonospaceFontRenderer
import li.cil.oc.common.tileentity.Screen
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.{GLAllocation, OpenGlHelper}
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.{GL14, GL11}

object ScreenRenderer extends TileEntitySpecialRenderer with Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {

  private val maxRenderDistanceSq = 6 * 6

  private val fadeDistanceSq = 2 * 2

  /** We cache the display lists for the screens we render for performance. */
  val cache = com.google.common.cache.CacheBuilder.newBuilder().
    weakKeys().
    expireAfterAccess(5, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[Screen, Int]].build[Screen, Int]()

  /** Used to pass the current screen along to call(). */
  private var tileEntity: Screen = null

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def renderTileEntityAt(t: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    val player = Minecraft.getMinecraft.thePlayer
    val playerDistance = player.getDistanceSq(t.xCoord + 0.5, t.yCoord + 0.5, t.zCoord + 0.5).toFloat
    if (playerDistance > maxRenderDistanceSq)
      return

    tileEntity = t.asInstanceOf[Screen]

    // Crude check whether screen text can be seen by the local player based
    // on the player's look direction -> angle relative to screen.
    val screenFacing = tileEntity.facing.getOpposite
    val screenFacingVec = t.worldObj.getWorldVec3Pool.
      getVecFromPool(screenFacing.offsetX, screenFacing.offsetY, screenFacing.offsetZ)
    val playerFacingVec = player.getLookVec
    if (playerFacingVec.dotProduct(screenFacingVec) <= 0)
      return

    GL11.glPushAttrib(0xFFFFFF)
    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    GL11.glDepthMask(false)
    GL11.glDisable(GL11.GL_LIGHTING)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR)
    GL11.glDepthFunc(GL11.GL_LEQUAL)

    if (playerDistance > fadeDistanceSq) {
      val fade = 1f min ((playerDistance - fadeDistanceSq) / (maxRenderDistanceSq - fadeDistanceSq))
      GL14.glBlendColor(0, 0, 0, 1 - fade)
      //GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA​)
      GL11.glBlendFunc(0x8003, 0x8004) // For some reason the compiler doesn't like the above.
    }
    MonospaceFontRenderer.init(tileEntityRenderer.renderEngine)
    val list = cache.get(tileEntity, this)
    compile(list)
    GL11.glCallList(list)

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }

  private def compile(list: Int) = if (tileEntity.hasChanged) {
    tileEntity.hasChanged = false

    GL11.glNewList(list, GL11.GL_COMPILE)

    tileEntity.yaw match {
      case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }
    tileEntity.pitch match {
      case ForgeDirection.DOWN => GL11.glRotatef(90, 1, 0, 0)
      case ForgeDirection.UP => GL11.glRotatef(-90, 1, 0, 0)
      case _ => // No pitch.
    }

    // Fit area to screen (top left = top left).
    GL11.glTranslatef(-0.5f, 0.5f, 0.501f)

    // Scale to inner screen size and offset it.
    GL11.glTranslatef(2.25f / 16f, -2.25f / 16f, 0)
    GL11.glScalef(11.5f / 16f, 11.5f / 16f, 1)

    // Scale based on actual buffer size.
    val (w, h) = tileEntity.screen.resolution
    val scale = 1f / ((w * MonospaceFontRenderer.fontWidth) max (h * MonospaceFontRenderer.fontHeight))
    GL11.glScalef(scale, scale, 1)

    // Flip text upside down.
    GL11.glScalef(1, -1, 1)

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 200, 200)

    for ((line, i) <- tileEntity.screen.lines.zipWithIndex) {
      MonospaceFontRenderer.drawString(line, 0, i * MonospaceFontRenderer.fontHeight)
    }

    GL11.glEndList()
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    val list = GLAllocation.generateDisplayLists(1)
    tileEntity.hasChanged = true // Force compilation.
    compile(list)
    list
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) =
    GLAllocation.deleteDisplayLists(e.getValue)

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  def getLabel = "OpenComputers.Screen"

  def ticks() = util.EnumSet.of(TickType.CLIENT)

  def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) = cache.cleanUp()

  def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}