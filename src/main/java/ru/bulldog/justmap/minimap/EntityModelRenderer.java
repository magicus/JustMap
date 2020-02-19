package ru.bulldog.justmap.minimap;

import com.mojang.blaze3d.systems.RenderSystem;

import ru.bulldog.justmap.config.Params;
import ru.bulldog.justmap.util.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.LivingEntity;

public class EntityModelRenderer {	

	private static MinecraftClient minecraftClient = MinecraftClient.getInstance();
	private static EntityRenderDispatcher renderDispatcher = minecraftClient.getEntityRenderManager();
	private static VertexConsumerProvider.Immediate consumerProvider = minecraftClient.getBufferBuilders().getEntityVertexConsumers();
	
	public static void renderModel(Entity entity, int x, int y) {
		
		LivingEntity livingEntity = (LivingEntity) entity;
		
		float headYaw = livingEntity.headYaw;
		float bodyYaw = livingEntity.bodyYaw;
		float prevHeadYaw = livingEntity.prevHeadYaw;
		float prevBodyYaw = livingEntity.prevBodyYaw;
		float pitch = livingEntity.pitch;
		float prevPitch = livingEntity.prevPitch;
		
		setPitchAndYaw(livingEntity);
		
		float scale = getScale(livingEntity);
		
		MatrixStack matrixStack = new MatrixStack();
		
		int modelSize = Params.entityModelSize;
		
		matrixStack.push();
		matrixStack.translate(x, y, 0);
		matrixStack.translate(modelSize / 4, modelSize / 2, 0);
		matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(180.0F));
		matrixStack.push();
		matrixStack.scale(scale, scale, scale);
		
		DiffuseLighting.enable();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		renderDispatcher.getRenderer(livingEntity).render(livingEntity, 0.0F, 0.0F, matrixStack, consumerProvider, Colors.LIGHT);
		DiffuseLighting.disable();
		
		matrixStack.pop();
		matrixStack.pop();
		
		livingEntity.pitch = pitch;
		livingEntity.headYaw = headYaw;
		livingEntity.bodyYaw = bodyYaw;
		livingEntity.prevPitch = prevPitch;
		livingEntity.prevHeadYaw = prevHeadYaw;
		livingEntity.prevBodyYaw = prevBodyYaw;
	}
	
	private static float getScale(LivingEntity livingEntity) {
		int modelSize = Params.entityModelSize;
		
		float scaleX = modelSize / Math.max(livingEntity.getWidth(), 0.5F);
		float scaleY = modelSize / Math.max(livingEntity.getHeight(), 0.5F);
		
		float scale = Math.max(Math.min(scaleX, scaleY), modelSize);
		
		if (livingEntity instanceof GhastEntity || livingEntity instanceof EnderDragonEntity) {
			scale = modelSize / 3;
		}		
		if (livingEntity.isSleeping()) {
			scale = modelSize;
		}
		
		return scale;
	}
	
	private static void setPitchAndYaw(LivingEntity livingEntity) {
		livingEntity.pitch = 0.0F;
		livingEntity.prevPitch = 0.0F;
		
		switch(livingEntity.getMovementDirection()) {
			case NORTH:
				livingEntity.headYaw = 0.0F;
				livingEntity.bodyYaw = 0.0F;
				livingEntity.prevHeadYaw = 0.0F;
				livingEntity.prevBodyYaw = 0.0F;
				break;
			case WEST:
				livingEntity.headYaw = 135.0F;
				livingEntity.bodyYaw = 135.0F;
				livingEntity.prevHeadYaw = 135.0F;
				livingEntity.prevBodyYaw = 135.0F;
				break;
			case EAST:
				livingEntity.headYaw = 225.0F;
				livingEntity.bodyYaw = 225.0F;
				livingEntity.prevHeadYaw = 225.0F;
				livingEntity.prevBodyYaw = 225.0F;
				break;
			default:
				livingEntity.headYaw = 180.0F;
				livingEntity.bodyYaw = 180.0F;
				livingEntity.prevHeadYaw = 180.0F;
				livingEntity.prevBodyYaw = 180.0F;
			break;
		}
	}
}
