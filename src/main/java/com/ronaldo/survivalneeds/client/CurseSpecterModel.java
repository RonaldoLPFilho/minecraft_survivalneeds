package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Modelo do Espectro da Maldição: corpo largo e corcunda, braços longos com ombreiras de osso e espinhos, elmo com
 * chifres e coroa de espinhos, presas, cauda, faixas rasgadas nas costas e correntes. Textura de 128x128 em
 * {@code textures/entity/curse_specter.png}, com uma cópia só dos pontos que brilham em
 * {@code curse_specter_glow.png} (olhos e runas). As posições de textura abaixo precisam bater com o script que as gera.
 * <p>
 * Regiões compartilhadas: osso em (96,16), carne em (96,56), faixa em (100,80), corrente em (96,104).
 */
public class CurseSpecterModel extends EntityModel<CurseSpecterRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SurvivalNeeds.id("curse_specter"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart tail1;
	private final ModelPart tail2;
	private final ModelPart tail3;
	private final ModelPart banners;

	public CurseSpecterModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.tail1 = body.getChild("tail1");
		this.tail2 = tail1.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.banners = body.getChild("banners");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Pernas curtas e grossas: pés no chão (y = 24), topo a 12 de altura
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(66, 76).addBox(-3.0f, 0.0f, -3.0f, 6, 12, 6),
			PartPose.offset(-5.0f, 12.0f, 0.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(66, 76).mirror().addBox(-3.0f, 0.0f, -3.0f, 6, 12, 6),
			PartPose.offset(5.0f, 12.0f, 0.0f));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 40).addBox(-8.0f, -14.0f, -5.0f, 16, 14, 10),
			PartPose.offset(0.0f, 12.0f, 0.0f));

		// Espinhos nas costas
		body.addOrReplaceChild("spikes", CubeListBuilder.create()
				.texOffs(60, 40).addBox(-6.0f, -17.0f, 3.0f, 2, 6, 2)
				.texOffs(60, 40).addBox(-1.0f, -20.0f, 3.0f, 2, 8, 2)
				.texOffs(60, 40).addBox(4.0f, -17.0f, 3.0f, 2, 6, 2),
			PartPose.ZERO);
		// Pano da cintura e correntes nas laterais
		body.addOrReplaceChild("loincloth", CubeListBuilder.create().texOffs(0, 64).addBox(-4.5f, 0.0f, -5.6f, 9, 11, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("chains", CubeListBuilder.create()
				.texOffs(96, 104).addBox(-8.6f, 0.0f, -4.5f, 1, 9, 1)
				.texOffs(96, 104).addBox(7.6f, 0.0f, -4.5f, 1, 9, 1),
			PartPose.ZERO);
		// Faixas rasgadas nas costas
		body.addOrReplaceChild("banners", CubeListBuilder.create()
				.texOffs(100, 80).addBox(-7.0f, -13.0f, 5.1f, 4, 16, 1)
				.texOffs(100, 80).addBox(3.0f, -13.0f, 5.1f, 4, 16, 1),
			PartPose.ZERO);

		// Cauda de três segmentos
		PartDefinition tail1 = body.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(96, 56).addBox(-2.5f, -2.5f, 0.0f, 5, 5, 8),
			PartPose.offsetAndRotation(0.0f, -2.0f, 4.5f, -0.3f, 0.0f, 0.0f));
		PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(96, 56).addBox(-2.0f, -2.0f, 0.0f, 4, 4, 8),
			PartPose.offset(0.0f, 0.0f, 8.0f));
		tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
				.texOffs(96, 56).addBox(-1.5f, -1.5f, 0.0f, 3, 3, 6)
				.texOffs(96, 16).addBox(-0.5f, -0.5f, 6.0f, 1, 1, 4),
			PartPose.offset(0.0f, 0.0f, 8.0f));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -9.0f, -5.0f, 10, 9, 10),
			PartPose.offset(0.0f, -13.0f, -1.0f));
		head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(0, 20).addBox(-5.0f, -9.0f, -5.0f, 10, 9, 10, new CubeDeformation(0.8f)),
			PartPose.ZERO);
		// Chifres grandes (base e ponta) e coroa de espinhos
		head.addOrReplaceChild("horns", CubeListBuilder.create()
				.texOffs(96, 16).addBox(-8.5f, -9.0f, -1.0f, 3, 3, 2)
				.texOffs(96, 16).addBox(-10.5f, -15.0f, -1.0f, 2, 7, 2)
				.texOffs(96, 16).addBox(5.5f, -9.0f, -1.0f, 3, 3, 2)
				.texOffs(96, 16).addBox(8.5f, -15.0f, -1.0f, 2, 7, 2)
				.texOffs(96, 16).addBox(-0.5f, -13.0f, -4.0f, 1, 4, 1)
				.texOffs(96, 16).addBox(-0.5f, -15.0f, -1.0f, 1, 6, 1)
				.texOffs(96, 16).addBox(-0.5f, -13.0f, 2.0f, 1, 4, 1),
			PartPose.ZERO);
		// Presas
		head.addOrReplaceChild("tusks", CubeListBuilder.create()
				.texOffs(96, 16).addBox(-3.0f, -4.0f, -5.8f, 1, 3, 1)
				.texOffs(96, 16).addBox(2.0f, -4.0f, -5.8f, 1, 3, 1),
			PartPose.ZERO);

		// Braços longos e pesados, com ombreira, espinhos e garras
		PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 76).addBox(-3.0f, 0.0f, -3.0f, 6, 18, 6),
			PartPose.offset(-11.0f, -12.0f, 0.0f));
		rightArm.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(28, 76).addBox(-4.5f, -2.0f, -4.5f, 9, 5, 9),
			PartPose.ZERO);
		rightArm.addOrReplaceChild("shoulder_spikes", CubeListBuilder.create()
				.texOffs(96, 16).addBox(-3.5f, -8.0f, -1.0f, 2, 6, 2)
				.texOffs(96, 16).addBox(1.5f, -7.0f, -1.0f, 2, 5, 2),
			PartPose.ZERO);
		rightArm.addOrReplaceChild("claws", CubeListBuilder.create().texOffs(28, 90).addBox(-3.0f, 18.0f, -3.0f, 6, 4, 6),
			PartPose.ZERO);
		PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 76).mirror().addBox(-3.0f, 0.0f, -3.0f, 6, 18, 6),
			PartPose.offset(11.0f, -12.0f, 0.0f));
		leftArm.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(28, 76).mirror().addBox(-4.5f, -2.0f, -4.5f, 9, 5, 9),
			PartPose.ZERO);
		leftArm.addOrReplaceChild("shoulder_spikes", CubeListBuilder.create()
				.texOffs(96, 16).addBox(-3.5f, -7.0f, -1.0f, 2, 5, 2)
				.texOffs(96, 16).addBox(1.5f, -8.0f, -1.0f, 2, 6, 2),
			PartPose.ZERO);
		leftArm.addOrReplaceChild("claws", CubeListBuilder.create().texOffs(28, 90).mirror().addBox(-3.0f, 18.0f, -3.0f, 6, 4, 6),
			PartPose.ZERO);

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(CurseSpecterRenderState state) {
		super.setupAnim(state);
		float swing = state.walkAnimationPos * 0.6662f;
		float amount = Math.min(1.0f, state.walkAnimationSpeed * 1.4f);
		float age = state.ageInTicks;

		body.xRot = 0.25f; // corcunda
		head.xRot = state.xRot * Mth.DEG_TO_RAD - 0.25f;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;

		rightLeg.xRot = Mth.cos(swing) * 1.3f * amount;
		leftLeg.xRot = Mth.cos(swing + Mth.PI) * 1.3f * amount;

		// Braços balançam; agressivo, ficam à frente, e o golpe de garra os faz descer
		float armBase = state.aggressive ? -1.1f : 0.0f;
		rightArm.xRot = armBase + Mth.cos(swing + Mth.PI) * 0.9f * amount + Mth.sin(age * 0.07f) * 0.04f;
		leftArm.xRot = armBase + Mth.cos(swing) * 0.9f * amount - Mth.sin(age * 0.07f) * 0.04f;
		rightArm.zRot = 0.08f;
		leftArm.zRot = -0.08f;
		if (state.attackAnim > 0.0f) {
			float strike = Mth.sin(state.attackAnim * Mth.PI) * 1.6f;
			rightArm.xRot -= strike;
			leftArm.xRot -= strike * 0.6f;
		}

		// Cauda e faixas ondulam sempre, mais forte ao andar
		float sway = 0.18f + amount * 0.25f;
		tail1.yRot = Mth.sin(age * 0.09f) * sway;
		tail2.yRot = Mth.sin(age * 0.09f - 0.9f) * sway * 1.3f;
		tail3.yRot = Mth.sin(age * 0.09f - 1.8f) * sway * 1.6f;
		banners.xRot = 0.08f + Mth.sin(age * 0.11f) * 0.05f + amount * 0.12f;
	}
}
