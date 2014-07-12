package li.cil.oc.client.renderer.font

import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.{PathIterator, Point2D}

import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.{GLU, GLUtessellatorCallbackAdapter}

class FontCharRenderer(val font: Font) extends DynamicCharRenderer {
  private val context = new FontRenderContext(font.getTransform, true, true)
  private val callback = new FontCharRenderer.Callback()
  private val maxCharBounds = font.getMaxCharBounds(context)

  def charWidth = maxCharBounds.getWidth

  def charHeight = maxCharBounds.getHeight

  def drawChar(charCode: Int) {
    GL11.glPushMatrix()
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    GL11.glTranslated(-maxCharBounds.getX, -maxCharBounds.getY, 0)
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    GL11.glColor4f(1, 1, 1, 1)

    val vector = font.createGlyphVector(context, Array(charCode.toChar))

    val tess = GLU.gluNewTess()
    tess.gluTessCallback(GLU.GLU_TESS_BEGIN, callback)
    tess.gluTessCallback(GLU.GLU_TESS_END, callback)
    tess.gluTessCallback(GLU.GLU_TESS_VERTEX, callback)
    tess.gluTessCallback(GLU.GLU_TESS_EDGE_FLAG, callback)
    tess.gluTessNormal(0, 0, -1)

    for (i <- 0 until vector.getNumGlyphs) {
      val outline = vector.getGlyphOutline(i).getPathIterator(null)
      tess.gluTessBeginPolygon(null)
      val current = new Point2D.Double(0, 0)
      val coords = new Array[Double](6)
      while (!outline.isDone) {
        if (outline.getWindingRule == PathIterator.WIND_EVEN_ODD)
          tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD)
        else
          tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO)
        outline.currentSegment(coords) match {
          case PathIterator.SEG_MOVETO =>
            tess.gluTessBeginContour()
            current.setLocation(coords(0), coords(1))
          case PathIterator.SEG_LINETO =>
            val buffer = Array[Float](coords(0).toFloat, coords(1).toFloat)
            tess.gluTessVertex(coords, 0, buffer)
            current.setLocation(coords(0), coords(1))
          case PathIterator.SEG_QUADTO =>
            // From SEG_QUADTO:
            // P(t) = B(2,0)*CP + B(2,1)*P1 + B(2,2)*P2
            // with 0 <= t <= 1
            //      B(n,m) = mth coefficient of nth degree Bernstein polynomial
            //             = C(n,m) * t^(m) * (1 - t)^(n-m)
            //      C(n,m) = Combinations of n things, taken m at a time
            //             = n! / (m! * (n-m)!)
            // So:
            // P(t) = B(2,0)*CP + B(2,1)*P1 + B(2,2)*P2
            //      = C(2,0) * t^(0) * (1 - t)^(2-0) * CP +
            //        C(2,1) * t^(1) * (1 - t)^(2-1) * P1 +
            //        C(2,2) * t^(2) * (1 - t)^(2-2) * P2
            //      = 2! / (0! * (2-0)!) * (1 - t)^2 * CP +
            //        2! / (1! * (2-1)!) * t * (1 - t) * P1 +
            //        2! / (2! * (2-2)!) * t^2 * P2
            //      = (1 - t)^2 * CP +
            //        2 * t * (1 - t) * P1 +
            //        t^2 * P2
            //      = (1 - 2*t + t^2) * CP +
            //        2 * (t - t^2) * P1 +
            //        t^2 * P2
            val interpolated = new Array[Double](3)
            def p(t: Double) = {
              val tSquared = t * t
              val fc = 1 - 2 * t + tSquared
              val f1 = 2 * (t - tSquared)
              val f2 = tSquared
              interpolated(0) = fc * current.x + f1 * coords(0) + f2 * coords(2)
              interpolated(1) = fc * current.y + f1 * coords(1) + f2 * coords(3)
              val buffer = Array[Float](interpolated(0).toFloat, interpolated(1).toFloat)
              tess.gluTessVertex(interpolated, 0, buffer)
            }
            for (t <- 0.0 until 1.0 by 0.25) {
              p(t)
            }
            current.setLocation(coords(2), coords(3))
          case PathIterator.SEG_CUBICTO =>
          // Not supported.
          case PathIterator.SEG_CLOSE =>
            tess.gluTessEndContour()
          case _ => // Wuh?
        }
        outline.next()
      }
      tess.gluTessEndPolygon()
    }

    tess.gluDeleteTess()

    GL11.glPopAttrib()
    GL11.glPopMatrix()
  }
}

object FontCharRenderer {

  private class Callback extends GLUtessellatorCallbackAdapter {
    override def begin(mode: Int) = GL11.glBegin(mode)

    override def end() = GL11.glEnd()

    override def vertex(coords: scala.Any) {
      val point = coords.asInstanceOf[Array[Float]]
      GL11.glVertex2f(point(0), point(1))
    }

    override def edgeFlag(boundaryEdge: Boolean) {
      GL11.glEdgeFlag(boundaryEdge)
    }
  }

}