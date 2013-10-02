package li.cil.oc.api.fs

/**
 * Represents a handle to a file opened from a `FileSystem`.
 */
trait Handle {
  /** The current position in the file. */
  def position: Long

  /** The total length of the file. */
  def length: Long

  /**
   * Closes the handle.
   * <p/>
   * For example, if there is an underlying stream, this should close that
   * stream. Any future calls to `read` or `write` should throw an
   * `IOException` after this function was called.
   */
  def close()

  /**
   * Tries to read as much data from the file as fits into the specified
   * array.
   *
   * @param into the buffer to read the data into.
   * @return the number of bytes read; -1 if there are no more bytes (EOF).
   * @throws IOException if the file was opened in writing mode or an I/O
   *                     error occurred or the file was already closed.
   */
  def read(into: Array[Byte]): Int

  /**
   * Jump to the specified position in the file, if possible.
   *
   * @param to the position in the file to jump to.
   * @return the resulting position in the file.
   */
  def seek(to: Long): Long

  /**
   * Tries to write all the data from the specified array into the file.
   *
   * @param value the data to write into the file.
   * @throws IOException if the file was opened in read-only mode, or another
   *                     I/O error occurred (no more space, for example), or
   *                     the file was already closed.
   */
  def write(value: Array[Byte])
}
