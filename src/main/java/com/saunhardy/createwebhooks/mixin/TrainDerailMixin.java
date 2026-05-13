package com.saunhardy.createwebhooks.mixin;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.saunhardy.createwebhooks.events.TrainDerailHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = Train.class, remap = false)
public abstract class TrainDerailMixin {

    @Shadow public UUID id;
    @Shadow public Component name;
    @Shadow public double speed;
    @Shadow public boolean derailed;
    @Shadow public List<Carriage> carriages;
    @Shadow public @Nullable UUID owner;
    @Shadow public TrackGraph graph;

    @Unique
    private boolean createwebhooks$wasDerailed;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void createwebhooks$onTickStart(Level level, CallbackInfo ci) {
        createwebhooks$wasDerailed = this.derailed;
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void createwebhooks$onTickEnd(Level level, CallbackInfo ci) {
        if (createwebhooks$wasDerailed || !this.derailed) return;
        if (TrainDerailHandler.consumeCrashed(this.id)) return;

        String trainName = this.name != null ? this.name.getString() : "Unknown";
        int carriageCount = this.carriages != null ? this.carriages.size() : 0;

        double[] pos = null;
        String dimension = null;
        String ownerName = null;

        if (this.carriages != null && !this.carriages.isEmpty()) {
            final double[][] posHolder = {null};
            final String[] dimHolder = {null};
            final MinecraftServer[] serverHolder = {null};

            for (Carriage carriage : this.carriages) {
                try {
                    carriage.forEachPresentEntity(entity -> {
                        if (posHolder[0] == null) {
                            Vec3 entityPos = entity.position();
                            posHolder[0] = new double[]{entityPos.x, entityPos.y, entityPos.z};
                            dimHolder[0] = entity.level().dimension().location().toString();
                        }
                        if (serverHolder[0] == null) {
                            serverHolder[0] = entity.level().getServer();
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

            if (serverHolder[0] != null && this.owner != null) {
                GameProfileCache cache = serverHolder[0].getProfileCache();
                if (cache != null) {
                    try {
                        Optional<GameProfile> profile = cache.get(this.owner);
                        if (profile.isPresent()) {
                            ownerName = profile.get().getName();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        TrainDerailHandler.reportDerailment(this.id, trainName, this.speed, carriageCount,
                pos, dimension, this.owner, ownerName);
    }
}
