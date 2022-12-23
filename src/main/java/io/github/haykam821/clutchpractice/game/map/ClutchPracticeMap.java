package io.github.haykam821.clutchpractice.game.map;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class ClutchPracticeMap {
	private static final BlockBounds EMPTY_BOUNDS = BlockBounds.ofBlock(BlockPos.ORIGIN);
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private final ClutchPracticeMapConfig config;
	private final MapTemplate template;
	private final Box box;
	private final BlockBounds area;
	private final Box exit;

	public ClutchPracticeMap(ClutchPracticeMapConfig config, MapTemplate template) {
		this.config = config;
		this.template = template;

		this.box = this.template.getBounds().asBox();

		this.area = ClutchPracticeMap.getBounds(template, "area");
		this.exit = ClutchPracticeMap.getBox(template, "exit");
	}

	public BlockBounds getArea() {
		return this.area;
	}

	public BlockState clearArea(ServerWorld world) {
		Random random = world.getRandom();
		int minY = this.area.min().getY();

		for (BlockPos pos : this.area) {
			if (pos.getY() == minY) {
				world.setBlockState(pos, this.config.getFloorProvider().get(random, pos));
			} else {
				world.setBlockState(pos, AIR);
			}
		}

		// Place base
		int baseX = MathHelper.nextInt(random, this.area.min().getX(), this.area.max().getX());
		int baseZ = MathHelper.nextInt(random, this.area.min().getZ(), this.area.max().getZ());

		BlockPos basePos = new BlockPos(baseX, minY + 1, baseZ);
		BlockState baseState = this.config.getBaseProvider().get(random, basePos);

		world.setBlockState(basePos, baseState);
		return baseState;
	}

	public Box getExit() {
		return this.exit;
	}

	public Vec3d getSpawn() {
		TemplateRegion spawn = this.template.getMetadata().getFirstRegion("spawn");
		if (spawn != null) {
			return spawn.getBounds().centerBottom();
		}

		return new Vec3d(0.5, 76, 0.5);
	}

	private Vec2f getSpawnRotation() {
		TemplateRegion spawn = this.template.getMetadata().getFirstRegion("spawn");
		if (spawn != null) {
			NbtList tag = spawn.getData().getList("Rotation", NbtType.FLOAT);
			return new Vec2f(tag.getFloat(0), tag.getFloat(1));
		}

		return new Vec2f(90, 0);
	}

	public void spawn(ServerPlayerEntity player) {
		Vec3d spawn = this.getSpawn();
		Vec2f rotation = this.getSpawnRotation();

		player.teleport(player.getWorld(), spawn.getX(), spawn.getY(), spawn.getZ(), rotation.x, rotation.y);
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
		return bounds == null ? EMPTY_BOUNDS : bounds;
	}

	private static Box getBox(MapTemplate template, String marker) {
		BlockBounds bounds = template.getMetadata().getFirstRegionBounds(marker);
		return bounds == null ? new Box(Vec3d.ZERO, Vec3d.ZERO) : bounds.asBox();
	}
}
