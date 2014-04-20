package li.cil.oc.server.fs

import java.io.{FileNotFoundException, IOException}
import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable
import java.nio.channels.SeekableByteChannel
import java.nio.ByteBuffer

trait InputStreamFileSystem extends api.fs.FileSystem {
  private val handles = mutable.Map.empty[Int, Handle]

  // ----------------------------------------------------------------------- //

  override def isReadOnly = true

  override def delete(path: String) = false

  override def makeDirectory(path: String) = false

  override def rename(from: String, to: String) = false

  override def setLastModified(path: String, time: Long) = false

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = this.synchronized(if (mode == Mode.Read && exists(path) && !isDirectory(path)) {
    val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(handles.contains).next()
    openInputChannel(path) match {
      case Some(channel) =>
        handles += handle -> new Handle(this, handle, path, channel)
        handle
      case _ => throw new FileNotFoundException()
    }
  } else throw new FileNotFoundException())

  override def getHandle(handle: Int): api.fs.Handle = this.synchronized(handles.get(handle).orNull)

  override def close() = this.synchronized {
    for (handle <- handles.values)
      handle.close()
    handles.clear()
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    val handlesNbt = nbt.getTagList("input")
    (0 until handlesNbt.tagCount).map(handlesNbt.tagAt).map(_.asInstanceOf[NBTTagCompound]).foreach(handleNbt => {
      val handle = handleNbt.getInteger("handle")
      val path = handleNbt.getString("path")
      val position = handleNbt.getLong("position")
      openInputChannel(path) match {
        case Some(channel) =>
          val fileHandle = new Handle(this, handle, path, channel)
          channel.position(position)
          handles += handle -> fileHandle
        case _ => // The source file seems to have disappeared since last time.
      }
    })
  }

  override def save(nbt: NBTTagCompound) = this.synchronized {
    val handlesNbt = new NBTTagList()
    for (file <- handles.values) {
      assert(file.channel.isOpen)
      val handleNbt = new NBTTagCompound()
      handleNbt.setInteger("handle", file.handle)
      handleNbt.setString("path", file.path)
      handleNbt.setLong("position", file.position)
      handlesNbt.appendTag(handleNbt)
    }
    nbt.setTag("input", handlesNbt)
  }

  // ----------------------------------------------------------------------- //

  protected def openInputChannel(path: String): Option[SeekableByteChannel]

  protected class InputFileChannel(val inputStream: java.io.InputStream) extends SeekableByteChannel {
    var isOpen = true

    private var position_ = 0L

    override def close() = inputStream.close()

    override def truncate(size: Long) = throw new java.io.IOException()

    override def size() = inputStream.available()

    override def position(newPosition: Long) = {
      inputStream.reset()
      position_ = inputStream.skip(newPosition)
      this
    }

    override def position = position_

    override def write(src: ByteBuffer) = throw new java.io.IOException()

    override def read(dst: ByteBuffer): Int = {
      if (dst.hasArray) {
        inputStream.read(dst.array())
      }
      else {
        val count = dst.limit - dst.position
        for (i <- 0 until count) {
          inputStream.read match {
            case -1 => return i
            case b => dst.put(b.toByte)
          }
        }
        count
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private class Handle(val owner: InputStreamFileSystem, val handle: Int, val path: String, val channel: SeekableByteChannel) extends api.fs.Handle {
    override def position = channel.position

    override def length = owner.size(path)

    override def close() = if (channel.isOpen) {
      owner.handles -= handle
      channel.close()
    }

    override def read(into: Array[Byte]) = {
      channel.read(ByteBuffer.wrap(into))
    }

    override def seek(to: Long) = {
      channel.position(to)
      channel.position
    }

    override def write(value: Array[Byte]) = throw new IOException("bad file descriptor")
  }

}
