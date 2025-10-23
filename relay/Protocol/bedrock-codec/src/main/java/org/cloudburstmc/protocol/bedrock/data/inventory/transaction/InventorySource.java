package org.cloudburstmc.protocol.bedrock.data.inventory.transaction;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;

import static org.cloudburstmc.protocol.common.util.Preconditions.checkNotNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventorySource {
    private static final InventorySource CREATIVE_SOURCE = new InventorySource(Type.CREATIVE, ContainerId.NONE, Flag.NONE);
    private static final InventorySource GLOBAL_SOURCE = new InventorySource(Type.GLOBAL, ContainerId.NONE, Flag.NONE);
    private static final InventorySource INVALID_SOURCE = new InventorySource(Type.INVALID, ContainerId.NONE, Flag.NONE);
    private final Type type;
    private final int containerId;
    private final Flag flag;

    public static InventorySource fromContainerWindowId(int containerId) {
        checkNotNull(containerId, "containerId");
        return new InventorySource(Type.CONTAINER, containerId, Flag.NONE);
    }

    public static InventorySource fromCreativeInventory() {
        return CREATIVE_SOURCE;
    }

    public static InventorySource fromGlobalInventory() {
        return GLOBAL_SOURCE;
    }

    public static InventorySource fromInvalid() {
        return INVALID_SOURCE;
    }

    public static InventorySource fromNonImplementedTodo(int containerId) {
        checkNotNull(containerId, "containerId");
        return new InventorySource(Type.NON_IMPLEMENTED_TODO, containerId, Flag.NONE);
    }

    public static InventorySource fromUntrackedInteractionUI(int containerId) {
        checkNotNull(containerId, "containerId");
        return new InventorySource(Type.UNTRACKED_INTERACTION_UI, containerId, Flag.NONE);
    }

    public static InventorySource fromWorldInteraction(@NonNull Flag flag) {
        checkNotNull(flag, "flag");
        return new InventorySource(Type.WORLD_INTERACTION, ContainerId.NONE, flag);
    }

    public enum Type {
        INVALID(-1),
        CONTAINER(0),
        GLOBAL(1),
        WORLD_INTERACTION(2),
        CREATIVE(3),
        UNTRACKED_INTERACTION_UI(100),
        NON_IMPLEMENTED_TODO(99999);

        private static final Int2ObjectMap<Type> BY_ID = new Int2ObjectOpenHashMap<>(6);

        static {
            for (Type type : values()) {
                BY_ID.put(type.id, type);
            }
        }

        private final int id;

        Type(int id) {
            this.id = id;
        }

        public static Type byId(int id) {
            Type type = BY_ID.get(id);
            return type == null ? INVALID : type;
        }

        public int id() {
            return id;
        }
    }

    public enum Flag {
        DROP_ITEM,
        PICKUP_ITEM,
        NONE
    }
}