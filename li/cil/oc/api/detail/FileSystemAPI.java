package li.cil.oc.api.detail;

import dan200.computer.api.IMount;
import dan200.computer.api.IWritableMount;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.network.ManagedEnvironment;

public interface FileSystemAPI {
    FileSystem fromClass(Class<?> clazz, String domain, String root);

    FileSystem fromSaveDirectory(String root, long capacity, boolean buffered);

    FileSystem fromMemory(long capacity);

    FileSystem fromComputerCraft(IMount mount);

    FileSystem fromComputerCraft(IWritableMount mount);

    ManagedEnvironment asManagedEnvironment(FileSystem fs, String label);
}