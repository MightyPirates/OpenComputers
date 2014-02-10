The API can be used to either interact with existing implementations in OpenComputers or to implement your own extensions for OpenComputers.

Extending OpenComputers
========================

Making a tile entity available as a component / peripheral
--------------------------------------------------
If you simply wish to expose a couple of methods that can be called from a computer if your tile entity's block is 'connected' to the computer, you can use the `SimpleComponent` interface. This interface serves as a marker for OpenComputers to know it has to inject code that converts your tile entity into a component using its class transformer. It is an interface instead of an annotation to allow stripping it, removing any dependencies on OpenComputers. Here is an example implementation:
```java
@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")
public class TileEntityMyFancyThing extends TileEntity
       implements SimpleComponent
{
    @Override
    public String getComponentName() {
        return "fancy_thing";
    }

    @Callback
    @Optional.Method(modid = "OpenComputers")
    public Object[] greet(Context context, Arguments args) {
        return new Object[]{String.format("Hello, %s!", args.checkString(0))};
    }
}
```

The `getComponentName` determines with which name the tile entity will be available to computers. The `Callback` annotation tells OpenComputers to make the annotated method available to the computer. See the documentation on the `Callback` annotation for more information, in particular how it can be used to manipulate the call behavior (synchronized to the main thread vs. in the thread driving the calling computer).

So to call the greeter method, in Lua you'd do this:
```lua
print(component.fancy_thing.greet("Steve")) -- prints "Hello, Steve!"
````

More control
------------
If you really need more control over how how your tile entity interacts with OpenComputer's internal network, you will have to implement the `Environment` interface on your tile entity. There's a basic implementation in the prefab package, named `TileEntityEnvironment`. Doing so will give you access to the `Node` that connects to the component network, and you must take care of the construction of the node itself (using the factory method in `api.Network`). This allows you to make the node a `Connector` node, which will allow you to draw internal power from OpenComputers or feed energy into it. You will also be able to send messages over the component network, see the `send...` methods in the `Node` interface. See the documentation on those interfaces to get a better idea on how they work together.

Making a thrid-party block available as component / peripheral
--------------------------------------------------------------
Blocks from other mods, i.e. blocks where you have no control over the tile entity implementation, can be accessed using the Adapter block as long as there is a driver available that supports the block. If there are multiple drivers they are automatically merged. Please see the [OpenComponents][] project for examples, and consider contributing any block drivers you write to it. Thank you.

Making items available as components
------------------------------------
To make items usable as components in computers, such as cards or hard drives, you have to provide a driver for that item. This means you have to implement the `driver.Item` interface on a class and register an instance of it via the `api.Driver` registry. You can base your item drivers on the `DriverItem` prefab. Please see the example project on Github for a working example, and read the documentation of the driver interface for more information.

FileSystem API
==============
If you'd like to make some files/scripts you ship with your mod available to a computer, you can do so by wrapping those files using an OpenComputers file system. Use the factory methods in `api.FileSystem` to wrap the location your files are stored at in a file system, use the `asManagedEnvironment` methods to wrap it in a node that can be attached to the component network. For example, in an environment of a tile entity or created by an item driver you could do this in the `onConnect` method whenever a computer is connected (i.e. `node.host() instanceof Context`). Code-wise it may look something like this:
```java
public class TileEntityWithFileSystem extends TileEntityEnvironment {
    private final Node fileSystem;

    public TileEntityWithFileSystem() {
        node = Network.newNode(this, Visibility.Network).create();
        fileSystem = FileSystem.asManagedEnvironment(FileSystem.fromClass(getClass, "yourmodid/lua"), "my_files");
    }

    @Override
    public void onConnect(final Node node) {
        if (node.host() instanceof Context) {
            // Attach our file system to new computers we get connected to.
            // Note that this is also called for all already present computers
            // when we're added to an already existing network, so we don't
            // have to loop through the existing nodes manually.
            node.connect(fileSystem);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node.host() instanceof Context) {
            // Remove our file systems when we get disconnected from a
            // computer.
            node.disconnect(fileSystem);
        } else if (node == this.node) {
            // Remove the file system if we are disconnected, because in that
            // case this method is only called once.
            fileSystem.node.remove();
        }
    }
}
```


[OpenComponents]: https://github.com/MightyPirates/OpenComponents