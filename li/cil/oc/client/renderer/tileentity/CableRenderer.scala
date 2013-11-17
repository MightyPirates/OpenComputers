package li.cil.oc.client.renderer.tileentity

import li.cil.oc.Config
import li.cil.oc.common.tileentity.Cable
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.{OpenGlHelper, Tessellator, GLAllocation}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object CableRenderer extends TileEntitySpecialRenderer {
  private val texture = new ResourceLocation(Config.resourceDomain, "textures/blocks/cable.png")

  private val displayLists = GLAllocation.generateDisplayLists(64)

  def compileLists() {
    val t = Tessellator.instance
    val lb = 0.375
    val ub = 0.625
    val s = 0.25
    val t1 = Array(ub, ub, lb, lb)
    val t2 = Array(lb, ub, ub, lb)
    val uv1 = Array(0, 0, 0.5, 0.5)
    val uv2 = Array(0.5, 0, 0, 0.5)
    val uo = 4.0 / 8.0
    val vs = 6.0 / 4.0
    val translations = Array(Array(lb, 0, 0), Array(0, lb, 0), Array(0, 0, lb))
    val offsets = Array(Array(s, 0, 0), Array(0, s, 0), Array(0, 0, s))
    val light = Array(0.4f, 0.6f, 0.8f, 1.0f)
    def uv(i: Int) = (i + 4) % 4
    def brightness(i: Int) = t.setColorOpaque_F(light(i), light(i), light(i))

    for (mask <- 0 to 0xFF >> 2) {
      GL11.glNewList(displayLists + mask, GL11.GL_COMPILE)
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val connects = (side.flag & mask) != 0
        val z = if (connects) 0 else lb
        val uc = if (ForgeDirection.VALID_DIRECTIONS.
          filter(_ != side).filter(_ != side.getOpposite).
          exists(s => (s.flag & mask) != 0)) uo
        else 0
        t.startDrawingQuads()
        val (tx, ty, tz, u, v) = side match {
          case ForgeDirection.WEST =>
            brightness(2)
            (Array.fill(4)(z), t2, t1, uv1.reverse, uv2)
          case ForgeDirection.EAST =>
            brightness(2)
            (Array.fill(4)(1 - z), t1, t2, uv2, uv1)
          case ForgeDirection.DOWN =>
            brightness(0)
            (t1, Array.fill(4)(z), t2, uv1.reverse, uv2)
          case ForgeDirection.UP =>
            brightness(3)
            (t2, Array.fill(4)(1 - z), t1, uv2, uv1)
          case ForgeDirection.NORTH =>
            brightness(1)
            (t2, t1, Array.fill(4)(z), uv2, uv1)
          case ForgeDirection.SOUTH =>
            brightness(1)
            (t1, t2, Array.fill(4)(1 - z), uv1.reverse, uv2)
          case _ => throw new AssertionError()
        }
        t.addVertexWithUV(tx(0), ty(0), tz(0), u(0) + uc, v(0))
        t.addVertexWithUV(tx(1), ty(1), tz(1), u(1) + uc, v(1))
        t.addVertexWithUV(tx(2), ty(2), tz(2), u(2) + uc, v(2))
        t.addVertexWithUV(tx(3), ty(3), tz(3), u(3) + uc, v(3))
        t.draw()

        if (connects) {
          val (axis, sign, uv1, uv2, uv3, uv4, l0, l1, l2, l3) = side match {
            case ForgeDirection.WEST => (0, -1, 1, 1, 1, 1, 1, 1, 0, 3)
            case ForgeDirection.EAST => (0, 1, 3, 3, 1, 1, 3, 0, 1, 1)
            case ForgeDirection.DOWN => (1, -1, 1, 3, 2, 0, 2, 2, 1, 1)
            case ForgeDirection.UP => (1, 1, 2, 0, 3, 1, 1, 1, 2, 2)
            case ForgeDirection.NORTH => (2, -1, 0, 2, 1, 1, 3, 0, 2, 2)
            case ForgeDirection.SOUTH => (2, 1, 1, 1, 0, 2, 2, 2, 0, 3)
            case _ => throw new AssertionError()
          }
          val tl = translations(axis)
          val o1 = offsets((axis + sign + 3) % 3)
          val o2 = offsets((axis - sign + 3) % 3)

          t.startDrawingQuads()
          brightness(l0)
          t.addVertexWithUV(tx(0) - sign * tl(0), ty(0) - sign * tl(1), tz(0) - sign * tl(2), u(uv(0 + uv1)) + uo, v(uv(0 + uv1)) * vs)
          t.addVertexWithUV(tx(1) - sign * tl(0), ty(1) - sign * tl(1), tz(1) - sign * tl(2), u(uv(1 + uv1)) + uo, v(uv(1 + uv1)) * vs)
          t.addVertexWithUV(tx(2) + o1(0), ty(2) + o1(1), tz(2) + o1(2), u(uv(2 + uv1)) + uo, v(uv(2 + uv1)) * vs)
          t.addVertexWithUV(tx(3) + o1(0), ty(3) + o1(1), tz(3) + o1(2), u(uv(3 + uv1)) + uo, v(uv(3 + uv1)) * vs)
          t.draw()

          t.startDrawingQuads()
          brightness(l1)
          t.addVertexWithUV(tx(0) - o1(0), ty(0) - o1(1), tz(0) - o1(2), u(uv(0 + uv2)) + uo, v(uv(0 + uv2)) * vs)
          t.addVertexWithUV(tx(1) - o1(0), ty(1) - o1(1), tz(1) - o1(2), u(uv(1 + uv2)) + uo, v(uv(1 + uv2)) * vs)
          t.addVertexWithUV(tx(2) - sign * tl(0), ty(2) - sign * tl(1), tz(2) - sign * tl(2), u(uv(2 + uv2)) + uo, v(uv(2 + uv2)) * vs)
          t.addVertexWithUV(tx(3) - sign * tl(0), ty(3) - sign * tl(1), tz(3) - sign * tl(2), u(uv(3 + uv2)) + uo, v(uv(3 + uv2)) * vs)
          t.draw()

          t.startDrawingQuads()
          brightness(l2)
          t.addVertexWithUV(tx(0) - sign * tl(0), ty(0) - sign * tl(1), tz(0) - sign * tl(2), u(uv(0 + uv3)) + uo, v(uv(0 + uv3)) * vs)
          t.addVertexWithUV(tx(1) - o2(0), ty(1) - o2(1), tz(1) - o2(2), u(uv(1 + uv3)) + uo, v(uv(1 + uv3)) * vs)
          t.addVertexWithUV(tx(2) - o2(0), ty(2) - o2(1), tz(2) - o2(2), u(uv(2 + uv3)) + uo, v(uv(2 + uv3)) * vs)
          t.addVertexWithUV(tx(3) - sign * tl(0), ty(3) - sign * tl(1), tz(3) - sign * tl(2), u(uv(3 + uv3)) + uo, v(uv(3 + uv3)) * vs)
          t.draw()

          t.startDrawingQuads()
          brightness(l3)
          t.addVertexWithUV(tx(0) + o2(0), ty(0) + o2(1), tz(0) + o2(2), u(uv(0 + uv4)) + uo, v(uv(0 + uv4)) * vs)
          t.addVertexWithUV(tx(1) - sign * tl(0), ty(1) - sign * tl(1), tz(1) - sign * tl(2), u(uv(1 + uv4)) + uo, v(uv(1 + uv4)) * vs)
          t.addVertexWithUV(tx(2) - sign * tl(0), ty(2) - sign * tl(1), tz(2) - sign * tl(2), u(uv(2 + uv4)) + uo, v(uv(2 + uv4)) * vs)
          t.addVertexWithUV(tx(3) + o2(0), ty(3) + o2(1), tz(3) + o2(2), u(uv(3 + uv4)) + uo, v(uv(3 + uv4)) * vs)
          t.draw()
        }
      }

      GL11.glEndList()
    }
  }

  compileLists()

  def renderTileEntityAt(t: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    val cable = t.asInstanceOf[Cable]

    val l = t.getWorldObj.getLightBrightnessForSkyBlocks(t.xCoord, t.yCoord, t.zCoord, 0)
    val l1 = l % 65536
    val l2 = l / 65536
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l1, l2)

    GL11.glPushMatrix()
    GL11.glTranslated(x, y, z)

    bindTexture(texture)
    GL11.glCallList(displayLists + cable.neighbors)

    GL11.glPopMatrix()
  }
}
