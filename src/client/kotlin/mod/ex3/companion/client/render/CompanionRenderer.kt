package mod.ex3.companion.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import mod.ex3.companion.companion.CompanionEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.block.BlockModelRenderState
import net.minecraft.client.renderer.block.model.BlockDisplayContext
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Blocks

/**
 * Render state carrying the two block models (glass shell + sea-lantern core)
 * plus animation values extracted from the entity.
 */
class CompanionRenderState : EntityRenderState() {
	val glassBlock: BlockModelRenderState = BlockModelRenderState()
	val lampBlock: BlockModelRenderState = BlockModelRenderState()
	var spinDegrees: Float = 0f
	var bobOffset: Float = 0f
	var coreScale: Float = 0.30f
}

/**
 * Renders the companion as a floating glass cube with a small glowing
 * sea-lantern core inside. Vanilla block models are reused directly,
 * so all UV mapping / texture folding is exactly like in-game blocks.
 *
 * Uses the 26.x submit-based pipeline: block states are resolved during
 * [extractRenderState] and submitted in [submit].
 */
class CompanionRenderer(context: EntityRendererProvider.Context) :
	EntityRenderer<CompanionEntity, CompanionRenderState>(context) {

	private val modelResolver = context.blockModelResolver

	private val glassState = Blocks.GLASS.defaultBlockState()
	private val lampState = Blocks.SEA_LANTERN.defaultBlockState()
	private val lampStateAttacking = Blocks.REDSTONE_BLOCK.defaultBlockState()

	init {
		shadowRadius = 0.3f // small shadow under the floating cube
	}

	override fun createRenderState(): CompanionRenderState = CompanionRenderState()

	override fun extractRenderState(
		entity: CompanionEntity,
		state: CompanionRenderState,
		partialTick: Float,
	) {
		super.extractRenderState(entity, state, partialTick)

		val displayContext = BlockDisplayContext.create()
		modelResolver.update(state.glassBlock, glassState, displayContext)
		val lamp = if (entity.isAttacking) lampStateAttacking else lampState
		modelResolver.update(state.lampBlock, lamp, displayContext)

		val age = entity.tickCount + partialTick

		// Gentle hover bobbing.
		state.bobOffset = Mth.sin(age * BOB_SPEED) * 0.08f

		// Slow idle spin; faster while exploring or fighting (curiosity / battle mode!).
		val spinSpeed = when {
			entity.isAttacking -> SPIN_ATTACKING
			entity.isExploring -> SPIN_EXPLORING
			else -> SPIN_IDLE
		}
		state.spinDegrees = age * spinSpeed

		// Pulsing core glow; spikes with mood.
		state.coreScale = 0.11f + Mth.sin(age * PULSE_SPEED) * 0.01f + entity.moodLevel * 0.02f
	}

	override fun submit(
		state: CompanionRenderState,
		poseStack: PoseStack,
		collector: SubmitNodeCollector,
		cameraRenderState: CameraRenderState,
	) {
		super.submit(state, poseStack, collector, cameraRenderState)

		// --- Outer glass cube (full-bright: the whole companion glows like a sea lantern) ---
		poseStack.pushPose()
		poseStack.translate(0.0f, CUBE_CENTER_Y + state.bobOffset, 0.0f)
		poseStack.mulPose(Axis.YP.rotationDegrees(state.spinDegrees))
		poseStack.scale(GLASS_SCALE, GLASS_SCALE, GLASS_SCALE)
		poseStack.translate(-0.5f, -0.5f, -0.5f)
		state.glassBlock.submit(poseStack, collector, LAMP_GLOW_LIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor)
		poseStack.popPose()

		// --- Inner sea-lantern core (counter-rotating, pulsing, full-bright) ---
		poseStack.pushPose()
		poseStack.translate(0.0f, CUBE_CENTER_Y + state.bobOffset, 0.0f)
		poseStack.mulPose(Axis.YP.rotationDegrees(-state.spinDegrees * CORE_COUNTER_SPIN))
		poseStack.scale(state.coreScale, state.coreScale, state.coreScale)
		poseStack.translate(-0.5f, -0.5f, -0.5f)
		state.lampBlock.submit(poseStack, collector, LAMP_GLOW_LIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor)
		poseStack.popPose()
	}

	companion object {
		private const val GLASS_SCALE = 0.32f
		private const val CUBE_CENTER_Y = 0.16f
		private const val BOB_SPEED = 0.09
		private const val PULSE_SPEED = 0.12
		private const val SPIN_IDLE = 0.5f
		private const val SPIN_EXPLORING = 2.2f
		private const val SPIN_ATTACKING = 3.2f
		private const val CORE_COUNTER_SPIN = 0.6f
		private const val LAMP_GLOW_LIGHT = 15728880 // full-bright packed light
	}
}
