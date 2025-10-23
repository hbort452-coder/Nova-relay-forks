package org.cloudburstmc.protocol.bedrock.codec.v776.serializer;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v486.serializer.BossEventSerializer_v486;
import org.cloudburstmc.protocol.bedrock.packet.BossEventPacket;
import org.cloudburstmc.protocol.common.util.TextConverter;
import org.cloudburstmc.protocol.common.util.VarInts;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BossEventSerializer_v776 extends BossEventSerializer_v486 {
    public static final BossEventSerializer_v776 INSTANCE = new BossEventSerializer_v776();

    @Override
    protected void serializeAction(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        TextConverter converter = helper.getTextConverter();
        if (packet.getAction() == BossEventPacket.Action.CREATE) {
            helper.writeString(buffer, converter.serialize(packet.getTitle(CharSequence.class)));
            helper.writeString(buffer, converter.serialize(packet.getFilteredTitle(CharSequence.class)));
            buffer.writeFloatLE(packet.getHealthPercentage());
            // fall through to UPDATE_PROPERTIES
            buffer.writeShortLE(packet.getDarkenSky());
            // fall through to UPDATE_STYLE
            VarInts.writeUnsignedInt(buffer, packet.getColor());
            VarInts.writeUnsignedInt(buffer, packet.getOverlay());
        } else if (packet.getAction() == BossEventPacket.Action.UPDATE_NAME) {
            helper.writeString(buffer, converter.serialize(packet.getTitle(CharSequence.class)));
            helper.writeString(buffer, converter.serialize(packet.getFilteredTitle(CharSequence.class)));
        } else {
            super.serializeAction(buffer, helper, packet);
        }
    }

    @Override
    protected void deserializeAction(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        TextConverter converter = helper.getTextConverter();
        if (packet.getAction() == BossEventPacket.Action.CREATE) {
            packet.setTitle(converter.deserialize(helper.readString(buffer)));
            packet.setFilteredTitle(converter.deserialize(helper.readString(buffer)));
            packet.setHealthPercentage(buffer.readFloatLE());
            // fall through to UPDATE_PROPERTIES
            packet.setDarkenSky(buffer.readUnsignedShortLE());
            // fall through to UPDATE_STYLE
            packet.setColor(VarInts.readUnsignedInt(buffer));
            packet.setOverlay(VarInts.readUnsignedInt(buffer));
        } else if (packet.getAction() == BossEventPacket.Action.UPDATE_NAME) {
            packet.setTitle(converter.deserialize(helper.readString(buffer)));
            packet.setFilteredTitle(converter.deserialize(helper.readString(buffer)));
        } else {
            super.deserializeAction(buffer, helper, packet);
        }
    }
}
