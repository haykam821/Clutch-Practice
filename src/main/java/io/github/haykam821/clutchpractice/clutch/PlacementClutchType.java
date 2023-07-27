package io.github.haykam821.clutchpractice.clutch;

import java.util.Set;
import java.util.function.Consumer;

import io.github.haykam821.clutchpractice.TrackedBlockStateProvider;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class PlacementClutchType extends ClutchType {
	private final ItemStack stack;

	protected PlacementClutchType(ItemConvertible item) {
		super(new ItemStack(item));

		this.stack = new ItemStack(item);
	}

	@Override
	protected Text createName() {
		return this.stack.getName();
	}

	@Override
	public void addItems(Consumer<ItemStack> adder, Set<BlockState> floor, Set<BlockState> base) {
		ItemStackBuilder builder = ItemStackBuilder.of(this.stack);

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
