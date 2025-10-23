package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.common.PacketSignal;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class BossEventPacket implements BedrockPacket {
    public long bossUniqueEntityId;
    public Action action;
    public long playerUniqueEntityId;
    public CharSequence title;
    public CharSequence filteredTitle = "";
    public float healthPercentage;
    public int darkenSky;
    public int color;
    public int overlay;

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.BOSS_EVENT;
    }

    public enum Action {
        /**
         * Creates the bossbar to the player.
         */
        CREATE,
        /**
         * Registers a player to a boss fight.
         */
        REGISTER_PLAYER,
        /**
         * Removes the bossbar from the client.
         */
        REMOVE,
        /**
         * Unregisters a player from a boss fight.
         */
        UNREGISTER_PLAYER,
        /**
         * Appears not to be implemented. Currently bar percentage only appears to change in response to the target entity's health.
         */
        UPDATE_PERCENTAGE,
        /**
         * Also appears to not be implemented. Title clientside sticks as the target entity's nametag, or their entity transactionType name if not set.
         */
        UPDATE_NAME,
        /**
         * Darken the sky when the boss bar is shown.
         */
        UPDATE_PROPERTIES,
        /**
         * Not implemented :( Intended to alter bar appearance, but these currently produce no effect on clientside whatsoever.
         */
        UPDATE_STYLE,
        QUERY
    }

    @Override
    public BossEventPacket clone() {
        try {
            return (BossEventPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getTitle() {
        return getTitle(String.class);
    }

    public <T extends CharSequence> T getTitle(Class<T> type) {
        return type.cast(title);
    }

    public String getFilteredTitle() {
        return getFilteredTitle(String.class);
    }

    public <T extends CharSequence> T getFilteredTitle(Class<T> type) {
        return type.cast(filteredTitle);
    }
}

