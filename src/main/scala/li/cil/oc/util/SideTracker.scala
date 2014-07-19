package li.cil.oc.util

import java.util.Collections

import cpw.mods.fml.common.FMLCommonHandler

object SideTracker {
  private val serverThreads = Collections.newSetFromMap(new java.util.WeakHashMap[Thread, java.lang.Boolean])

  def addServerThread() = serverThreads.add(Thread.currentThread())

  def isServer = FMLCommonHandler.instance.getEffectiveSide.isServer || serverThreads.contains(Thread.currentThread())

  def isClient = !isServer
}
