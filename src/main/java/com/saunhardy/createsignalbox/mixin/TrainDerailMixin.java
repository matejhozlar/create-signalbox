package com.saunhardy.createsignalbox.mixin;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.saunhardy.createsignalbox.events.TrainDerailHandler;
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

    @Unique
    private boolean createsignalbox$wasDerailed;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void createsignalbox$onTickStart(Level level, CallbackInfo ci) {
        createsignalbox$wasDerailed = this.derailed;
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void createsignalbox$onTickEnd(Level level, CallbackInfo ci) {
        if (createsignalbox$wasDerailed || !this.derailed) return;
        if (TrainDerailHandler.consumeCrashed(this.id)) return;

        String trainName = this.name != null ? this.name.getString() : "Unknown";
        int carriageCount = this.carriages != null ? this.carriages.size() : 0;

        double[] pos = null;
        String dimension = null;
        String ownerName = null;

        if (this.carriages != null && !this.carriages.isEmpty()) {
            try {
                final double[][] posHolder = {null};
                final String[] dimHolder = {null};
                final MinecraftServer[] serverHolder = {null};

                this.carriages.get(0).forEachPresentEntity(entity -> {
                    Vec3 entityPos = entity.position();
                    posHolder[0] = new double[]{entityPos.x, entityPos.y, entityPos.z};
                    dimHolder[0] = entity.level().dimension().location().toString();
                    serverHolder[0] = entity.level().getServer();
                });

                pos = posHolder[0];
                dimension = dimHolder[0];

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
            } catch (Exception ignored) {
            }
        }

        TrainDerailHandler.reportDerailment(this.id, trainName, this.speed, carriageCount,
                pos, dimension, this.owner, ownerName);
    }
}
