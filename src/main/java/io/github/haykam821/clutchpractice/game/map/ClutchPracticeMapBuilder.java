package io.github.haykam821.clutchpractice.game.map;

import java.io.IOException;

import net.minecraft.text.TranslatableText;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;

public class ClutchPracticeMapBuilder {
	private final ClutchPracticeMapConfig config;

	public ClutchPracticeMapBuilder(ClutchPracticeMapConfig config) {
		this.config = config;
	}

	public ClutchPracticeMap build() {
		try {
			MapTemplate template = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.getId());
			return new ClutchPracticeMap(this.config, template);
		} catch (IOException exception) {
			throw new GameOpenException(new TranslatableText("text.clutchpractice.template_load_failed"), exception);
		}
	}
}