package mod.ex3.companion.client

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider
import net.minecraft.core.HolderLookup
import java.util.concurrent.CompletableFuture

object Ex3DataGenerator : DataGeneratorEntrypoint {
	override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
		val pack = fabricDataGenerator.createPack()

		pack.addProvider { output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider> ->
			Ex3EnglishLangProvider(output, registryLookup)
		}

		pack.addProvider { output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider> ->
			Ex3RussianLangProvider(output, registryLookup)
		}
	}
}

private class Ex3EnglishLangProvider(
	output: FabricPackOutput,
	registryLookup: CompletableFuture<HolderLookup.Provider>,
) : FabricLanguageProvider(output, "en_us", registryLookup) {

	override fun generateTranslations(
		registryLookup: HolderLookup.Provider,
		translationBuilder: TranslationBuilder,
	) {
		translationBuilder.add("item.ex3-companion.companion_core", "Ex³ Companion Core")
		translationBuilder.add("entity.ex3-companion.companion", "Ex³ Companion")
		translationBuilder.add("ex3companion.tooltip.health", "\u2764 %s")
		translationBuilder.add("ex3companion.tooltip.level", "\u2726 Lv. %s")
		translationBuilder.add("ex3companion.tooltip.xp", " \u00b7 %s XP")
		translationBuilder.add("ex3companion.tooltip.combat", "\u2694 %s dmg")
		translationBuilder.add("ex3companion.tooltip.attack_speed", " \u00b7 %ss")
		translationBuilder.add("ex3companion.tooltip.healing_active", "\u271a Owner healing: %s HP/s")
		translationBuilder.add("ex3companion.tooltip.healing_locked", "\u271a Healing: Lv. %s (+%s)")
		translationBuilder.add("ex3companion.tooltip.recovering", "Condition: Recovering")
		translationBuilder.add("ex3companion.level_up", "\u2726 Companion leveled up to %s!")
		translationBuilder.add("ex3companion.command.given", "Given companion core (temp test command)")

		// Creative tab
		translationBuilder.add("creativeTab.ex3companion", "Ex³ Companion")

		// Config GUI
		translationBuilder.add("key.ex3companion.config", "Open Companion Config")
		translationBuilder.add("category.ex3companion", "Ex³ Companion")
		translationBuilder.add("ex3companion.config.title", "Ex³ Companion Settings")
		translationBuilder.add("ex3companion.config.tab.server", "Gameplay")
		translationBuilder.add("ex3companion.config.tab.client", "Movement")
		// Config groups
		translationBuilder.add("ex3companion.config.group.health", "Health")
		translationBuilder.add("ex3companion.config.group.combat", "Combat")
		translationBuilder.add("ex3companion.config.group.healing", "Healing")
		translationBuilder.add("ex3companion.config.group.leveling", "Leveling")
		translationBuilder.add("ex3companion.config.group.movement", "Movement")
		// Health
		translationBuilder.add("ex3companion.config.health.base", "Base Health")
		translationBuilder.add("ex3companion.config.health.perLevel", "Health Per Level")
		translationBuilder.add("ex3companion.config.health.cap", "Max Health")
		translationBuilder.add("ex3companion.config.health.regenPerTick", "Regen Per Tick")
		translationBuilder.add("ex3companion.config.health.reviveDelayTicks", "Revive Delay (ticks)")
		translationBuilder.add("ex3companion.config.health.regenLevelScale", "Regen Speed Per Level")
		translationBuilder.add("ex3companion.config.health.invulnerable", "Invulnerable")
		// Combat
		translationBuilder.add("ex3companion.config.combat.damageBase", "Base Damage")
		translationBuilder.add("ex3companion.config.combat.damagePerLevel", "Damage Per Level")
		translationBuilder.add("ex3companion.config.combat.damageCap", "Max Damage")
		translationBuilder.add("ex3companion.config.combat.intervalBase", "Attack Interval Base")
		translationBuilder.add("ex3companion.config.combat.intervalPerLevel", "Interval Reduction Per Level")
		translationBuilder.add("ex3companion.config.combat.intervalMin", "Min Attack Interval")
		translationBuilder.add("ex3companion.config.combat.searchRadius", "Search Radius")
		translationBuilder.add("ex3companion.config.combat.fireRange", "Fire Range")
		translationBuilder.add("ex3companion.config.combat.preferredRange", "Preferred Hover Distance")
		translationBuilder.add("ex3companion.config.combat.abortDistance", "Abort Distance")
		translationBuilder.add("ex3companion.config.combat.xpPerKill", "XP Per Kill")
		translationBuilder.add("ex3companion.config.combat.noTargetTimeout", "No Target Timeout")
		translationBuilder.add("ex3companion.config.combat.staleTargetTimeout", "Stale Target Timeout")
		translationBuilder.add("ex3companion.config.combat.ownerAbortDistance", "Owner Abort Distance")
		translationBuilder.add("ex3companion.config.combat.combatMode", "Combat Mode")
		// Healing
		translationBuilder.add("ex3companion.config.healing.unlockLevel", "Unlock Level")
		translationBuilder.add("ex3companion.config.healing.basePerTick", "Base Heal Per Tick")
		translationBuilder.add("ex3companion.config.healing.perLevelAbove", "Heal Per Level Above")
		translationBuilder.add("ex3companion.config.healing.range", "Heal Range")
		// Explore
		translationBuilder.add("ex3companion.config.tab.explore", "Explore")
		translationBuilder.add("ex3companion.config.group.explore", "Exploration")
		translationBuilder.add("ex3companion.config.explore.cooldownMin", "Exploration Cooldown")
		translationBuilder.add("ex3companion.config.explore.cooldownVariance", "Cooldown Variance")
		translationBuilder.add("ex3companion.config.explore.chance", "Exploration Chance")
		translationBuilder.add("ex3companion.config.explore.durationMin", "Duration Min")
		translationBuilder.add("ex3companion.config.explore.durationVariance", "Duration Variance")
		translationBuilder.add("ex3companion.config.explore.maxOwnerDistance", "Max Owner Distance")
		translationBuilder.add("ex3companion.config.explore.abortDistance", "Abort Distance")
		translationBuilder.add("ex3companion.config.explore.scanIntervalMin", "Scan Interval Min")
		translationBuilder.add("ex3companion.config.explore.scanIntervalVariance", "Scan Interval Variance")
		translationBuilder.add("ex3companion.config.explore.searchRadius", "Search Radius")
		// XP
		translationBuilder.add("ex3companion.config.xp.levelCap", "Level Cap")
		translationBuilder.add("ex3companion.config.xp.formulaMultiplier", "XP Formula Multiplier")
		// Movement
		translationBuilder.add("ex3companion.config.movement.normalSpeed", "Normal Speed")
		translationBuilder.add("ex3companion.config.movement.fastSpeed", "Fast Speed")
		translationBuilder.add("ex3companion.config.movement.catchUpSpeed", "Catch-Up Speed")
		translationBuilder.add("ex3companion.config.movement.exploreSpeed", "Explore Speed")
		translationBuilder.add("ex3companion.config.movement.teleportDistance", "Teleport Distance")
		translationBuilder.add("ex3companion.config.movement.accel", "Velocity Smoothing")
		translationBuilder.add("ex3companion.config.movement.followGain", "Proportional Slow-Down")
		translationBuilder.add("ex3companion.config.movement.maxVertical", "Max Vertical Speed")
	}
}

private class Ex3RussianLangProvider(
	output: FabricPackOutput,
	registryLookup: CompletableFuture<HolderLookup.Provider>,
) : FabricLanguageProvider(output, "ru_ru", registryLookup) {

	override fun generateTranslations(
		registryLookup: HolderLookup.Provider,
		translationBuilder: TranslationBuilder,
	) {
		translationBuilder.add("item.ex3-companion.companion_core", "Ядро Ex³ компаньона")
		translationBuilder.add("entity.ex3-companion.companion", "Ex³ компаньон")
		translationBuilder.add("ex3companion.tooltip.health", "❤ %s")
		translationBuilder.add("ex3companion.tooltip.level", "✦ Ур. %s")
		translationBuilder.add("ex3companion.tooltip.xp", " · %s опыта")
		translationBuilder.add("ex3companion.tooltip.combat", "⚔ %s урн.")
		translationBuilder.add("ex3companion.tooltip.attack_speed", " · %sс")
		translationBuilder.add("ex3companion.tooltip.healing_active", "✚ Лечение владельца: %s ИС/с")
		translationBuilder.add("ex3companion.tooltip.healing_locked", "✚ Лечение: Ур. %s (+%s)")
		translationBuilder.add("ex3companion.tooltip.recovering", "Состояние: Восстановление")
		translationBuilder.add("ex3companion.level_up", "✦ Компаньон повысил уровень до %s!")
		translationBuilder.add("ex3companion.command.given", "Выдано ядро компаньона (временная тестовая команда)")

		// Creative tab
		translationBuilder.add("creativeTab.ex3companion", "Ex³ Companion")

		// Config GUI
		translationBuilder.add("key.ex3companion.config", "Открыть настройки компаньона")
		translationBuilder.add("category.ex3companion", "Ex³ Companion")
		translationBuilder.add("ex3companion.config.title", "Настройки Ex³ компаньона")
		translationBuilder.add("ex3companion.config.tab.server", "Геймплей")
		translationBuilder.add("ex3companion.config.tab.client", "Движение")
		// Config groups
		translationBuilder.add("ex3companion.config.group.health", "Здоровье")
		translationBuilder.add("ex3companion.config.group.combat", "Бой")
		translationBuilder.add("ex3companion.config.group.healing", "Лечение")
		translationBuilder.add("ex3companion.config.group.leveling", "Прокачка")
		translationBuilder.add("ex3companion.config.group.movement", "Движение")
		// Health
		translationBuilder.add("ex3companion.config.health.base", "Базовое здоровье")
		translationBuilder.add("ex3companion.config.health.perLevel", "Здоровье за уровень")
		translationBuilder.add("ex3companion.config.health.cap", "Макс. здоровье")
		translationBuilder.add("ex3companion.config.health.regenPerTick", "Регенерация за тик")
		translationBuilder.add("ex3companion.config.health.reviveDelayTicks", "Задержка возрождения (тики)")
		translationBuilder.add("ex3companion.config.health.regenLevelScale", "Скорость регена за уровень")
		translationBuilder.add("ex3companion.config.health.invulnerable", "Неуязвимость")
		// Combat
		translationBuilder.add("ex3companion.config.combat.damageBase", "Базовый урон")
		translationBuilder.add("ex3companion.config.combat.damagePerLevel", "Урон за уровень")
		translationBuilder.add("ex3companion.config.combat.damageCap", "Макс. урон")
		translationBuilder.add("ex3companion.config.combat.intervalBase", "Интервал атаки (база)")
		translationBuilder.add("ex3companion.config.combat.intervalPerLevel", "Сокращение интервала за уровень")
		translationBuilder.add("ex3companion.config.combat.intervalMin", "Мин. интервал атаки")
		translationBuilder.add("ex3companion.config.combat.searchRadius", "Радиус поиска")
		translationBuilder.add("ex3companion.config.combat.fireRange", "Дальность огня")
		translationBuilder.add("ex3companion.config.combat.preferredRange", "Предпочтительная дистанция зависания")
		translationBuilder.add("ex3companion.config.combat.abortDistance", "Дистанция отступа")
		translationBuilder.add("ex3companion.config.combat.xpPerKill", "Опыта за убийство")
		translationBuilder.add("ex3companion.config.combat.noTargetTimeout", "Таймаут без цели")
		translationBuilder.add("ex3companion.config.combat.staleTargetTimeout", "Таймаут застрявшей цели")
		translationBuilder.add("ex3companion.config.combat.ownerAbortDistance", "Дистанция от владельца для отступа")
		translationBuilder.add("ex3companion.config.combat.combatMode", "Режим боя")
		// Healing
		translationBuilder.add("ex3companion.config.healing.unlockLevel", "Уровень отпирания")
		translationBuilder.add("ex3companion.config.healing.basePerTick", "Базовое лечение за тик")
		translationBuilder.add("ex3companion.config.healing.perLevelAbove", "Лечение за уровень выше")
		translationBuilder.add("ex3companion.config.healing.range", "Дальность лечения")
		// Explore
		translationBuilder.add("ex3companion.config.tab.explore", "Исследование")
		translationBuilder.add("ex3companion.config.group.explore", "Исследование")
		translationBuilder.add("ex3companion.config.explore.cooldownMin", "Кулдаун исследования")
		translationBuilder.add("ex3companion.config.explore.cooldownVariance", "Вариативность кулдауна")
		translationBuilder.add("ex3companion.config.explore.chance", "Шанс исследования")
		translationBuilder.add("ex3companion.config.explore.durationMin", "Мин. длительность")
		translationBuilder.add("ex3companion.config.explore.durationVariance", "Вариативность длительности")
		translationBuilder.add("ex3companion.config.explore.maxOwnerDistance", "Макс. дистанция от владельца")
		translationBuilder.add("ex3companion.config.explore.abortDistance", "Дистанция отступа")
		translationBuilder.add("ex3companion.config.explore.scanIntervalMin", "Мин. интервал сканирования")
		translationBuilder.add("ex3companion.config.explore.scanIntervalVariance", "Вариативность интервала сканирования")
		translationBuilder.add("ex3companion.config.explore.searchRadius", "Радиус поиска")
		// XP
		translationBuilder.add("ex3companion.config.xp.levelCap", "Лимит уровня")
		translationBuilder.add("ex3companion.config.xp.formulaMultiplier", "Множитель опыта формулы")
		// Movement
		translationBuilder.add("ex3companion.config.movement.normalSpeed", "Обычная скорость")
		translationBuilder.add("ex3companion.config.movement.fastSpeed", "Быстрая скорость")
		translationBuilder.add("ex3companion.config.movement.catchUpSpeed", "Скорость догона")
		translationBuilder.add("ex3companion.config.movement.exploreSpeed", "Скорость исследования")
		translationBuilder.add("ex3companion.config.movement.teleportDistance", "Дистанция телепорта")
		translationBuilder.add("ex3companion.config.movement.accel", "Сглаживание скорости")
		translationBuilder.add("ex3companion.config.movement.followGain", "Замедление у цели")
		translationBuilder.add("ex3companion.config.movement.maxVertical", "Макс. вертикальная скорость")
	}
}
