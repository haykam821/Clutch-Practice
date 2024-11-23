package io.github.haykam821.clutchpractice.game;

import io.github.haykam821.clutchpractice.TrackedBlockStateProvider;
import io.github.haykam821.clutchpractice.clutch.ClutchType;
import io.github.haykam821.clutchpractice.clutch.ClutchTypes;
import io.github.haykam821.clutchpractice.game.map.ClutchDisplay;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMapBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.JoinOfferResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ClutchPracticeGame implements GameActivityEvents.Tick, BlockPlaceEvent.Before, GamePlayerEvents.Accept, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent, GamePlayerEvents.Remove, BlockUseEvent {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final ClutchPracticeMap map;
	private final ClutchPracticeConfig config;
	private final ClutchDisplay clutchDisplay;

	private ServerPlayerEntity mainPlayer;
	private ClutchType clutchType = ClutchTypes.RANDOM;
	private int streak = 0;
	private int maxStreak = 0;

	public ClutchPracticeGame(GameSpace gameSpace, ServerWorld world, ClutchPracticeMap map, ClutchPracticeConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;

		Vec3d clutchDisplayPos = this.map.getClutchDisplayPos();
		this.clutchDisplay = clutchDisplayPos == null ? null : new ClutchDisplay(world, clutchDisplayPos, this.clutchType);
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.DISMOUNT_VEHICLE);
		activity.allow(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.FLUID_FLOW);
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
			activity.listen(GamePlayerEvents.ACCEPT, phase);
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

		if (this.isOnClutchGround(this.mainPlayer)) {
			this.resetAndUpdateStreak(ClutchResult.SUCCESS);
		}
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.map.getSpawn()).thenRunForEach(player -> {
			if (this.mainPlayer == null) {
				this.mainPlayer = player;
				this.reset(false);
				player.changeGameMode(GameMode.ADVENTURE);
			} else {
				player.changeGameMode(GameMode.SPECTATOR);
			}
		});
	}

	public JoinOfferResult onOfferPlayers(JoinOffer offer) {
		return this.mainPlayer == null ? offer.acceptParticipants() : offer.acceptSpectators();
	}

	public EventResult onDamage(ServerPlayerEntity player, DamageSource source, float damage) {
		if (player == this.mainPlayer) {
			this.resetAndUpdateStreak(ClutchResult.FAIL);
		}
		return EventResult.DENY;
	}

	public EventResult onPlace(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, ItemUsageContext context) {
		return this.getAreaPosResult(pos);
	}

	public EventResult onDeath(ServerPlayerEntity player, DamageSource source) {
		if (player == this.mainPlayer) {
			this.resetAndUpdateStreak(ClutchResult.FAIL);
		}
		return EventResult.DENY;
	}

	public void onRemovePlayer(ServerPlayerEntity player) {
		if (player == this.mainPlayer) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	public ActionResult onUse(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		BlockPos pos = hitResult.getBlockPos();

		if (player == this.mainPlayer && hand == Hand.MAIN_HAND) {
			BlockBounds clutchSelector = this.map.getClutchSelector();

			if (clutchSelector != null && clutchSelector.contains(pos)) {
				this.clutchType = ClutchTypes.getNext(this.clutchType);
				this.sendSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);

				this.streak = 0;
				this.maxStreak = 0;

				this.reset(false);

				if (this.clutchDisplay != null) {
					this.clutchDisplay.setType(this.clutchType);
				}

				return ActionResult.SUCCESS_SERVER;
			}
		}

		return this.getAreaPosResult(pos).asActionResult();
	}

	// Utilities
	private boolean isOnClutchGround(ServerPlayerEntity player) {
		if (!player.isOnGround()) return false;

		BlockBounds area = this.map.getArea();
		return player.getY() == area.min().getY() + 1 || area.asBox().intersects(player.getBoundingBox());
	}

	private void resetAndUpdateStreak(ClutchResult result) {
		map.spawn(this.mainPlayer);

		if (result == ClutchResult.SUCCESS) {
			this.streak += 1;
			if (this.streak <= this.maxStreak) {
				this.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
			} else {
				this.maxStreak = this.streak;
				this.sendSound(SoundEvents.ENTITY_PLAYER_LEVELUP);
			}
		} else if (result == ClutchResult.FAIL) {
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

		TrackedBlockStateProvider floor = this.map.getTrackedFloorProvider();
		TrackedBlockStateProvider base = this.map.getTrackedBaseProvider();

		ClutchType clutchType = this.clutchType.resolve(this.world.getRandom());
		clutchType.clearArea(this.world, this.map, floor, base);

		PlayerInventory inventory = this.mainPlayer.getInventory();

		inventory.clear();
		clutchType.addItems(inventory::offerOrDrop, floor.getStates(), base.getStates(), this.world.getRegistryManager());

		this.mainPlayer.setExperienceLevel(this.streak);
	}

	private EventResult getAreaPosResult(BlockPos pos) {
		if (this.map.getArea().contains(pos)) {
			return EventResult.PASS;
		}
		return EventResult.DENY;
	}
}
