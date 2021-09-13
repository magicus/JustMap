package ru.bulldog.justmap.map.data;

import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.util.StateUtil;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class MapProcessor {

	public static int getTopBlockY(WorldChunk worldChunk, Layer layer, int level, int x, int z, boolean liquids) {
		int yws = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z);
		int ymb = worldChunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x, z);
		int y = Math.max(yws, ymb);

		if (y < 0) return -1;

		return getTopBlockY(worldChunk, layer, level, x, y, z, liquids);
	}

	private static int getTopBlockY(WorldChunk worldChunk, Layer layer, int level, int x, int startY, int z, boolean liquids) {
		if (worldChunk == null || worldChunk.isEmpty()) return -1;

		ChunkPos chunkPos = worldChunk.getPos();
		int posX = x + (chunkPos.x << 4);
		int posZ = z + (chunkPos.z << 4);

		boolean plants = !ClientSettings.hidePlants;
		BlockPos.Mutable mutPos = new BlockPos.Mutable(posX, startY, posZ);
		if ((layer.equals(Layer.NETHER) || layer.equals(Layer.CAVES))) {
			int floor = level * layer.height;
			for (int i = floor + (layer.height - 1); i >= floor; i--) {
				mutPos.setY(i);
				int worldPosY = loopPos(worldChunk, mutPos, 0, liquids, plants);
				mutPos.setY(worldPosY + 1);
				if (StateUtil.checkState(worldChunk.getBlockState(mutPos), liquids, plants)) {
					return worldPosY;
				}
			}
		} else {
			int worldPosY = loopPos(worldChunk, mutPos, 0, liquids, plants);
			mutPos.setY(worldPosY + 1);
			BlockState overState = worldChunk.getBlockState(mutPos);
			if (StateUtil.checkState(overState, liquids, plants)) {
				return worldPosY;
			}
		}

		return -1;
	}

	private static int loopPos(WorldChunk worldChunk, BlockPos.Mutable pos, int stop, boolean liquids, boolean plants) {
		boolean loop = false;
		do {
			loop = StateUtil.checkState(worldChunk.getBlockState(pos), liquids, plants);
			loop &= pos.getY() > stop;
			if (loop) {
				pos.move(0, -1, 0);
			}
		} while (loop);

		return pos.getY();
	}

	private static int checkLiquids(WorldChunk worldChunk, Layer layer, int level, int x, int y, int z) {
		if (y == -1 || worldChunk == null || worldChunk.isEmpty()) return 0;

		BlockPos pos = new BlockPos(x + (worldChunk.getPos().x << 4), y, z + (worldChunk.getPos().z << 4));
		BlockState state = worldChunk.getBlockState(pos);
		if (StateUtil.isLiquid(state, false)) {
			y = getTopBlockY(worldChunk, layer, level, x, y, z, false);
		}

		return y;
	}

	public static int heightDifference(ChunkData mapChunk, Layer layer, int level, BlockPos.Mutable mutPos, boolean skipWater) {
		int x = mutPos.getX();
		int z = mutPos.getZ();
		int y = mutPos.getY();
		int ex = x + 1;
		int sz = z - 1;

		int east, south;
		ChunkPos pos = mapChunk.getPos();
		World world = mapChunk.getWorldChunk().getWorld();
		ChunkLevel chunkLevel = mapChunk.getChunkLevel(layer, level);
		if (ex > 15) {
			ex -= 16;
			WorldChunk eastChunk = world.getChunk(pos.x + 1, pos.z);
			east = getTopBlockY(eastChunk, layer, level, ex, z, skipWater);
			east = checkLiquids(eastChunk, layer, level, ex, east, z);
		} else {
			east = chunkLevel.sampleHeightmap(ex, z);
			east = checkLiquids(mapChunk.getWorldChunk(), layer, level, ex, east, z);
		}
		if (sz < 0) {
			sz += 16;
			WorldChunk southChunk = world.getChunk(pos.x, pos.z - 1);
			south = getTopBlockY(southChunk, layer, level, x, sz, skipWater);
			south = checkLiquids(southChunk, layer, level, x, south, sz);
		} else {
			south = chunkLevel.sampleHeightmap(x, sz);
			south = checkLiquids(mapChunk.getWorldChunk(), layer, level, x, south, sz);
		}

		y = checkLiquids(mapChunk.getWorldChunk(), layer, level, x, y, z);

		east = east > 0 ? east - y : 0;
		south = south > 0 ? south - y : 0;

		int diff = east - south;
		if (diff == 0) return 0;

		int maxDiff = ClientSettings.terrainStrength;
		diff = diff < 0 ? Math.max(-maxDiff, diff) : Math.min(maxDiff, diff);

		return diff;
	}
}
