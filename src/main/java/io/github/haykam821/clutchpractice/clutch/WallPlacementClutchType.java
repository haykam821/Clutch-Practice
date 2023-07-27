package io.github.haykam821.clutchpractice.clutch;

import io.github.haykam821.clutchpractice.TrackedBlockStateProvider;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;

public class WallPlacementClutchType extends PlacementClutchType {
	protected WallPlacementClutchType(Block block) {
		super(block);
	}

	@Override
	public void clearArea(ServerWorld world, ClutchPracticeMap map, TrackedBlockStateProvider floor, TrackedBlockStateProvider base) {
		map.clearArea(world, floor);
		map.placeRandomBase(world, base, 1);
	}
}
