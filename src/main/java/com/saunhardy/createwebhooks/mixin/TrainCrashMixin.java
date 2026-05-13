package com.saunhardy.createwebhooks.mixin;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.saunhardy.createwebhooks.events.TrainCrashHandler;
import com.saunhardy.createwebhooks.events.TrainDerailHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;

@Mixin(value = Train.class, remap = false)
public abstract class TrainCrashMixin {

    @Shadow public UUID id;
    @Shadow public Component name;
    @Shadow public double speed;
    @Shadow public boolean derailed;
    @Shadow public List<Carriage> carriages;
    @Shadow public @Nullable UUID owner;
    @Shadow public @Nullable Player backwardsDriver;
    @Shadow public TrackGraph graph;

    @Inject(method = "crash", at = @At("HEAD"), remap = false)
    private void createwebhooks$onTrainCrash(CallbackInfo ci) {
        if (this.derailed) return;

        TrainDerailHandler.markCrashed(this.id);

        String trainName = this.name != null ? this.name.getString() : "Unknown";
        int carriageCount = this.carriages != null ? this.carriages.size() : 0;

        double[] pos = null;
        String dimension = null;
        UUID driverUuid = null;
        List<TrainCrashHandler.PlayerInfo> passengers = new ArrayList<>();

        if (this.carriages != null) {
            final double[][] posHolder = {null};
            final String[] dimHolder = {null};

            for (Carriage carriage : this.carriages) {
                try {
                    carriage.forEachPresentEntity(entity -> {
                        if (posHolder[0] == null) {
                            Vec3 entityPos = entity.position();
                            posHolder[0] = new double[]{entityPos.x, entityPos.y, entityPos.z};
                            dimHolder[0] = entity.level().dimension().location().toString();
                        }

                        Optional<UUID> controlling = entity.getControllingPlayer();
                        if (controlling.isPresent()) {
                            passengers.add(new TrainCrashHandler.PlayerInfo(
                                    controlling.get(), null, true));
                        }

                        for (Entity passenger : entity.getIndirectPassengers()) {
                            if (passenger instanceof Player p) {
                                boolean isDriver = controlling.isPresent()
                                        && controlling.get().equals(p.getUUID());
                                passengers.add(new TrainCrashHandler.PlayerInfo(
                                        p.getUUID(), p.getName().getString(), isDriver));
                            }
                        }
                    });
                } catch (Exception ignored) {
                }
            }

            pos = posHolder[0];
            dimension = dimHolder[0];

            // Fallback: get position from track graph if no entity was loaded
            if (pos == null && this.graph != null) {
                try {
                    TravellingPoint point = this.carriages.get(0).getLeadingPoint();
                    if (point.node1 != null && point.edge != null) {
                        Vec3 graphPos = point.getPosition(this.graph);
                        pos = new double[]{graphPos.x, graphPos.y, graphPos.z};
                        dimension = point.node1.getLocation().dimension.location().toString();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        Map<UUID, TrainCrashHandler.PlayerInfo> deduped = new LinkedHashMap<>();
        for (TrainCrashHandler.PlayerInfo p : passengers) {
            deduped.merge(p.uuid(), p, (existing, incoming) -> new TrainCrashHandler.PlayerInfo(
                    existing.uuid(),
                    existing.name() != null ? existing.name() : incoming.name(),
                    existing.isDriver() || incoming.isDriver()));
        }

        for (TrainCrashHandler.PlayerInfo p : deduped.values()) {
            if (p.isDriver()) {
                driverUuid = p.uuid();
                break;
            }
        }

        String backwardsDriverName = null;
        UUID backwardsDriverUuid = null;
        if (this.backwardsDriver != null) {
            backwardsDriverUuid = this.backwardsDriver.getUUID();
            backwardsDriverName = this.backwardsDriver.getName().getString();
        }

        // Resolve any remaining null names via profile cache
        String ownerName = null;
        MinecraftServer server = null;
        if (this.carriages != null) {
            final MinecraftServer[] serverHolder = {null};
            for (Carriage carriage : this.carriages) {
                if (serverHolder[0] != null) break;
                try {
                    carriage.forEachPresentEntity(entity ->
                            serverHolder[0] = entity.level().getServer());
                } catch (Exception ignored) {
                }
            }
            server = serverHolder[0];
        }

        if (server != null) {
            GameProfileCache cache = server.getProfileCache();
            if (cache != null) {
                // Resolve owner
                if (this.owner != null) {
                    try {
                        Optional<GameProfile> profile = cache.get(this.owner);
                        if (profile.isPresent()) {
                            ownerName = profile.get().getName();
                        }
                    } catch (Exception ignored) {
                    }
                }

                // Resolve passengers/driver with null names
                Map<UUID, TrainCrashHandler.PlayerInfo> resolved = new LinkedHashMap<>();
                for (var entry : deduped.entrySet()) {
                    TrainCrashHandler.PlayerInfo info = entry.getValue();
                    if (info.name() == null) {
                        try {
                            Optional<GameProfile> profile = cache.get(info.uuid());
                            if (profile.isPresent()) {
                                info = new TrainCrashHandler.PlayerInfo(
                                        info.uuid(), profile.get().getName(), info.isDriver());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    resolved.put(entry.getKey(), info);
                }
                deduped = resolved;
            }
        }

        TrainCrashHandler.reportCrash(this.id, trainName, this.speed, carriageCount,
                pos, dimension, this.owner, ownerName, driverUuid,
                new ArrayList<>(deduped.values()),
                backwardsDriverUuid, backwardsDriverName);
    }
}
