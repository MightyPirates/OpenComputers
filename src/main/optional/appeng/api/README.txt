If your considering using the AE API, please take a moment to read though the following.
 
Before we begin,
        AE's API will change as new features are added to the mod, I try to keep
        the changes to a minimum and try to prevent breaking changes, however it will
        probably happen at some point, and it can't truly be avoided in some cases.
 
The Grid
        AE refers to the "ME Network" internally as "The Grid" you will see methods such
         as getGrid(), setGrid() commonly, most interaction with networks, and creating new
         network blocks will involve the grid.
 
Network Enumeration
        If your trying to add a new ME Network Block, you must at minimum implement
        IGridTileEntity, and post the GridTileLoadEvent, and GridTileUnloadEvent when your
        tile is added / removed respectfully, if you fail to do this correctly, your tile,
        may ghost in the network, or cause invalid behaviour. You must inform AE that your
        tiles is joining and exiting the Network, with these events or you will not get notified
        of any network related features.
 
Thread Safety
        AE is not thread safe, do not attempt to use the API from other threads, this will
        result in crashes, and CME exceptions - and will not end well.
 
Blocks / Items / Materials
        AE Exposes its Original Item stacks via the Blocks, Items, Materials classes for easy access,
        don't change these, if you need to use an item make sure you .copy() it and use the copy, if
        failing to do so can result in dupe bugs irregular crafting patterns, and other issues.
 
AE and ItemStacks / Item Lists
        AE's API commonly refers to IAEItemStacks, and IItemLists this are internally implemented
        classes in AE, Do not Implement IAEItemstack, or IItemList, use the util methods to
        construct new instances them.
 
API Conflicts / Inclusion
        Refrain from including portions of the API as class files in your mod, unless required
        because your implementing an interface, even then its greatly appreciated if you put in
        the effort to get around this.
       
        If you are not sure how to do this and plan on releasing your mod, I will be happy to explain
        what steps you can take to improve compatibility, and prevent conflicts no one likes crashes,
        and these cause crashes.
 
I've been trying to improve the documentation included in the API java files however things can be
confusing or misleading, if you have questions after trying to figure something out I'm usually
available on IRC.