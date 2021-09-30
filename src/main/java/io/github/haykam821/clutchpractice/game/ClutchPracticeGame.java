package io.github.haykam821.clutchpractice.game;

import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMapBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ClutchPracticeGame implements GameActivityEvents.Tick, BlockPlaceEvent.Before, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent, GamePlayerEvents.Remove, BlockUseEvent {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final ClutchPracticeMap map;
	private final ClutchPracticeConfig config;

	private ServerPlayerEntity mainPlayer;
	private int streak = 0;
	private int maxStreak = 0;

	public ClutchPracticeGame(GameSpace gameSpace, ServerWorld world, ClutchPracticeMap map, ClutchPracticeConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.DISMOUNT_VEHICLE);
		activity.allow(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.allow(GameRuleType.INTERACTION);
		activity.allow(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PLAYER_PROJECTILE_KNOCKBACK);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
		activity.deny(GameRuleType.TRIDENTS_LOYAL_IN_VOID);
		activity.deny(GameRuleType.UNSTABLE_TNT);
	}

	public static GameOpenProcedure open(GameOpenContext<ClutchPracticeConfig> context) {
		ClutchPracticeConfig config = context.config();
		ClutchPracticeMap map = new ClutchPracticeMapBuilder(config.getMapConfig()).build(context.server());

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()))
			.setGameRule(GameRules.DO_TILE_DROPS, false);

		return context.openWithWorld(worldConfig, (activity, world) -> {
			ClutchPracticeGame phase = new ClutchPracticeGame(activity.getGameSpace(), world, map, config);
			ClutchPracticeGame.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(BlockPlaceEvent.BEFORE, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
			activity.listen(BlockUseEvent.EVENT, phase);
		});
	}

	// Listeners
	public void onTick() {
		if (this.mainPlayer == null) return;

		if (this.map.getExit().contains(this.mainPlayer.getPos())) {
			this.gameSpace.getPlayers().kick(this.mainPlayer);
		}
		this.map.respawnIfOutOfBounds(this.mainPlayer);

		if (this.mainPlayer.isOnGround() && this.mainPlayer.getY() == this.map.getArea().min().getY() + 1) {
			this.resetAndUpdateStreak(ActionResult.SUCCESS);
		}
	}

	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getSpawn()).and(() -> {
			if (this.mainPlayer == null) {
				this.mainPlayer = offer.player();
				this.reset(false);
				offer.player().changeGameMode(GameMode.ADVENTURE);
			} else {
				offer.player().changeGameMode(GameMode.SPECTATOR);
			}
		});
	}

	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float damage) {
		if (player == this.mainPlayer) {
			this.resetAndUpdateStreak(ActionResult.FAIL);
		}
		return ActionResult.FAIL;
	}

	public ActionResult onPlace(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, ItemUsageContext context) {
		return this.getAreaPosResult(pos);
	}

	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		if (player == this.mainPlayer) {
			this.resetAndUpdateStreak(ActionResult.FAIL);
		}
		return ActionResult.FAIL;
	}

	public void onRemovePlayer(ServerPlayerEntity player) {
		if (player == this.mainPlayer) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	public ActionResult onUse(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		return this.getAreaPosResult(hitResult.getBlockPos());
	}

	// Utilities
	private void resetAndUpdateStreak(ActionResult result) {
		map.spawn(this.mainPlayer);

		if (result == ActionResult.SUCCESS) {
			this.streak += 1;
			if (this.streak <= this.maxStreak) {
				this.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
			} else {
				this.maxStreak = this.streak;
				this.sendSound(SoundEvents.ENTITY_PLAYER_LEVELUP);
			}
		} else if (result == ActionResult.FAIL) {
			this.sendSound(SoundEvents.ENTITY_VILLAGER_NO);
			this.streak = 0;
		}

		this.reset(true);
	}

	private void sendSound(SoundEvent sound) {
		this.gameSpace.getPlayers().playSound(sound, SoundCategory.PLAYERS, 1, 1);
	}

	private void reset(boolean spawn) {
		if (spawn) {
			map.spawn(this.mainPlayer);
		}
		BlockState baseState = map.clearArea(this.world);

		ItemStack ladder = ItemStackBuilder.of(Items.LADDER)
			.addCanPlaceOn(baseState.getBlock())
			.build();

		this.mainPlayer.getInventory().clear();
		this.mainPlayer.getInventory().offerOrDrop(ladder);

		this.mainPlayer.setExperienceLevel(this.streak);
	}

	private ActionResult getAreaPosResult(BlockPos pos) {
		if (this.map.getArea().contains(pos)) {
			return ActionResult.PASS;
		}
		return ActionResult.FAIL;
	}
}
