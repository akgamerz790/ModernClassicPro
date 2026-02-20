package dev.akgamerz_790.modernclassic;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ModernClassicClient implements ClientModInitializer {
	private static final double MIN_FALL_SPEED = -0.08D;
	private static final int EXTRA_SCAN_DISTANCE = 24;
	private static final float OUTLINE_RED = 0.15F;
	private static final float OUTLINE_GREEN = 0.95F;
	private static final float OUTLINE_BLUE = 1.00F;
	private static final float OUTLINE_ALPHA = 1.00F;

	@Override
	public void onInitializeClient() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(this::renderLandingBlockOutline);
	}

	private void renderLandingBlockOutline(WorldRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		ClientLevel world = minecraft.level;
		if (player == null || world == null) {
			return;
		}

		Vec3 velocity = player.getDeltaMovement();
		if (velocity.y >= MIN_FALL_SPEED || player.onGround() || player.isFallFlying()) {
			return;
		}

		Optional<BlockPos> landingPos = findLandingBlock(player, world);
		if (landingPos.isEmpty()) {
			return;
		}

		PoseStack matrices = context.matrixStack();
		MultiBufferSource.BufferSource consumers = context.consumers();
		if (matrices == null || consumers == null) {
			return;
		}

		Vec3 cameraPos = context.camera().getPosition();
		AABB targetBox = new AABB(landingPos.get()).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		LevelRenderer.renderLineBox(
			matrices,
			consumers.getBuffer(RenderType.lines()),
			targetBox,
			OUTLINE_RED,
			OUTLINE_GREEN,
			OUTLINE_BLUE,
			OUTLINE_ALPHA
		);
	}

	private Optional<BlockPos> findLandingBlock(LocalPlayer player, ClientLevel world) {
		int feetX = (int) Math.floor(player.getX());
		int feetY = (int) Math.floor(player.getY() - 0.05D);
		int feetZ = (int) Math.floor(player.getZ());

		int maxDrop = Math.max((int) Math.ceil(player.fallDistance) + EXTRA_SCAN_DISTANCE, EXTRA_SCAN_DISTANCE);
		int minY = Math.max(world.getMinY(), feetY - maxDrop);

		for (int y = feetY - 1; y >= minY; y--) {
			BlockPos pos = new BlockPos(feetX, y, feetZ);
			BlockState state = world.getBlockState(pos);
			FluidState fluidState = state.getFluidState();

			if (state.getCollisionShape(world, pos).isEmpty()) {
				continue;
			}

			// Ignore pure fluid blocks so the highlight focuses on solid clutch targets.
			if (!fluidState.isEmpty() && state.getBlock().defaultBlockState().isAir()) {
				continue;
			}

			return Optional.of(pos);
		}

		return Optional.empty();
	}
}
