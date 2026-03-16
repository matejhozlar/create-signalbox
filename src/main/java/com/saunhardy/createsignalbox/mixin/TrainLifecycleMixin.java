package com.saunhardy.createsignalbox.mixin;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.saunhardy.createsignalbox.events.TrainLifecycleHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = GlobalRailwayManager.class, remap = false)
public abstract class TrainLifecycleMixin {

    @Shadow public Map<UUID, Train> trains;

    @Inject(method = "addTrain", at = @At("HEAD"), remap = false)
    private void createsignalbox$onTrainAdded(Train train, CallbackInfo ci) {
        try {
            String trainName = train.name != null ? train.name.getString() : "Unknown";
            int carriageCount = train.carriages != null ? train.carriages.size() : 0;

            String ownerName = resolveOwnerName(train.owner);

            TrainLifecycleHandler.reportCreated(train.id, trainName, carriageCount,
                    train.owner, ownerName);
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "removeTrain", at = @At("HEAD"), remap = false)
    private void createsignalbox$onTrainRemoved(UUID id, CallbackInfo ci) {
        try {
            Train train = this.trains.get(id);
            if (train == null) return;

            String trainName = train.name != null ? train.name.getString() : "Unknown";

            String ownerName = resolveOwnerName(train.owner);

            TrainLifecycleHandler.reportRemoved(train.id, trainName, train.owner, ownerName);
        } catch (Exception ignored) {
        }
    }

    private static String resolveOwnerName(UUID owner) {
        if (owner == null) return null;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;

        GameProfileCache cache = server.getProfileCache();
        if (cache == null) return null;

        try {
            Optional<GameProfile> profile = cache.get(owner);
            if (profile.isPresent()) {
                return profile.get().getName();
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
