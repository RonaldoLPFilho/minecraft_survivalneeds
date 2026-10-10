package com.ronaldo.survivalneeds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronaldo.survivalneeds.SurvivalNeeds;
import com.ronaldo.survivalneeds.entity.CurseSpecter;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class CurseSpecterRenderer extends MobRenderer<CurseSpecter, CurseSpecterRenderState, CurseSpecterModel> {
	private static final Identifier TEXTURE = SurvivalNeeds.id("textures/entity/curse_specter.png");
	private static final Identifier GLOW = SurvivalNeeds.id("textures/entity/curse_specter_glow.png");
	private static final float SIZE = 1.15f;

	public CurseSpecterRenderer(EntityRendererProvider.Context context) {
		super(context, new CurseSpecterModel(context.bakeLayer(CurseSpecterModel.LAYER)), 1.0f);
		// Olhos e runas brilham no escuro
		addLayer(new EyesLayer<CurseSpecterRenderState, CurseSpecterModel>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(GLOW);
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CurseSpecterRenderState state) {
		return TEXTURE;
	}

	@Override
	public CurseSpecterRenderState createRenderState() {
		return new CurseSpecterRenderState();
	}

	@Override
	protected void scale(CurseSpecterRenderState state, PoseStack poseStack) {
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	@Override
	public void extractRenderState(CurseSpecter entity, CurseSpecterRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.aggressive = entity.isAggressive();
		state.attackAnim = entity.getSwingAnimation(partialTick);
	}
}
