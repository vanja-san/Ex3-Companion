package mod.ex3.companion.client.config

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.LongSliderControllerBuilder
import mod.ex3.companion.companion.CombatMode
import mod.ex3.companion.config.CompanionConfig
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object CompanionConfigFactory {

    fun createScreen(parent: Screen?): Screen {
        val cfg = CompanionConfig.get()

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("ex3companion.config.title"))
            .category(gameplayCategory(cfg))
            .category(exploreCategory(cfg))
            .category(movementCategory(cfg))
            .save { CompanionConfig.save() }
            .build()
            .generateScreen(parent)
    }

    private fun gameplayCategory(cfg: CompanionConfig): ConfigCategory =
        ConfigCategory.createBuilder()
            .name(Component.translatable("ex3companion.config.tab.server"))
            .group(healthGroup(cfg))
            .group(combatGroup(cfg))
            .group(healingGroup(cfg))
            .group(xpGroup(cfg))
            .build()

    private fun exploreCategory(cfg: CompanionConfig): ConfigCategory =
        ConfigCategory.createBuilder()
            .name(Component.translatable("ex3companion.config.tab.explore"))
            .group(exploreGroup(cfg))
            .build()

    private fun movementCategory(cfg: CompanionConfig): ConfigCategory =
        ConfigCategory.createBuilder()
            .name(Component.translatable("ex3companion.config.tab.client"))
            .group(movementGroup(cfg))
            .build()

    private fun healthGroup(cfg: CompanionConfig): OptionGroup =
        OptionGroup.createBuilder()
            .name(Component.translatable("ex3companion.config.group.health"))
            .option(
                floatOption(
                    "ex3companion.config.health.base",
                    "Base HP at level 1",
                    cfg.health.base, 1f, 100f, 1f,
                ) { cfg.health.base = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.health.perLevel",
                    "HP added per level",
                    cfg.health.perLevel, 0f, 20f, 0.5f,
                ) { cfg.health.perLevel = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.health.cap",
                    "Maximum possible HP",
                    cfg.health.cap, 10f, 200f, 5f,
                ) { cfg.health.cap = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.health.regenPerTick",
                    "HP regenerated per tick",
                    cfg.health.regenPerTick, 0f, 2f, 0.01f,
                ) { cfg.health.regenPerTick = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.health.regenLevelScale",
                    "Regen speed increase per level (e.g. 0.05 = +5%/level)",
                    cfg.health.regenLevelScale, 0f, 1f, 0.01f,
                ) { cfg.health.regenLevelScale = it },
            )
            .option(
                Option.createBuilder<Long>()
                    .name(Component.translatable("ex3companion.config.health.reviveDelayTicks"))
                    .description(OptionDescription.of(Component.literal("Ticks after death before revival")))
                    .binding(Binding.generic(cfg.health.reviveDelayTicks, { cfg.health.reviveDelayTicks }, { cfg.health.reviveDelayTicks = it }))
                    .controller { opt -> LongSliderControllerBuilder.create(opt).range(0L, 1200L).step(20L) }
                    .build(),
            )
            .option(
                Option.createBuilder<Boolean>()
                    .name(Component.translatable("ex3companion.config.health.invulnerable"))
                    .description(OptionDescription.of(Component.literal("When enabled, the companion cannot take damage from mobs")))
                    .binding(Binding.generic(cfg.health.invulnerable, { cfg.health.invulnerable }, { cfg.health.invulnerable = it }))
                    .controller { opt -> BooleanControllerBuilder.create(opt) }
                    .build(),
            )
            .build()

    private fun combatGroup(cfg: CompanionConfig): OptionGroup =
        OptionGroup.createBuilder()
            .name(Component.translatable("ex3companion.config.group.combat"))
            .option(
                floatOption(
                    "ex3companion.config.combat.damageBase",
                    "Base attack damage",
                    cfg.combat.damageBase, 0f, 30f, 0.5f,
                ) { cfg.combat.damageBase = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.combat.damagePerLevel",
                    "Damage added per level",
                    cfg.combat.damagePerLevel, 0f, 10f, 0.5f,
                ) { cfg.combat.damagePerLevel = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.combat.damageCap",
                    "Maximum attack damage",
                    cfg.combat.damageCap, 1f, 50f, 1f,
                ) { cfg.combat.damageCap = it },
            )
            .option(
                intOption(
                    "ex3companion.config.combat.intervalBase",
                    "Ticks between attacks at level 1",
                    cfg.combat.intervalBase, 1, 200,
                ) { cfg.combat.intervalBase = it },
            )
            .option(
                intOption(
                    "ex3companion.config.combat.intervalPerLevel",
                    "Interval reduction per level",
                    cfg.combat.intervalPerLevel, 0, 20,
                ) { cfg.combat.intervalPerLevel = it },
            )
            .option(
                intOption(
                    "ex3companion.config.combat.intervalMin",
                    "Minimum attack interval in ticks",
                    cfg.combat.intervalMin, 1, 100,
                ) { cfg.combat.intervalMin = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.combat.searchRadius",
                    "Blocks to scan for hostiles",
                    cfg.combat.searchRadius, 4.0, 64.0, 1.0,
                ) { cfg.combat.searchRadius = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.combat.fireRange",
                    "Max distance to fire beam",
                    cfg.combat.fireRange, 2.0, 32.0, 0.5,
                ) { cfg.combat.fireRange = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.combat.abortDistance",
                    "Abort combat beyond this distance",
                    cfg.combat.abortDistance, 5.0, 64.0, 1.0,
                ) { cfg.combat.abortDistance = it },
            )
            .option(
                intOption(
                    "ex3companion.config.combat.xpPerKill",
                    "XP granted per kill assist",
                    cfg.combat.xpPerKill, 1, 100,
                ) { cfg.combat.xpPerKill = it },
            )
			.option(
				intOption(
					"ex3companion.config.combat.noTargetTimeout",
					"Ticks before disengage if mob stops attacking",
					cfg.combat.noTargetTimeout, 20, 600,
				) { cfg.combat.noTargetTimeout = it },
			)
			.option(
				Option.createBuilder<CombatMode>()
					.name(Component.translatable("ex3companion.config.combat.combatMode"))
					.description(OptionDescription.of(Component.literal("Defender: fight mobs attacking owner. Aggressive: seek any hostile. Strategic: consider mob type.")))
					.binding(Binding.generic(cfg.combat.combatMode, { cfg.combat.combatMode }, { cfg.combat.combatMode = it }))
					.controller { opt -> EnumControllerBuilder.create(opt).enumClass(CombatMode::class.java) }
					.build(),
			)
			.build()

    private fun healingGroup(cfg: CompanionConfig): OptionGroup =
        OptionGroup.createBuilder()
            .name(Component.translatable("ex3companion.config.group.healing"))
            .option(
                intOption(
                    "ex3companion.config.healing.unlockLevel",
                    "Level to unlock owner healing",
                    cfg.healing.unlockLevel, 1, 50,
                ) { cfg.healing.unlockLevel = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.healing.basePerTick",
                    "Base HP healed per tick",
                    cfg.healing.basePerTick, 0f, 1f, 0.005f,
                ) { cfg.healing.basePerTick = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.healing.perLevelAbove",
                    "Extra heal per level above unlock",
                    cfg.healing.perLevelAbove, 0f, 0.1f, 0.001f,
                ) { cfg.healing.perLevelAbove = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.healing.range",
                    "Max distance for healing",
                    cfg.healing.range, 1.0, 16.0, 0.5,
                ) { cfg.healing.range = it },
            )
            .build()

    private fun exploreGroup(cfg: CompanionConfig): OptionGroup =
        OptionGroup.createBuilder()
            .name(Component.translatable("ex3companion.config.group.explore"))
            .option(
                intOption(
                    "ex3companion.config.explore.cooldownMin",
                    "Min ticks between explorations (~25 s at 20 TPS)",
                    cfg.explore.cooldownMin, 100, 2000,
                ) { cfg.explore.cooldownMin = it },
            )
            .option(
                intOption(
                    "ex3companion.config.explore.cooldownVariance",
                    "Extra random ticks added to cooldown",
                    cfg.explore.cooldownVariance, 0, 2000,
                ) { cfg.explore.cooldownVariance = it },
            )
            .option(
                floatOption(
                    "ex3companion.config.explore.chance",
                    "Chance to explore when cooldown expires",
                    cfg.explore.chance, 0f, 1f, 0.05f,
                ) { cfg.explore.chance = it },
            )
            .option(
                intOption(
                    "ex3companion.config.explore.durationMin",
                    "Min exploration duration in ticks",
                    cfg.explore.durationMin, 20, 600,
                ) { cfg.explore.durationMin = it },
            )
            .option(
                intOption(
                    "ex3companion.config.explore.durationVariance",
                    "Extra random ticks added to duration",
                    cfg.explore.durationVariance, 0, 600,
                ) { cfg.explore.durationVariance = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.explore.maxOwnerDistance",
                    "Max distance from owner to allow exploration",
                    cfg.explore.maxOwnerDistance, 8.0, 128.0, 1.0,
                ) { cfg.explore.maxOwnerDistance = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.explore.abortDistance",
                    "Abort exploration beyond this distance",
                    cfg.explore.abortDistance, 10.0, 128.0, 1.0,
                ) { cfg.explore.abortDistance = it },
            )
            .option(
                intOption(
                    "ex3companion.config.explore.scanIntervalMin",
                    "Min ticks between scan rotations at POI",
                    cfg.explore.scanIntervalMin, 10, 200,
                ) { cfg.explore.scanIntervalMin = it },
            )
            .option(
                intOption(
                    "ex3companion.config.explore.scanIntervalVariance",
                    "Extra random ticks added to scan interval",
                    cfg.explore.scanIntervalVariance, 0, 200,
                ) { cfg.explore.scanIntervalVariance = it },
            )
            .option(
                intOption(
                    "ex3companion.config.explore.searchRadius",
                    "Blocks to scan for interesting blocks",
                    cfg.explore.searchRadius, 4, 64,
                ) { cfg.explore.searchRadius = it },
            )
            .build()

    private fun xpGroup(cfg: CompanionConfig): OptionGroup =
        OptionGroup.createBuilder()
            .name(Component.translatable("ex3companion.config.group.leveling"))
            .option(
                intOption(
                    "ex3companion.config.xp.levelCap",
                    "Maximum companion level",
                    cfg.xp.levelCap, 1, 100,
                ) { cfg.xp.levelCap = it },
            )
            .option(
                intOption(
                    "ex3companion.config.xp.formulaMultiplier",
                    "XP required = level × multiplier",
                    cfg.xp.formulaMultiplier, 1, 100,
                ) { cfg.xp.formulaMultiplier = it },
            )
            .build()

    private fun movementGroup(cfg: CompanionConfig): OptionGroup =
        OptionGroup.createBuilder()
            .name(Component.translatable("ex3companion.config.group.movement"))
            .option(
                doubleOption(
                    "ex3companion.config.movement.normalSpeed",
                    "Base follow speed",
                    cfg.movement.normalSpeed, 0.01, 2.0, 0.01,
                ) { cfg.movement.normalSpeed = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.movement.fastSpeed",
                    "Speed when lagging behind",
                    cfg.movement.fastSpeed, 0.05, 3.0, 0.05,
                ) { cfg.movement.fastSpeed = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.movement.catchUpSpeed",
                    "Sprint speed to catch up",
                    cfg.movement.catchUpSpeed, 0.1, 5.0, 0.1,
                ) { cfg.movement.catchUpSpeed = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.movement.exploreSpeed",
                    "Speed while exploring",
                    cfg.movement.exploreSpeed, 0.01, 2.0, 0.01,
                ) { cfg.movement.exploreSpeed = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.movement.teleportDistance",
                    "Distance at which companion teleports",
                    cfg.movement.teleportDistance, 8.0, 256.0, 8.0,
                ) { cfg.movement.teleportDistance = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.movement.accel",
                    "Velocity smoothing (0-1, higher = snappier)",
                    cfg.movement.accel, 0.05, 1.0, 0.05,
                ) { cfg.movement.accel = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.movement.followGain",
                    "Proportional slow-down near goal",
                    cfg.movement.followGain, 0.05, 1.0, 0.05,
                ) { cfg.movement.followGain = it },
            )
            .option(
                doubleOption(
                    "ex3companion.config.movement.maxVertical",
                    "Max vertical speed (blocks/tick)",
                    cfg.movement.maxVertical, 0.05, 2.0, 0.05,
                ) { cfg.movement.maxVertical = it },
            )
            .build()

    // ── Helpers ──

    private fun floatOption(
        key: String,
        desc: String,
        current: Float,
        min: Float,
        max: Float,
        step: Float,
        setter: (Float) -> Unit,
    ): Option<Float> =
        Option.createBuilder<Float>()
            .name(Component.translatable(key))
            .description(OptionDescription.of(Component.literal(desc)))
            .binding(Binding.generic(current, { current }, setter))
            .controller { opt -> FloatSliderControllerBuilder.create(opt).range(min, max).step(step) }
            .build()

    private fun intOption(
        key: String,
        desc: String,
        current: Int,
        min: Int,
        max: Int,
        setter: (Int) -> Unit,
    ): Option<Int> =
        Option.createBuilder<Int>()
            .name(Component.translatable(key))
            .description(OptionDescription.of(Component.literal(desc)))
            .binding(Binding.generic(current, { current }, setter))
            .controller { opt -> IntegerFieldControllerBuilder.create(opt).min(min).max(max) }
            .build()

    private fun doubleOption(
        key: String,
        desc: String,
        current: Double,
        min: Double,
        max: Double,
        step: Double,
        setter: (Double) -> Unit,
    ): Option<Double> =
        Option.createBuilder<Double>()
            .name(Component.translatable(key))
            .description(OptionDescription.of(Component.literal(desc)))
            .binding(Binding.generic(current, { current }, setter))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(min, max).step(step) }
            .build()
}
