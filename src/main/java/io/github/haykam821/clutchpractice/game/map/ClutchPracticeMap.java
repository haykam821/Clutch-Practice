package io.github.haykam821.clutchpractice.game.map;

import java.util.Random;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.map.template.TemplateRegion;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class ClutchPracticeMap {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private final ClutchPracticeMapConfig config;
	private final MapTemplate template;
	private final Box box;
	private final BlockBounds area;
	private final Box exit;

	public ClutchPracticeMap(ClutchPracticeMapConfig config, MapTemplate template) {
		this.config = config;
		this.template = template;

		this.box = this.template.getBounds().toBox();

		this.area = ClutchPracticeMap.getBounds(template, "area");
		this.exit = ClutchPracticeMap.getBox(template, "exit");
	}

	public BlockBounds getArea() {
		return this.area;
	}

	public BlockState clearArea(ServerWorld world) {
		Random random = world.getRandom();
		int minY = this.area.getMin().getY();

		for (BlockPos pos : this.area) {
			if (pos.getY() == minY) {
				world.setBlockState(pos, this.config.getFloorProvider().getBlockState(random, pos));
			} else {
				world.setBlockState(pos, AIR);
			}
		}

		// Place base
		int baseX = MathHelper.nextInt(random, this.area.getMin().getX(), this.area.getMax().getX());
		int baseZ = MathHelper.nextInt(random, this.area.getMin().getZ(), this.area.getMax().getZ());

		BlockPos basePos = new BlockPos(baseX, minY + 1, baseZ);
		BlockState baseState = this.config.getBaseProvider().getBlockState(random, basePos);

		world.setBlockState(basePos, baseState);
		return baseState;
	}

	public Box getExit() {
		return this.exit;
	}

	private Vec3d getSpawn() {
		TemplateRegion spawn = this.template.getMetadata().getFirstRegion("spawn");
		if (spawn != null) {
			return spawn.getBounds().getCenterBottom();
		}

		return new Vec3d(0.5, 76, 0.5);
	}

	private Vec2f getSpawnRotation() {
		TemplateRegion spawn = this.template.getMetadata().getFirstRegion("spawn");
		if (spawn != null) {
			ListTag tag = spawn.getData().getList("Rotation", NbtType.FLOAT);
			return new Vec2f(tag.getFloat(0), tag.getFloat(1));
		}

		return new Vec2f(90, 0);
	}

	public void spawn(ServerPlayerEntity player) {
		Vec3d spawn = this.getSpawn();
		Vec2f rotation = this.getSpawnRotation();

		player.teleport(player.getServerWorld(), spawn.getX(), spawn.getY(), spawn.getZ(), rotation.x, rotation.y);
	}

	public boolean respawnIfOutOfBounds(ServerPlayerEntity player) {
		if (this.box.contains(player.getPos())) {
			return false;
		}

		this.spawn(player);
		return true;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}

	private static BlockBounds getBounds(MapTemplate template, String marker) {
		BlockBounds bounds = template.getMetadata().getFirstRegionBounds(marker);
		return bounds == null ? BlockBounds.EMPTY : bounds;
	}

	private static Box getBox(MapTemplate template, String marker) {
		BlockBounds bounds = template.getMetadata().getFirstRegionBounds(marker);
		return bounds == null ? new Box(Vec3d.ZERO, Vec3d.ZERO) : bounds.toBox();
	}
}
