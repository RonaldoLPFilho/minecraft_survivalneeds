package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import com.ronaldo.survivalneeds.entity.PrincessBrubin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class BrubinRenderer extends MobRenderer<PrincessBrubin, BrubinRenderState, BrubinModel> {
	private static final Identifier TEXTURE = SurvivalNeeds.id("textures/entity/princess_brubin.png");

	public BrubinRenderer(EntityRendererProvider.Context context) {
		super(context, new BrubinModel(context.bakeLayer(BrubinModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTextureLocation(BrubinRenderState state) {
		return TEXTURE;
	}

	@Override
	public BrubinRenderState createRenderState() {
		return new BrubinRenderState();
	}
}
