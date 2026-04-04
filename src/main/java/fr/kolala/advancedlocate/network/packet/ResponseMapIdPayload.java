package fr.kolala.advancedlocate.network.packet;

import fr.kolala.advancedlocate.AdvancedLocate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ResponseMapIdPayload(net.minecraft.component.type.MapIdComponent mapId) implements CustomPayload {
    public static final Id<ResponseMapIdPayload> ID = new Id<>(Identifier.of(AdvancedLocate.MOD_ID, "response_map_id"));
    public static final PacketCodec<PacketByteBuf, RequestMapIdPayload> CODEC = PacketCodec.unit(new RequestMapIdPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}