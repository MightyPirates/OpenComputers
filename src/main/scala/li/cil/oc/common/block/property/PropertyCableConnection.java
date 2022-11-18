package li.cil.oc.common.block.property;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.state.EnumProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;

public final class PropertyCableConnection {
    public static enum Shape implements IStringSerializable {
        NONE("none"),
        CABLE("cable"),
        DEVICE("device");

        private final String name;

        private Shape(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return name;
        }

        public String toString() {
            return name;
        }
    }

    public static final EnumProperty<Shape> DOWN = EnumProperty.create("conn_down", Shape.class);
    public static final EnumProperty<Shape> UP = EnumProperty.create("conn_up", Shape.class);
    public static final EnumProperty<Shape> NORTH = EnumProperty.create("conn_north", Shape.class);
    public static final EnumProperty<Shape> SOUTH = EnumProperty.create("conn_south", Shape.class);
    public static final EnumProperty<Shape> WEST = EnumProperty.create("conn_west", Shape.class);
    public static final EnumProperty<Shape> EAST = EnumProperty.create("conn_east", Shape.class);
    public static Map<Direction, EnumProperty<Shape>> BY_DIRECTION;

    static
    {
        EnumMap<Direction, EnumProperty<Shape>> byDir = new EnumMap<>(Direction.class);
        byDir.put(Direction.DOWN, DOWN);
        byDir.put(Direction.UP, UP);
        byDir.put(Direction.NORTH, NORTH);
        byDir.put(Direction.SOUTH, SOUTH);
        byDir.put(Direction.WEST, WEST);
        byDir.put(Direction.EAST, EAST);
        BY_DIRECTION = Collections.unmodifiableMap(byDir);
    }

    private PropertyCableConnection() {
    }
}
