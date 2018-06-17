package li.cil.oc.client.renderer.font

import li.cil.oc.Settings
import li.cil.oc.OpenComputers
import li.cil.oc.util.PackedColor
import li.cil.oc.util.RenderState
import li.cil.oc.util.TextBuffer
import li.cil.oc.util.FontUtils
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

object TextureFontRenderer {
  private val maxCharsRes = math.max(Settings.screenResolutionsByTier.last._1, Settings.screenResolutionsByTier.last._2)
  
  private val _glyphProvider: IGlyphProvider = Settings.get.fontRenderer match {
    case _ => new FontParserHex()
  }
  
  _glyphProvider.initialize()
  
  //val texMaxPxWidth = maxCharsRes * _glyphProvider.getGlyphWidth 
  //val texMaxPxHeight = maxCharsRes * _glyphProvider.getGlyphHeight
  
  
	def createTexture(): Int = {
    var texID = GL11.glGenTextures()
    
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
    
    if (Settings.get.textLinearFiltering) {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    } else {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    }
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    
    
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, _glyphProvider.getGlyphWidth, _glyphProvider.getGlyphHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, BufferUtils.createByteBuffer(_glyphProvider.getGlyphWidth * _glyphProvider.getGlyphHeight * 4))
    
    texID
  }
}

class TextureFontRenderer {
  protected final val basicChars = """☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■ """
  
  protected final val glyphProvider =  TextureFontRenderer._glyphProvider
  final val charRenderWidth = glyphProvider.getGlyphWidth
  final val charRenderHeight = glyphProvider.getGlyphHeight
  
  private final val staticbuff = BufferUtils.createByteBuffer(TextureFontRenderer.maxCharsRes * charRenderWidth * 4 * charRenderHeight);


  def drawBuffer(buffer: TextBuffer, viewportWidth: Int, viewportHeight: Int, texID: Int, forceRefresh: Boolean) {
    val format = buffer.format

    GL11.glPushMatrix()
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    //GlStateManager.scale(0.5f, 0.5f, 1)

    GL11.glDepthMask(false)
    RenderState.makeItBlend()
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)

    RenderState.checkError(getClass.getName + ".drawBuffer: configure state")
    
    // Create texture if it does not exist
    var texCharsWidth = viewportWidth
    var texCharsHeight = viewportHeight max buffer.height
    var texPxWidth = viewportWidth * charRenderWidth
    var texPxHeight = texCharsHeight * charRenderHeight
    
    
    var currentPxWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH)
    var currentPxHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT)
    if (texPxWidth != currentPxWidth || texPxHeight != currentPxHeight) {
      GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, texPxWidth, texPxHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, BufferUtils.createByteBuffer(texPxWidth * texPxHeight * 4))
    }
    

    if (Settings.get.textLinearFiltering) {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    }
    
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val line = buffer.buffer(y)
      val color = buffer.color(y)
      val dirty = buffer.dirty(y)
      
      var ty = y * charRenderHeight
      var tx = 0
      
      var backlogData = new Array[Int](viewportWidth)
      var backlogPx = -1
      var backlogSize = 0
      var backlogWidth = 0
      
      var flush = () => {
        var pxStrideOffset = 0
          
        for (backlogIdx <- 0 until backlogSize) {
          val lineIdx = backlogData(backlogIdx)
          val char = line(lineIdx);
          val charWidth = FontUtils.wcwidth(char) * charRenderWidth
          
          val col = PackedColor.unpackForeground(color(lineIdx), format)
          val back = PackedColor.unpackBackground(color(lineIdx), format)
          
          staticbuff.position(pxStrideOffset * 4)
          glyphProvider.getGlyph(char, col, back, staticbuff, backlogWidth - charWidth)
          
          pxStrideOffset += charWidth
        }
        
    	  staticbuff.position(0)
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, backlogPx, ty, backlogWidth, charRenderHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, staticbuff)
        
        backlogPx = -1
        backlogSize = 0
        backlogWidth = 0
      }
      
      var n = 0
      while (n < viewportWidth) {
        val nChar = line(n)
        val nCharWidth = FontUtils.wcwidth(nChar)
        val nCharWidthPx = nCharWidth * charRenderWidth;
        
        if (dirty(n) || forceRefresh) {
          dirty(n) = false
          
          backlogData(backlogSize) = n
          backlogSize += 1
          backlogWidth += nCharWidthPx
          
          if (backlogPx == -1) {
            backlogPx = tx
          }
          
        } else if (backlogSize != 0) {
          // this one is not dirty, but the last one was
          flush()
        }
        tx += nCharWidthPx;
      
        // OC represents wide chars as a wide char followed by a space
        // The wide char will render over into that space so we can skip the next char
        n += nCharWidth;
      }
    	if (backlogSize != 0) {
          flush()
    	}
    }
    
    GL11.glBegin(GL11.GL_QUADS)
      var tx = 0
      var ty = 0
      var h = (viewportHeight min buffer.height) * charRenderHeight
      var w = viewportWidth * charRenderWidth
      var u1 = 0
      var u2 = w.floatValue() / texPxWidth
      var v1 = 0
      var v2 = h.floatValue() / texPxHeight
      GL11.glTexCoord2d(u1, v2)
      GL11.glVertex2f(tx, ty + h)
      GL11.glTexCoord2d(u2, v2)
      GL11.glVertex2f(tx + w, ty + h)
      GL11.glTexCoord2d(u2, v1)
      GL11.glVertex2f(tx + w, ty)
      GL11.glTexCoord2d(u1, v1)
      GL11.glVertex2f(tx, ty)
    GL11.glEnd()

    RenderState.checkError(getClass.getName + ".drawBuffer: foreground")

    GL11.glPopAttrib()
    GL11.glPopMatrix()

    RenderState.checkError(getClass.getName + ".drawBuffer: leaving")
  }
}
