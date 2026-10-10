package com.ronaldo.survivalneeds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.serialization.Codec;
import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import com.ronaldo.survivalneeds.entity.PrincessBrubin;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

/**
 * Onde a Princess Brubin é encontrada:
 * <ul>
 *   <li><b>Em toda vila:</b> quando o chunk onde a vila começa carrega e ainda não há uma Brubin ali, uma é colocada
 *       perto do centro. Vale para vilas novas e para as já exploradas.</li>
 *   <li><b>Rara pelo mapa:</b> cada chunk que carrega pela primeira vez (fora de vilas) tem uma pequena chance de ter
 *       uma Brubin solitária na superfície.</li>
 * </ul>
 * O chunk guarda uma marca, para isso acontecer uma vez só. Como o chunk que carrega pode ter vizinhos ainda não
 * carregados, o trabalho entra numa fila e é feito nos ticks seguintes, quando o lugar está carregado.
 */
public final class BrubinSpawner {
	/** Marca no chunk: já verificado (e, se tinha vila, com Brubin colocada). */
	private static final AttachmentType<Boolean> CHECKED = AttachmentRegistry.createPersistent(
		SurvivalNeeds.id("brubin_chunk"), Codec.BOOL);

	private static final int MAX_WAIT_TICKS = 400;
	private static final int DUPLICATE_RADIUS = 48;

	private record Task(ServerLevel level, ChunkPos chunk, BlockPos center, int[] age) {}

	private static final List<Task> PENDING = new ArrayList<>();
	private static final Random RANDOM = new Random();

	private BrubinSpawner() {}

	public static void init() {
		ServerChunkEvents.CHUNK_LOAD.register(BrubinSpawner::onChunkLoad);
		ServerTickEvents.END_SERVER_TICK.register(server -> processPending());
	}

	private static SurvivalNeedsConfig.Curse cfg() {
		return SurvivalNeedsConfig.get().curse;
	}

	private static void onChunkLoad(ServerLevel level, LevelChunk chunk, boolean newlyGenerated) {
		if (!cfg().enabled || Boolean.TRUE.equals(chunk.getAttached(CHECKED))) {
			return;
		}

		BlockPos villageCenter = villageCenter(level, chunk);
		if (villageCenter != null) {
			PENDING.add(new Task(level, chunk.getPos(), villageCenter, new int[] {0}));
			return;
		}

		// Fora de vilas: uma única chance, por chunk
		chunk.setAttached(CHECKED, true);
		if (RANDOM.nextDouble() < cfg().brubinWildChancePerChunk) {
			int x = chunk.getPos().getMinBlockX() + 3 + RANDOM.nextInt(10);
			int z = chunk.getPos().getMinBlockZ() + 3 + RANDOM.nextInt(10);
			PENDING.add(new Task(level, chunk.getPos(), new BlockPos(x, 64, z), new int[] {0}));
		}
	}

	/** O centro da vila que começa neste chunk, ou null se o chunk não tem o início de uma vila. */
	private static BlockPos villageCenter(ServerLevel level, LevelChunk chunk) {
		for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
			StructureStart start = entry.getValue();
			boolean village = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.wrapAsHolder(entry.getKey()).is(StructureTags.VILLAGE);
			if (village && start.isValid() && start.getChunkPos().equals(chunk.getPos())) {
				return start.getBoundingBox().getCenter();
			}
		}
		return null;
	}

	private static void processPending() {
		Iterator<Task> iterator = PENDING.iterator();
		while (iterator.hasNext()) {
			Task task = iterator.next();
			task.age()[0]++;
			ServerLevel level = task.level();
			LevelChunk chunk = level.getChunkSource().getChunkNow(task.chunk().x(), task.chunk().z());
			if (chunk == null) {
				if (task.age()[0] > MAX_WAIT_TICKS) {
					iterator.remove(); // o chunk descarregou antes: tenta de novo na próxima vez que carregar
				}
				continue;
			}
			if (task.age()[0] < 10) {
				continue; // dá um tempo para os chunks vizinhos carregarem
			}
			iterator.remove();
			if (place(level, task.center())) {
				chunk.setAttached(CHECKED, true);
			} else if (task.age()[0] > MAX_WAIT_TICKS / 2) {
				chunk.setAttached(CHECKED, true); // sem lugar bom: desiste
			}
		}
	}

	/** Procura um chão livre perto de {@code center} (em volta, de dentro para fora) e coloca a Brubin. */
	private static boolean place(ServerLevel level, BlockPos center) {
		AABB around = new AABB(center).inflate(DUPLICATE_RADIUS);
		if (!level.getEntitiesOfClass(PrincessBrubin.class, around).isEmpty()) {
			return true; // já há uma por perto
		}
		for (int radius = 0; radius <= 12; radius++) {
			for (int attempt = 0; attempt < 8; attempt++) {
				int x = center.getX() + (radius == 0 ? 0 : RANDOM.nextInt(radius * 2 + 1) - radius);
				int z = center.getZ() + (radius == 0 ? 0 : RANDOM.nextInt(radius * 2 + 1) - radius);
				if (!level.hasChunkAt(new BlockPos(x, 64, z))) {
					continue;
				}
				int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				BlockPos pos = new BlockPos(x, y, z);
				if (isGoodSpot(level, pos)) {
					PrincessBrubin brubin = ModEntities.PRINCESS_BRUBIN.spawn(level, pos, EntitySpawnReason.STRUCTURE);
					if (brubin != null) {
						brubin.setYRot(RANDOM.nextFloat() * 360.0f);
						brubin.setYHeadRot(brubin.getYRot());
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Chão firme embaixo, ar livre em cima, sem água, sem folhas e com o céu aberto (nada de telhados). */
	private static boolean isGoodSpot(ServerLevel level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
			&& !below.is(net.minecraft.tags.BlockTags.LEAVES)
			&& level.getBlockState(pos).isAir()
			&& level.getBlockState(pos.above()).isAir()
			&& level.getFluidState(pos).isEmpty()
			&& level.getFluidState(pos.below()).isEmpty()
			&& level.canSeeSky(pos.above());
	}
}
