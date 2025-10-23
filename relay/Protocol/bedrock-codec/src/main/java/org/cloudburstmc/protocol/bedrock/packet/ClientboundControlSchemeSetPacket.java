package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.ControlScheme;
import org.cloudburstmc.protocol.common.PacketSignal;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class ClientboundControlSchemeSetPacket implements BedrockPacket {

    public ControlScheme scheme;

    @Override
    public PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    @Override
    public BedrockPacketType getPacketType() {
        return BedrockPacketType.CLIENTBOUND_CONTROL_SCHEME_SET;
    }

    @Override
    public BedrockPacket clone() {
        try {
            return (ClientboundControlSchemeSetPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
