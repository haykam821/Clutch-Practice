package io.github.haykam821.clutchpractice.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMapConfig;

public class ClutchPracticeConfig {
	public static final Codec<ClutchPracticeConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			ClutchPracticeMapConfig.CODEC.fieldOf("map").forGetter(ClutchPracticeConfig::getMapConfig)
		).apply(instance, ClutchPracticeConfig::new);
	});

	private final ClutchPracticeMapConfig mapConfig;

	public ClutchPracticeConfig(ClutchPracticeMapConfig mapConfig) {
		this.mapConfig = mapConfig;
	}

	public ClutchPracticeMapConfig getMapConfig() {
		return this.mapConfig;
	}
}
