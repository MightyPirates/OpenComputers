package li.cil.oc.api.detail;

import cpw.mods.fml.common.Optional;
import dan200.computer.api.IMount;
import dan200.computer.api.IWritableMount;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.ManagedEnvironment;

public interface FileSystemAPI {
    FileSystem fromClass(Class<?> clazz, String domain, String root);

    FileSystem fromSaveDirectory(String root, long capacity, boolean buffered);

    FileSystem fromMemory(long capacity);

    @Optional.Method(modid = "ComputerCraft")
    FileSystem fromComputerCraft(IMount mount);

    @Optional.Method(modid = "ComputerCraft")
    FileSystem fromComputerCraft(IWritableMount mount);

    ManagedEnvironment asManagedEnvironment(FileSystem fs, Label label);

    ManagedEnvironment asManagedEnvironment(FileSystem fs, String label);

    ManagedEnvironment asManagedEnvironment(FileSystem fs);
}