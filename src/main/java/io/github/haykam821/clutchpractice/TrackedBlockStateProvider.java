package io.github.haykam821.clutchpractice;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class TrackedBlockStateProvider {
	private final BlockStateProvider delegate;
	private final Set<BlockState> states = new HashSet<>();

	public TrackedBlockStateProvider(BlockStateProvider delegate) {
		this.delegate = delegate;
	}

	public BlockState get(Random random, BlockPos pos) {
		BlockState state = this.delegate.get(random, pos);
		this.states.add(state);

		return state;
	}

	public Set<BlockState> getStates() {
		return this.states;
	}
}
