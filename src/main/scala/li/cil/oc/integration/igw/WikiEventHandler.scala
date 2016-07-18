package li.cil.oc.integration.igw

import java.util

import igwmod.api.PageChangeEvent
import igwmod.api.WikiRegistry
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.client.Manual
import li.cil.oc.client.renderer.markdown
import li.cil.oc.client.renderer.markdown.MarkupFormat
import li.cil.oc.common.init.Items
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object WikiEventHandler {
  var lastPath = ""

  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)

    for ((name, info) <- Items.descriptors) {
      val stack = info.createItemStack(1)
      val path = api.Manual.pathFor(stack)
      if (path != null && api.Manual.contentFor(path) != null) {
        WikiRegistry.registerBlockAndItemPageEntry(stack)
      }
    }
  }

  @SubscribeEvent
  def onPageChangeEvent(e: PageChangeEvent): Unit = {
    val path =
      if (e.associatedStack != null)
        "/" + api.Manual.pathFor(e.associatedStack)
      else if (e.currentFile.startsWith(OpenComputers.ID + ":"))
        e.currentFile.stripPrefix(OpenComputers.ID + ":")
      else null

    val base = lastPath
    lastPath = ""
    if (path != null) {
      val resolvedPath = Manual.makeRelative(path, base)
      val content = api.Manual.contentFor(resolvedPath)
      if (content != null) {
        val document = markdown.Document.parse(content)
        val processed = document.renderAsText(MarkupFormat.IGWMod)
        e.pageText = new util.ArrayList[String](asJavaCollection(processed))
        e.currentFile = resolvedPath
        lastPath = resolvedPath
      }
    }
  }
}
