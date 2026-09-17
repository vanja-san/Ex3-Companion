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

/**
 * Translation keys follow the ConfigLib TXF conventions (see TXFConfigClient):
 *  - title    <key>.config.title
 *  - category <key>.config.category.<tabId>
 *  - entry    <key>.config.<fieldName>
 *  - tooltip  <key>.config.<fieldName>.tooltip
 *  - enum     <key>.config.enum.CombatMode.<VALUE>
 *  - comment  <key>.config.<sectionFieldName>  (section headers / spacers)
 *
 * The config is registered once under `ex3-companion`; the screen shows a
 * single "server" tab (gameplay, synced) with section headers.
 */
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
		translationBuilder.add("ex3-companion.config.title", "Ex³ Companion Settings")
		translationBuilder.add("ex3-companion.config.category.server", "Server")

		// Config section headers (Comment fields)
		translationBuilder.add("ex3-companion.config.sectionHealth", "Health")
		translationBuilder.add("ex3-companion.config.sectionCombat", "Combat")
		translationBuilder.add("ex3-companion.config.sectionHealing", "Healing")
		translationBuilder.add("ex3-companion.config.sectionMovement", "Movement")
		translationBuilder.add("ex3-companion.config.sectionExplore", "Exploration")
		translationBuilder.add("ex3-companion.config.sectionXp", "Leveling")

		// Health
		translationBuilder.add("ex3-companion.config.healthBase", "Base Health")
		translationBuilder.add("ex3-companion.config.healthBase.tooltip", "Base HP at level 1")
		translationBuilder.add("ex3-companion.config.healthPerLevel", "Health Per Level")
		translationBuilder.add("ex3-companion.config.healthPerLevel.tooltip", "HP added per level")
		translationBuilder.add("ex3-companion.config.healthCap", "Max Health")
		translationBuilder.add("ex3-companion.config.healthCap.tooltip", "Maximum possible HP")
		translationBuilder.add("ex3-companion.config.healthRegenPerTick", "Regen Per Tick")
		translationBuilder.add("ex3-companion.config.healthRegenPerTick.tooltip", "HP regenerated per tick")
		translationBuilder.add("ex3-companion.config.healthReviveDelayTicks", "Revive Delay (ticks)")
		translationBuilder.add("ex3-companion.config.healthReviveDelayTicks.tooltip", "Ticks after death before revival (0 = immediately)")
		translationBuilder.add("ex3-companion.config.healthRegenLevelScale", "Regen Speed Per Level")
		translationBuilder.add("ex3-companion.config.healthRegenLevelScale.tooltip", "Regen speed increase per level (e.g. 0.05 = +5%/level)")
		translationBuilder.add("ex3-companion.config.healthInvulnerable", "Invulnerable")
		translationBuilder.add("ex3-companion.config.healthInvulnerable.tooltip", "When enabled, the companion cannot take damage from mobs")

		// Combat
		translationBuilder.add("ex3-companion.config.combatDamageBase", "Base Damage")
		translationBuilder.add("ex3-companion.config.combatDamageBase.tooltip", "Base attack damage")
		translationBuilder.add("ex3-companion.config.combatDamagePerLevel", "Damage Per Level")
		translationBuilder.add("ex3-companion.config.combatDamagePerLevel.tooltip", "Damage added per level")
		translationBuilder.add("ex3-companion.config.combatDamageCap", "Max Damage")
		translationBuilder.add("ex3-companion.config.combatDamageCap.tooltip", "Maximum attack damage")
		translationBuilder.add("ex3-companion.config.combatIntervalBase", "Attack Interval Base")
		translationBuilder.add("ex3-companion.config.combatIntervalBase.tooltip", "Ticks between attacks at level 1")
		translationBuilder.add("ex3-companion.config.combatIntervalPerLevel", "Interval Reduction Per Level")
		translationBuilder.add("ex3-companion.config.combatIntervalPerLevel.tooltip", "Interval reduction per level")
		translationBuilder.add("ex3-companion.config.combatIntervalMin", "Min Attack Interval")
		translationBuilder.add("ex3-companion.config.combatIntervalMin.tooltip", "Minimum attack interval in ticks")
		translationBuilder.add("ex3-companion.config.combatSearchRadius", "Search Radius")
		translationBuilder.add("ex3-companion.config.combatSearchRadius.tooltip", "Blocks to scan for hostiles")
		translationBuilder.add("ex3-companion.config.combatFireRange", "Fire Range")
		translationBuilder.add("ex3-companion.config.combatFireRange.tooltip", "Max distance to fire beam")
		translationBuilder.add("ex3-companion.config.combatPreferredRange", "Preferred Hover Distance")
		translationBuilder.add("ex3-companion.config.combatPreferredRange.tooltip", "Preferred hover distance from target while firing")
		translationBuilder.add("ex3-companion.config.combatAbortDistance", "Abort Distance")
		translationBuilder.add("ex3-companion.config.combatAbortDistance.tooltip", "Abort combat beyond this distance")
		translationBuilder.add("ex3-companion.config.combatOwnerAbortDistance", "Owner Abort Distance")
		translationBuilder.add("ex3-companion.config.combatOwnerAbortDistance.tooltip", "Distance from owner at which combat aborts")
		translationBuilder.add("ex3-companion.config.combatXpPerKill", "XP Per Kill")
		translationBuilder.add("ex3-companion.config.combatXpPerKill.tooltip", "XP granted per kill assist")
		translationBuilder.add("ex3-companion.config.combatNoTargetTimeout", "No Target Timeout")
		translationBuilder.add("ex3-companion.config.combatNoTargetTimeout.tooltip", "Ticks before disengage if mob stops attacking")
		translationBuilder.add("ex3-companion.config.combatStaleTargetTimeout", "Stale Target Timeout")
		translationBuilder.add("ex3-companion.config.combatStaleTargetTimeout.tooltip", "Ticks before disengage if target heals faster than damage")
		translationBuilder.add("ex3-companion.config.combatCombatMode", "Combat Mode")
		translationBuilder.add("ex3-companion.config.combatCombatMode.tooltip", "Defender: fight mobs attacking owner. Aggressive: seek any hostile. Strategic: consider mob type.")
		translationBuilder.add("ex3-companion.config.enum.CombatMode.DEFENDER", "Defender")
		translationBuilder.add("ex3-companion.config.enum.CombatMode.AGGRESSIVE", "Aggressive")
		translationBuilder.add("ex3-companion.config.enum.CombatMode.STRATEGIC", "Strategic")

		// Healing
		translationBuilder.add("ex3-companion.config.healingUnlockLevel", "Unlock Level")
		translationBuilder.add("ex3-companion.config.healingUnlockLevel.tooltip", "Level to unlock owner healing")
		translationBuilder.add("ex3-companion.config.healingBasePerTick", "Base Heal Per Tick")
		translationBuilder.add("ex3-companion.config.healingBasePerTick.tooltip", "Base HP healed per tick")
		translationBuilder.add("ex3-companion.config.healingPerLevelAbove", "Heal Per Level Above")
		translationBuilder.add("ex3-companion.config.healingPerLevelAbove.tooltip", "Extra heal per level above unlock")
		translationBuilder.add("ex3-companion.config.healingMaxPerTick", "Max Heal Per Tick")
		translationBuilder.add("ex3-companion.config.healingMaxPerTick.tooltip", "Hard cap on owner heal rate")
		translationBuilder.add("ex3-companion.config.healingRange", "Heal Range")
		translationBuilder.add("ex3-companion.config.healingRange.tooltip", "Max distance for healing")

		// Movement
		translationBuilder.add("ex3-companion.config.movementNormalSpeed", "Normal Speed")
		translationBuilder.add("ex3-companion.config.movementNormalSpeed.tooltip", "Base follow speed")
		translationBuilder.add("ex3-companion.config.movementFastSpeed", "Fast Speed")
		translationBuilder.add("ex3-companion.config.movementFastSpeed.tooltip", "Speed when lagging behind")
		translationBuilder.add("ex3-companion.config.movementCatchUpSpeed", "Catch-Up Speed")
		translationBuilder.add("ex3-companion.config.movementCatchUpSpeed.tooltip", "Sprint speed to catch up")
		translationBuilder.add("ex3-companion.config.movementExploreSpeed", "Explore Speed")
		translationBuilder.add("ex3-companion.config.movementExploreSpeed.tooltip", "Speed while exploring")
		translationBuilder.add("ex3-companion.config.movementTeleportDistance", "Teleport Distance")
		translationBuilder.add("ex3-companion.config.movementTeleportDistance.tooltip", "Distance at which companion teleports")
		translationBuilder.add("ex3-companion.config.movementAccel", "Velocity Smoothing")
		translationBuilder.add("ex3-companion.config.movementAccel.tooltip", "Velocity smoothing (0-1, higher = snappier)")
		translationBuilder.add("ex3-companion.config.movementFollowGain", "Proportional Slow-Down")
		translationBuilder.add("ex3-companion.config.movementFollowGain.tooltip", "Proportional slow-down near goal")
		translationBuilder.add("ex3-companion.config.movementMaxVertical", "Max Vertical Speed")
		translationBuilder.add("ex3-companion.config.movementMaxVertical.tooltip", "Max vertical speed (blocks/tick)")

		// Explore
		translationBuilder.add("ex3-companion.config.exploreCooldownMin", "Exploration Cooldown")
		translationBuilder.add("ex3-companion.config.exploreCooldownMin.tooltip", "Min ticks between explorations (~25 s at 20 TPS)")
		translationBuilder.add("ex3-companion.config.exploreCooldownVariance", "Cooldown Variance")
		translationBuilder.add("ex3-companion.config.exploreCooldownVariance.tooltip", "Extra random ticks added to cooldown")
		translationBuilder.add("ex3-companion.config.exploreChance", "Exploration Chance")
		translationBuilder.add("ex3-companion.config.exploreChance.tooltip", "Chance to explore when cooldown expires")
		translationBuilder.add("ex3-companion.config.exploreDurationMin", "Duration Min")
		translationBuilder.add("ex3-companion.config.exploreDurationMin.tooltip", "Min exploration duration in ticks")
		translationBuilder.add("ex3-companion.config.exploreDurationVariance", "Duration Variance")
		translationBuilder.add("ex3-companion.config.exploreDurationVariance.tooltip", "Extra random ticks added to duration")
		translationBuilder.add("ex3-companion.config.exploreMaxOwnerDistance", "Max Owner Distance")
		translationBuilder.add("ex3-companion.config.exploreMaxOwnerDistance.tooltip", "Max distance from owner to allow exploration")
		translationBuilder.add("ex3-companion.config.exploreAbortDistance", "Abort Distance")
		translationBuilder.add("ex3-companion.config.exploreAbortDistance.tooltip", "Abort exploration beyond this distance")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalMin", "Scan Interval Min")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalMin.tooltip", "Min ticks between scan rotations at POI")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalVariance", "Scan Interval Variance")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalVariance.tooltip", "Extra random ticks added to scan interval")
		translationBuilder.add("ex3-companion.config.exploreSearchRadius", "Search Radius")
		translationBuilder.add("ex3-companion.config.exploreSearchRadius.tooltip", "Blocks to scan for interesting blocks")

		// XP
		translationBuilder.add("ex3-companion.config.xpLevelCap", "Level Cap")
		translationBuilder.add("ex3-companion.config.xpLevelCap.tooltip", "Maximum companion level")
		translationBuilder.add("ex3-companion.config.xpFormulaMultiplier", "XP Formula Multiplier")
		translationBuilder.add("ex3-companion.config.xpFormulaMultiplier.tooltip", "XP required = level² × multiplier")
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
		translationBuilder.add("ex3-companion.config.title", "Настройки Ex³ компаньона")
		translationBuilder.add("ex3-companion.config.category.server", "Сервер")

		// Config section headers (Comment fields)
		translationBuilder.add("ex3-companion.config.sectionHealth", "Здоровье")
		translationBuilder.add("ex3-companion.config.sectionCombat", "Бой")
		translationBuilder.add("ex3-companion.config.sectionHealing", "Лечение")
		translationBuilder.add("ex3-companion.config.sectionMovement", "Движение")
		translationBuilder.add("ex3-companion.config.sectionExplore", "Исследование")
		translationBuilder.add("ex3-companion.config.sectionXp", "Прокачка")

		// Health
		translationBuilder.add("ex3-companion.config.healthBase", "Базовое здоровье")
		translationBuilder.add("ex3-companion.config.healthBase.tooltip", "Базовое здоровье на 1 уровне")
		translationBuilder.add("ex3-companion.config.healthPerLevel", "Здоровье за уровень")
		translationBuilder.add("ex3-companion.config.healthPerLevel.tooltip", "Здоровье, добавляемое за уровень")
		translationBuilder.add("ex3-companion.config.healthCap", "Макс. здоровье")
		translationBuilder.add("ex3-companion.config.healthCap.tooltip", "Максимально возможное здоровье")
		translationBuilder.add("ex3-companion.config.healthRegenPerTick", "Регенерация за тик")
		translationBuilder.add("ex3-companion.config.healthRegenPerTick.tooltip", "Здоровье, восстанавливаемое за тик")
		translationBuilder.add("ex3-companion.config.healthReviveDelayTicks", "Задержка возрождения (тики)")
		translationBuilder.add("ex3-companion.config.healthReviveDelayTicks.tooltip", "Тики после смерти до возрождения (0 = сразу)")
		translationBuilder.add("ex3-companion.config.healthRegenLevelScale", "Скорость регена за уровень")
		translationBuilder.add("ex3-companion.config.healthRegenLevelScale.tooltip", "Ускорение регенерации за уровень (напр. 0.05 = +5%/уровень)")
		translationBuilder.add("ex3-companion.config.healthInvulnerable", "Неуязвимость")
		translationBuilder.add("ex3-companion.config.healthInvulnerable.tooltip", "Включено — компаньон не получает урон от мобов")

		// Combat
		translationBuilder.add("ex3-companion.config.combatDamageBase", "Базовый урон")
		translationBuilder.add("ex3-companion.config.combatDamageBase.tooltip", "Базовый урон атаки")
		translationBuilder.add("ex3-companion.config.combatDamagePerLevel", "Урон за уровень")
		translationBuilder.add("ex3-companion.config.combatDamagePerLevel.tooltip", "Урон, добавляемый за уровень")
		translationBuilder.add("ex3-companion.config.combatDamageCap", "Макс. урон")
		translationBuilder.add("ex3-companion.config.combatDamageCap.tooltip", "Максимальный урон атаки")
		translationBuilder.add("ex3-companion.config.combatIntervalBase", "Интервал атаки (база)")
		translationBuilder.add("ex3-companion.config.combatIntervalBase.tooltip", "Тики между атаками на 1 уровне")
		translationBuilder.add("ex3-companion.config.combatIntervalPerLevel", "Сокращение интервала за уровень")
		translationBuilder.add("ex3-companion.config.combatIntervalPerLevel.tooltip", "Сокращение интервала за уровень")
		translationBuilder.add("ex3-companion.config.combatIntervalMin", "Мин. интервал атаки")
		translationBuilder.add("ex3-companion.config.combatIntervalMin.tooltip", "Минимальный интервал атаки (тики)")
		translationBuilder.add("ex3-companion.config.combatSearchRadius", "Радиус поиска")
		translationBuilder.add("ex3-companion.config.combatSearchRadius.tooltip", "Радиус поиска врагов (в блоках)")
		translationBuilder.add("ex3-companion.config.combatFireRange", "Дальность огня")
		translationBuilder.add("ex3-companion.config.combatFireRange.tooltip", "Максимальная дистанция выстрела луча")
		translationBuilder.add("ex3-companion.config.combatPreferredRange", "Предпочтительная дистанция зависания")
		translationBuilder.add("ex3-companion.config.combatPreferredRange.tooltip", "Предпочтительная дистанция зависания при стрельбе")
		translationBuilder.add("ex3-companion.config.combatAbortDistance", "Дистанция отступа")
		translationBuilder.add("ex3-companion.config.combatAbortDistance.tooltip", "Отмена боя за этой дистанцией")
		translationBuilder.add("ex3-companion.config.combatOwnerAbortDistance", "Дистанция от владельца для отступа")
		translationBuilder.add("ex3-companion.config.combatOwnerAbortDistance.tooltip", "Дистанция от владельца, при которой бой отменяется")
		translationBuilder.add("ex3-companion.config.combatXpPerKill", "Опыта за убийство")
		translationBuilder.add("ex3-companion.config.combatXpPerKill.tooltip", "Опыт за помощь в убийстве")
		translationBuilder.add("ex3-companion.config.combatNoTargetTimeout", "Таймаут без цели")
		translationBuilder.add("ex3-companion.config.combatNoTargetTimeout.tooltip", "Тики до отхода, если моб перестал атаковать")
		translationBuilder.add("ex3-companion.config.combatStaleTargetTimeout", "Таймаут застрявшей цели")
		translationBuilder.add("ex3-companion.config.combatStaleTargetTimeout.tooltip", "Тики до отхода, если цель лечится быстрее, чем получает урон")
		translationBuilder.add("ex3-companion.config.combatCombatMode", "Режим боя")
		translationBuilder.add("ex3-companion.config.combatCombatMode.tooltip", "Защитник: бьёт мобов, атакующих владельца. Агрессивный: ищет любого врага. Стратегический: учитывает тип моба.")
		translationBuilder.add("ex3-companion.config.enum.CombatMode.DEFENDER", "Защитник")
		translationBuilder.add("ex3-companion.config.enum.CombatMode.AGGRESSIVE", "Агрессивный")
		translationBuilder.add("ex3-companion.config.enum.CombatMode.STRATEGIC", "Стратегический")

		// Healing
		translationBuilder.add("ex3-companion.config.healingUnlockLevel", "Уровень отпирания")
		translationBuilder.add("ex3-companion.config.healingUnlockLevel.tooltip", "Уровень, открывающий лечение владельца")
		translationBuilder.add("ex3-companion.config.healingBasePerTick", "Базовое лечение за тик")
		translationBuilder.add("ex3-companion.config.healingBasePerTick.tooltip", "Базовое здоровье за тик")
		translationBuilder.add("ex3-companion.config.healingPerLevelAbove", "Лечение за уровень выше")
		translationBuilder.add("ex3-companion.config.healingPerLevelAbove.tooltip", "Доп. лечение за уровень выше открытия")
		translationBuilder.add("ex3-companion.config.healingMaxPerTick", "Макс. лечение за тик")
		translationBuilder.add("ex3-companion.config.healingMaxPerTick.tooltip", "Максимальная скорость лечения владельца")
		translationBuilder.add("ex3-companion.config.healingRange", "Дальность лечения")
		translationBuilder.add("ex3-companion.config.healingRange.tooltip", "Максимальная дистанция лечения")

		// Movement
		translationBuilder.add("ex3-companion.config.movementNormalSpeed", "Обычная скорость")
		translationBuilder.add("ex3-companion.config.movementNormalSpeed.tooltip", "Базовая скорость следования")
		translationBuilder.add("ex3-companion.config.movementFastSpeed", "Быстрая скорость")
		translationBuilder.add("ex3-companion.config.movementFastSpeed.tooltip", "Скорость при отставании")
		translationBuilder.add("ex3-companion.config.movementCatchUpSpeed", "Скорость догона")
		translationBuilder.add("ex3-companion.config.movementCatchUpSpeed.tooltip", "Скорость спринта для догона")
		translationBuilder.add("ex3-companion.config.movementExploreSpeed", "Скорость исследования")
		translationBuilder.add("ex3-companion.config.movementExploreSpeed.tooltip", "Скорость при исследовании")
		translationBuilder.add("ex3-companion.config.movementTeleportDistance", "Дистанция телепорта")
		translationBuilder.add("ex3-companion.config.movementTeleportDistance.tooltip", "Дистанция, на которой компаньон телепортируется")
		translationBuilder.add("ex3-companion.config.movementAccel", "Сглаживание скорости")
		translationBuilder.add("ex3-companion.config.movementAccel.tooltip", "Сглаживание скорости (0-1, больше = резче)")
		translationBuilder.add("ex3-companion.config.movementFollowGain", "Замедление у цели")
		translationBuilder.add("ex3-companion.config.movementFollowGain.tooltip", "Пропорциональное замедление у цели")
		translationBuilder.add("ex3-companion.config.movementMaxVertical", "Макс. вертикальная скорость")
		translationBuilder.add("ex3-companion.config.movementMaxVertical.tooltip", "Макс. вертикальная скорость (блоков/тик)")

		// Explore
		translationBuilder.add("ex3-companion.config.exploreCooldownMin", "Кулдаун исследования")
		translationBuilder.add("ex3-companion.config.exploreCooldownMin.tooltip", "Мин. тики между исследованиями (~25 с при 20 TPS)")
		translationBuilder.add("ex3-companion.config.exploreCooldownVariance", "Вариативность кулдауна")
		translationBuilder.add("ex3-companion.config.exploreCooldownVariance.tooltip", "Доп. случайные тики к кулдауну")
		translationBuilder.add("ex3-companion.config.exploreChance", "Шанс исследования")
		translationBuilder.add("ex3-companion.config.exploreChance.tooltip", "Шанс исследования по истечении кулдауна")
		translationBuilder.add("ex3-companion.config.exploreDurationMin", "Мин. длительность")
		translationBuilder.add("ex3-companion.config.exploreDurationMin.tooltip", "Мин. длительность исследования (тики)")
		translationBuilder.add("ex3-companion.config.exploreDurationVariance", "Вариативность длительности")
		translationBuilder.add("ex3-companion.config.exploreDurationVariance.tooltip", "Доп. случайные тики к длительности")
		translationBuilder.add("ex3-companion.config.exploreMaxOwnerDistance", "Макс. дистанция от владельца")
		translationBuilder.add("ex3-companion.config.exploreMaxOwnerDistance.tooltip", "Макс. дистанция от владельца для исследования")
		translationBuilder.add("ex3-companion.config.exploreAbortDistance", "Дистанция отступа")
		translationBuilder.add("ex3-companion.config.exploreAbortDistance.tooltip", "Отмена исследования за этой дистанцией")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalMin", "Мин. интервал сканирования")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalMin.tooltip", "Мин. тики между оборотами скана в точке интереса")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalVariance", "Вариативность интервала сканирования")
		translationBuilder.add("ex3-companion.config.exploreScanIntervalVariance.tooltip", "Доп. случайные тики к интервалу скана")
		translationBuilder.add("ex3-companion.config.exploreSearchRadius", "Радиус поиска")
		translationBuilder.add("ex3-companion.config.exploreSearchRadius.tooltip", "Радиус поиска интересных блоков")

		// XP
		translationBuilder.add("ex3-companion.config.xpLevelCap", "Лимит уровня")
		translationBuilder.add("ex3-companion.config.xpLevelCap.tooltip", "Максимальный уровень компаньона")
		translationBuilder.add("ex3-companion.config.xpFormulaMultiplier", "Множитель опыта формулы")
		translationBuilder.add("ex3-companion.config.xpFormulaMultiplier.tooltip", "Требуемый опыт = уровень² × множитель")
	}
}