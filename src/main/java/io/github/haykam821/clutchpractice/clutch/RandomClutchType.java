package io.github.haykam821.clutchpractice.clutch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import io.github.haykam821.clutchpractice.TrackedBlockStateProvider;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

public class RandomClutchType extends ClutchType {
	protected RandomClutchType() {
		super(Items.BUNDLE.getDefaultStack());
	}

	@Override
	public ClutchType resolve(Random random) {
		List<ClutchType> types = new ArrayList<>(ClutchTypes.REGISTRY.values());
		types.remove(this);

		return Util.getRandom(types, random);
	}

	@Override
	public void addItems(Consumer<ItemStack> adder, Set<BlockState> floor, Set<BlockState> base) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearArea(ServerWorld world, ClutchPracticeMap map, TrackedBlockStateProvider floor, TrackedBlockStateProvider base) {
		throw new UnsupportedOperationException();
	}
}
