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
		translationBuilder.add("ex3companion.config.tab.server", "Server")
		translationBuilder.add("ex3companion.config.tab.server.oponly", "Operator-only — requires operator permissions on this server")
		translationBuilder.add("ex3companion.config.tab.client", "Client")
		// Client-side config
		translationBuilder.add("ex3companion.config.group.rendering", "Rendering")
		translationBuilder.add("ex3companion.config.client.dynamicLight", "Companion Dynamic Light")
		translationBuilder.add("ex3companion.config.client.dynamicLight.desc", "Companion emits light in the dark (requires LambDynamicLights)")
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
		translationBuilder.add("ex3companion.config.health.base.desc", "Base HP at level 1")
		translationBuilder.add("ex3companion.config.health.perLevel.desc", "HP added per level")
		translationBuilder.add("ex3companion.config.health.cap.desc", "Maximum possible HP")
		translationBuilder.add("ex3companion.config.health.regenPerTick.desc", "HP regenerated per tick")
		translationBuilder.add("ex3companion.config.health.reviveDelayTicks.desc", "Ticks after death before revival")
		translationBuilder.add("ex3companion.config.health.regenLevelScale.desc", "Regen speed increase per level (e.g. 0.05 = +5%/level)")
		translationBuilder.add("ex3companion.config.health.invulnerable.desc", "When enabled, the companion cannot take damage from mobs")
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
		translationBuilder.add("ex3companion.config.combat.damageBase.desc", "Base attack damage")
		translationBuilder.add("ex3companion.config.combat.damagePerLevel.desc", "Damage added per level")
		translationBuilder.add("ex3companion.config.combat.damageCap.desc", "Maximum attack damage")
		translationBuilder.add("ex3companion.config.combat.intervalBase.desc", "Ticks between attacks at level 1")
		translationBuilder.add("ex3companion.config.combat.intervalPerLevel.desc", "Interval reduction per level")
		translationBuilder.add("ex3companion.config.combat.intervalMin.desc", "Minimum attack interval in ticks")
		translationBuilder.add("ex3companion.config.combat.searchRadius.desc", "Blocks to scan for hostiles")
		translationBuilder.add("ex3companion.config.combat.fireRange.desc", "Max distance to fire beam")
		translationBuilder.add("ex3companion.config.combat.preferredRange.desc", "Preferred hover distance from target while firing")
		translationBuilder.add("ex3companion.config.combat.abortDistance.desc", "Abort combat beyond this distance")
		translationBuilder.add("ex3companion.config.combat.xpPerKill.desc", "XP granted per kill assist")
		translationBuilder.add("ex3companion.config.combat.noTargetTimeout.desc", "Ticks before disengage if mob stops attacking")
		translationBuilder.add("ex3companion.config.combat.staleTargetTimeout.desc", "Ticks before disengage if target heals faster than damage")
		translationBuilder.add("ex3companion.config.combat.ownerAbortDistance.desc", "Distance from owner at which combat aborts")
		translationBuilder.add("ex3companion.config.combat.combatMode.desc", "Defender: fight mobs attacking owner. Aggressive: seek any hostile. Strategic: consider mob type.")
		// Healing
		translationBuilder.add("ex3companion.config.healing.unlockLevel", "Unlock Level")
		translationBuilder.add("ex3companion.config.healing.basePerTick", "Base Heal Per Tick")
		translationBuilder.add("ex3companion.config.healing.perLevelAbove", "Heal Per Level Above")
		translationBuilder.add("ex3companion.config.healing.range", "Heal Range")
		translationBuilder.add("ex3companion.config.healing.unlockLevel.desc", "Level to unlock owner healing")
		translationBuilder.add("ex3companion.config.healing.basePerTick.desc", "Base HP healed per tick")
		translationBuilder.add("ex3companion.config.healing.perLevelAbove.desc", "Extra heal per level above unlock")
		translationBuilder.add("ex3companion.config.healing.range.desc", "Max distance for healing")
		// Explore
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
		translationBuilder.add("ex3companion.config.explore.cooldownMin.desc", "Min ticks between explorations (~25 s at 20 TPS)")
		translationBuilder.add("ex3companion.config.explore.cooldownVariance.desc", "Extra random ticks added to cooldown")
		translationBuilder.add("ex3companion.config.explore.chance.desc", "Chance to explore when cooldown expires")
		translationBuilder.add("ex3companion.config.explore.durationMin.desc", "Min exploration duration in ticks")
		translationBuilder.add("ex3companion.config.explore.durationVariance.desc", "Extra random ticks added to duration")
		translationBuilder.add("ex3companion.config.explore.maxOwnerDistance.desc", "Max distance from owner to allow exploration")
		translationBuilder.add("ex3companion.config.explore.abortDistance.desc", "Abort exploration beyond this distance")
		translationBuilder.add("ex3companion.config.explore.scanIntervalMin.desc", "Min ticks between scan rotations at POI")
		translationBuilder.add("ex3companion.config.explore.scanIntervalVariance.desc", "Extra random ticks added to scan interval")
		translationBuilder.add("ex3companion.config.explore.searchRadius.desc", "Blocks to scan for interesting blocks")
		// XP
		translationBuilder.add("ex3companion.config.xp.levelCap", "Level Cap")
		translationBuilder.add("ex3companion.config.xp.formulaMultiplier", "XP Formula Multiplier")
		translationBuilder.add("ex3companion.config.xp.levelCap.desc", "Maximum companion level")
		translationBuilder.add("ex3companion.config.xp.formulaMultiplier.desc", "XP required = level × multiplier")
		// Movement
		translationBuilder.add("ex3companion.config.movement.normalSpeed", "Normal Speed")
		translationBuilder.add("ex3companion.config.movement.fastSpeed", "Fast Speed")
		translationBuilder.add("ex3companion.config.movement.catchUpSpeed", "Catch-Up Speed")
		translationBuilder.add("ex3companion.config.movement.exploreSpeed", "Explore Speed")
		translationBuilder.add("ex3companion.config.movement.teleportDistance", "Teleport Distance")
		translationBuilder.add("ex3companion.config.movement.accel", "Velocity Smoothing")
		translationBuilder.add("ex3companion.config.movement.followGain", "Proportional Slow-Down")
		translationBuilder.add("ex3companion.config.movement.maxVertical", "Max Vertical Speed")
		translationBuilder.add("ex3companion.config.movement.normalSpeed.desc", "Base follow speed")
		translationBuilder.add("ex3companion.config.movement.fastSpeed.desc", "Speed when lagging behind")
		translationBuilder.add("ex3companion.config.movement.catchUpSpeed.desc", "Sprint speed to catch up")
		translationBuilder.add("ex3companion.config.movement.exploreSpeed.desc", "Speed while exploring")
		translationBuilder.add("ex3companion.config.movement.teleportDistance.desc", "Distance at which companion teleports")
		translationBuilder.add("ex3companion.config.movement.accel.desc", "Velocity smoothing (0-1, higher = snappier)")
		translationBuilder.add("ex3companion.config.movement.followGain.desc", "Proportional slow-down near goal")
		translationBuilder.add("ex3companion.config.movement.maxVertical.desc", "Max vertical speed (blocks/tick)")
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
		translationBuilder.add("ex3companion.config.tab.server", "Сервер")
		translationBuilder.add("ex3companion.config.tab.server.oponly", "Только для операторов — требуются права оператора на этом сервере")
		translationBuilder.add("ex3companion.config.tab.client", "Клиент")
		// Client-side config
		translationBuilder.add("ex3companion.config.group.rendering", "Отображение")
		translationBuilder.add("ex3companion.config.client.dynamicLight", "Подсветка компаньона")
		translationBuilder.add("ex3companion.config.client.dynamicLight.desc", "Компаньон излучает свет в темноте (требуется LambDynamicLights)")
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
		translationBuilder.add("ex3companion.config.health.base.desc", "Базовое здоровье на 1 уровне")
		translationBuilder.add("ex3companion.config.health.perLevel.desc", "Здоровье, добавляемое за уровень")
		translationBuilder.add("ex3companion.config.health.cap.desc", "Максимально возможное здоровье")
		translationBuilder.add("ex3companion.config.health.regenPerTick.desc", "Здоровье, восстанавливаемое за тик")
		translationBuilder.add("ex3companion.config.health.reviveDelayTicks.desc", "Тики после смерти до возрождения")
		translationBuilder.add("ex3companion.config.health.regenLevelScale.desc", "Ускорение регенерации за уровень (напр. 0.05 = +5%/уровень)")
		translationBuilder.add("ex3companion.config.health.invulnerable.desc", "Включено — компаньон не получает урон от мобов")
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
		translationBuilder.add("ex3companion.config.combat.damageBase.desc", "Базовый урон атаки")
		translationBuilder.add("ex3companion.config.combat.damagePerLevel.desc", "Урон, добавляемый за уровень")
		translationBuilder.add("ex3companion.config.combat.damageCap.desc", "Максимальный урон атаки")
		translationBuilder.add("ex3companion.config.combat.intervalBase.desc", "Тики между атаками на 1 уровне")
		translationBuilder.add("ex3companion.config.combat.intervalPerLevel.desc", "Сокращение интервала за уровень")
		translationBuilder.add("ex3companion.config.combat.intervalMin.desc", "Минимальный интервал атаки (тики)")
		translationBuilder.add("ex3companion.config.combat.searchRadius.desc", "Радиус поиска врагов (в блоках)")
		translationBuilder.add("ex3companion.config.combat.fireRange.desc", "Максимальная дистанция выстрела луча")
		translationBuilder.add("ex3companion.config.combat.preferredRange.desc", "Предпочтительная дистанция зависания при стрельбе")
		translationBuilder.add("ex3companion.config.combat.abortDistance.desc", "Отмена боя за этой дистанцией")
		translationBuilder.add("ex3companion.config.combat.xpPerKill.desc", "Опыт за помощь в убийстве")
		translationBuilder.add("ex3companion.config.combat.noTargetTimeout.desc", "Тики до отхода, если моб перестал атаковать")
		translationBuilder.add("ex3companion.config.combat.staleTargetTimeout.desc", "Тики до отхода, если цель лечится быстрее, чем получает урон")
		translationBuilder.add("ex3companion.config.combat.ownerAbortDistance.desc", "Дистанция от владельца, при которой бой отменяется")
		translationBuilder.add("ex3companion.config.combat.combatMode.desc", "Защитник: бьёт мобов, атакующих владельца. Агрессивный: ищет любого врага. Стратегический: учитывает тип моба.")
		// Healing
		translationBuilder.add("ex3companion.config.healing.unlockLevel", "Уровень отпирания")
		translationBuilder.add("ex3companion.config.healing.basePerTick", "Базовое лечение за тик")
		translationBuilder.add("ex3companion.config.healing.perLevelAbove", "Лечение за уровень выше")
		translationBuilder.add("ex3companion.config.healing.range", "Дальность лечения")
		translationBuilder.add("ex3companion.config.healing.unlockLevel.desc", "Уровень, открывающий лечение владельца")
		translationBuilder.add("ex3companion.config.healing.basePerTick.desc", "Базовое здоровье за тик")
		translationBuilder.add("ex3companion.config.healing.perLevelAbove.desc", "Доп. лечение за уровень выше открытия")
		translationBuilder.add("ex3companion.config.healing.range.desc", "Максимальная дистанция лечения")
		// Explore
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
		translationBuilder.add("ex3companion.config.explore.cooldownMin.desc", "Мин. тики между исследованиями (~25 с при 20 TPS)")
		translationBuilder.add("ex3companion.config.explore.cooldownVariance.desc", "Доп. случайные тики к кулдауну")
		translationBuilder.add("ex3companion.config.explore.chance.desc", "Шанс исследования по истечении кулдауна")
		translationBuilder.add("ex3companion.config.explore.durationMin.desc", "Мин. длительность исследования (тики)")
		translationBuilder.add("ex3companion.config.explore.durationVariance.desc", "Доп. случайные тики к длительности")
		translationBuilder.add("ex3companion.config.explore.maxOwnerDistance.desc", "Макс. дистанция от владельца для исследования")
		translationBuilder.add("ex3companion.config.explore.abortDistance.desc", "Отмена исследования за этой дистанцией")
		translationBuilder.add("ex3companion.config.explore.scanIntervalMin.desc", "Мин. тики между оборотами скана в точке интереса")
		translationBuilder.add("ex3companion.config.explore.scanIntervalVariance.desc", "Доп. случайные тики к интервалу скана")
		translationBuilder.add("ex3companion.config.explore.searchRadius.desc", "Радиус поиска интересных блоков")
		// XP
		translationBuilder.add("ex3companion.config.xp.levelCap", "Лимит уровня")
		translationBuilder.add("ex3companion.config.xp.formulaMultiplier", "Множитель опыта формулы")
		translationBuilder.add("ex3companion.config.xp.levelCap.desc", "Максимальный уровень компаньона")
		translationBuilder.add("ex3companion.config.xp.formulaMultiplier.desc", "Требуемый опыт = уровень × множитель")
		// Movement
		translationBuilder.add("ex3companion.config.movement.normalSpeed", "Обычная скорость")
		translationBuilder.add("ex3companion.config.movement.fastSpeed", "Быстрая скорость")
		translationBuilder.add("ex3companion.config.movement.catchUpSpeed", "Скорость догона")
		translationBuilder.add("ex3companion.config.movement.exploreSpeed", "Скорость исследования")
		translationBuilder.add("ex3companion.config.movement.teleportDistance", "Дистанция телепорта")
		translationBuilder.add("ex3companion.config.movement.accel", "Сглаживание скорости")
		translationBuilder.add("ex3companion.config.movement.followGain", "Замедление у цели")
		translationBuilder.add("ex3companion.config.movement.maxVertical", "Макс. вертикальная скорость")
		translationBuilder.add("ex3companion.config.movement.normalSpeed.desc", "Базовая скорость следования")
		translationBuilder.add("ex3companion.config.movement.fastSpeed.desc", "Скорость при отставании")
		translationBuilder.add("ex3companion.config.movement.catchUpSpeed.desc", "Скорость спринта для догона")
		translationBuilder.add("ex3companion.config.movement.exploreSpeed.desc", "Скорость при исследовании")
		translationBuilder.add("ex3companion.config.movement.teleportDistance.desc", "Дистанция, на которой компаньон телепортируется")
		translationBuilder.add("ex3companion.config.movement.accel.desc", "Сглаживание скорости (0-1, больше = резче)")
		translationBuilder.add("ex3companion.config.movement.followGain.desc", "Пропорциональное замедление у цели")
		translationBuilder.add("ex3companion.config.movement.maxVertical.desc", "Макс. вертикальная скорость (блоков/тик)")
	}
}
