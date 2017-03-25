package li.cil.oc.common;

public enum GuiType {
    Adapter(Category.BLOCK),
    Assembler(Category.BLOCK),
    Case(Category.BLOCK),
    Charger(Category.BLOCK),
    Database(Category.ITEM),
    Drive(Category.ITEM),
    Drone(Category.ENTITY),
    Manual(Category.ITEM),
    Printer(Category.BLOCK),
    Rack(Category.BLOCK),
    Raid(Category.BLOCK),
    Relay(Category.BLOCK),
    Robot(Category.BLOCK),
    Screen(Category.BLOCK),
    Server(Category.ITEM),
    ServerInRack(Category.BLOCK),
    Tablet(Category.ITEM),
    TabletInner(Category.ITEM),
    Terminal(Category.ITEM),
    Waypoint(Category.BLOCK);

    public enum Category {
        BLOCK,
        ENTITY,
        ITEM
    }

    public final Category category;

    GuiType(final Category category) {
        this.category = category;
    }

    public static final GuiType[] VALUES = values();

    public static int embedSlot(final int y, final int slot) {
        return (y & 0x00FFFFFF) | (slot << 24);
    }

    public static int extractY(final int value) {
        return value & 0x00FFFFFF;
    }

    public static int extractSlot(final int value) {
        return (value >> 24) & 0xFF;
    }
}
