package ru.bulldog.justmap.map.data.classic;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.client.screen.WorldmapScreen;
import ru.bulldog.justmap.map.IMap;
import ru.bulldog.justmap.map.data.MapDataManager;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.map.data.WorldMapper;
import ru.bulldog.justmap.map.data.classic.event.ChunkUpdateEvent;
import ru.bulldog.justmap.map.data.classic.event.ChunkUpdateListener;
import ru.bulldog.justmap.util.tasks.MemoryUtil;

public final class WorldManager implements MapDataManager {
	public static final WorldManager WORLD_MANAGER = new WorldManager();

	private boolean cacheClearing = false;

	public static IMap getCurrentlyShownMap() {
		MinecraftClient minecraft = MinecraftClient.getInstance();
		return minecraft.currentScreen instanceof WorldmapScreen ? (WorldmapScreen) minecraft.currentScreen : JustMapClient.getMiniMap();
	}

	public WorldData getWorldData() {
		return (WorldData) getWorldMapper();
	}

	private WorldData createWorldData() {
		return new WorldData(MapDataProvider.getMultiworldManager().getCurrentWorld());
	}

	@Override
	public WorldMapper getWorldMapper() {
		return MapDataProvider.getMultiworldManager().getOrCreateWorldMapper(this::createWorldData);
	}

	@Override
	public void onChunkLoad(World world, WorldChunk worldChunk) {
		if (world == null || worldChunk == null || worldChunk.isEmpty()) return;
		IMap map = getCurrentlyShownMap();
		WorldData mapData = getWorldData();
		if (mapData == null) return;
		ChunkData mapChunk = mapData.getChunk(worldChunk.getPos());
		ChunkUpdateEvent updateEvent = new ChunkUpdateEvent(worldChunk, mapChunk, map.getLayer(), map.getLevel(), 0, 0, 16, 16, true);
		ChunkUpdateListener.accept(updateEvent);
	}

	private void updateOnTick() {
		getWorldData().updateMap();
		JustMap.WORKER.execute(() -> MapDataProvider.getMultiworldManager().forEachWorldMapper(
			(key, worldMapper) -> {
				WorldData worldData = (WorldData) worldMapper;
				if (worldData != null) {
					worldData.clearCache();
				}
			}));
	}

	private void memoryControl() {
		if (cacheClearing) return;
		long usedPct = MemoryUtil.getMemoryUsage();
		if (usedPct >= 85L) {
			cacheClearing = true;
			JustMap.LOGGER.debug(String.format("Memory usage at %2d%%, forcing garbage collection.", usedPct));
			JustMap.WORKER.execute("Hard cache clearing...", () -> {
				int amount = ClientSettings.purgeAmount * 10;
				MapDataProvider.getMultiworldManager().forEachWorldMapper((key, worldMapper) -> {
					WorldData worldData = (WorldData) worldMapper;
					if (worldData != null) {
						worldData.getChunkManager().purge(amount, 1000);
					}
				});
				System.gc();
				cacheClearing = false;
			});
			usedPct = MemoryUtil.getMemoryUsage();
			JustMap.LOGGER.debug(String.format("Memory usage at %2d%%.", usedPct));
		}
	}

	@Override
	public void onSetBlockState(BlockPos pos, BlockState state, World world) {
		ChunkUpdateListener.onSetBlockState(pos, state, world);
	}

	@Override
	public void onTick(boolean isServer) {
		MapDataProvider.getMultiworldManager().onTick(isServer);
		if (!isServer) {
			if (!JustMapClient.canMapping()) return;
			updateOnTick();
			memoryControl();
		}
		ChunkUpdateListener.proceed();
	}

	@Override
	public void onWorldStop() {
		ChunkUpdateListener.stop();
	}
}
