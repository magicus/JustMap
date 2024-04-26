package ru.bulldog.justmap.mixins.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen.WorldEntryReason;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.util.CurrentWorldPos;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "joinWorld", at = @At("TAIL"))
	public void onJoinWorld(ClientWorld world, WorldEntryReason worldEntryReason, CallbackInfo ci) {
		MapDataProvider.getMultiworldManager().onWorldChanged(world);
		CurrentWorldPos.updateWorld(world);
	}
}
