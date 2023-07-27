package io.github.haykam821.clutchpractice.clutch;

import java.util.Set;
import java.util.function.Consumer;

import io.github.haykam821.clutchpractice.TrackedBlockStateProvider;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class PlacementClutchType extends ClutchType {
	private final Block block;

	protected PlacementClutchType(Block block) {
		super(block.asItem().getDefaultStack());

		this.block = block;
	}

	@Override
	protected Text createName() {
		return this.block.getName();
	}

	@Override
	public void addItems(Consumer<ItemStack> adder, Set<BlockState> floor, Set<BlockState> base) {
		ItemStackBuilder builder = ItemStackBuilder.of(this.block);

		for (BlockState state : base) {
			builder.addCanPlaceOn(state.getBlock());
		}

		adder.accept(builder.build());
	}

	@Override
	public void clearArea(ServerWorld world, ClutchPracticeMap map, TrackedBlockStateProvider floor, TrackedBlockStateProvider base) {
		map.clearArea(world, floor);
		map.placeRandomBase(world, base, 0);
	}
}
