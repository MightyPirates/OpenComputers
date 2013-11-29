package li.cil.oc.api.fs;

import java.io.IOException;

/**
 * Represents a handle to a file opened from a {@link FileSystem}.
 */
public interface Handle {
    /**
     * The current position in the file.
     */
    long position();

    /**
     * The total length of the file.
     */
    long length();

    /**
     * Closes the handle.
     * <p/>
     * For example, if there is an underlying stream, this should close that
     * stream. Any future calls to {@link #read} or {@link #write} should throw
     * an <tt>IOException</tt> after this function was called.
     */
    void close();

    /**
     * Tries to read as much data from the file as fits into the specified
     * array.
     * <p/>
     * For files opened in write or append mode this should always throw an
     * exception.
     *
     * @param into the buffer to read the data into.
     * @return the number of bytes read; -1 if there are no more bytes (EOF).
     * @throws IOException if the file was opened in writing mode or an
     *                     I/O error occurred or the file was already
     *                     closed.
     */
    int read(byte[] into) throws IOException;

    /**
     * Jump to the specified position in the file, if possible.
     * <p/>
     * For files opened in write or append mode this should always throw an
     * exception.
     *
     * @param to the position in the file to jump to.
     * @return the resulting position in the file.
     * @throws IOException if the file was opened in write mode.
     */
    long seek(long to) throws IOException;

    /**
     * Tries to write all the data from the specified array into the file.
     * <p/>
     * For files opened in read mode this should always throw an exception.
     *
     * @param value the data to write into the file.
     * @throws IOException if the file was opened in read-only mode, or
     *                     another I/O error occurred (no more space,
     *                     for example), or the file was already closed.
     */
    void write(byte[] value) throws IOException;
}
