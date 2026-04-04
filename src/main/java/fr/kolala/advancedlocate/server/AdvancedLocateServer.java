package fr.kolala.advancedlocate.server;

import fr.kolala.advancedlocate.network.packet.RequestMapIdPayload;
import fr.kolala.advancedlocate.network.packet.ResponseMapIdPayload;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;

public class AdvancedLocateServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(RequestMapIdPayload.ID, (payload, context) -> {
            var player = context.player();
            var world = player.getServerWorld();
            MapIdComponent mapId = world.increaseAndGetMapId();
            MapState mapState = MapState.of(player.getBlockX(), player.getBlockZ(), (byte) 0, true, true, world.getRegistryKey());
            world.putMapState(mapId, mapState);

            context.responseSender().sendPacket(new ResponseMapIdPayload(mapId));
        });
    }
}