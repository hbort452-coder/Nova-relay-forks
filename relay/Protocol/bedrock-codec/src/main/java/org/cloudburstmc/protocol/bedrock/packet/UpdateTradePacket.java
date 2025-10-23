package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.common.PacketSignal;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class UpdateTradePacket implements BedrockPacket {
    public int containerId;
    public ContainerType containerType;
    public int size; // Hardcoded to 0
    public int tradeTier;
    public long traderUniqueEntityId;
    public long playerUniqueEntityId;
    public CharSequence displayName;
    public NbtMap offers;
    public boolean newTradingUi;
    public boolean recipeAddedOnUpdate;
    public boolean usingEconomyTrade;

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.UPDATE_TRADE;
    }

    @Override
    public UpdateTradePacket clone() {
        try {
            return (UpdateTradePacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getDisplayName() {
        return getDisplayName(String.class);
    }

    public <T extends CharSequence> T getDisplayName(Class<T> type) {
        return type.cast(displayName);
    }
}

