package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.common.PacketSignal;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class DisconnectPacket implements BedrockPacket {
    public DisconnectFailReason reason = DisconnectFailReason.UNKNOWN;
    public boolean messageSkipped;
    public CharSequence kickMessage;
    /**
     * @since v712
     */
    public CharSequence filteredMessage = "";

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.DISCONNECT;
    }

    @Override
    public DisconnectPacket clone() {
        try {
            return (DisconnectPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getKickMessage() {
        return getKickMessage(String.class);
    }

    public <T extends CharSequence> T getKickMessage(Class<T> type) {
        return type.cast(kickMessage);
    }

    public String getFilteredMessage() {
        return getFilteredMessage(String.class);
    }

    public <T extends CharSequence> T getFilteredMessage(Class<T> type) {
        return type.cast(filteredMessage);
    }
}

