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
 * Modelo da Princess Brubin: proporções de jogador, com vestido em duas camadas (saia), mangas bufantes, cabelo longo,
 * coroa com rubi e manto. Textura de 128x128 em {@code textures/entity/princess_brubin.png}; as posições abaixo
 * precisam bater com o script que a gera.
 */
public class BrubinModel extends EntityModel<BrubinRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SurvivalNeeds.id("princess_brubin"), "main");

	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart hairBack;

	public BrubinModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.hairBack = root.getChild("body").getChild("hair_back");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8),
			PartPose.ZERO);
		head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.5f)),
			PartPose.ZERO);
		head.addOrReplaceChild("crown", CubeListBuilder.create()
				.texOffs(64, 0).addBox(-4.5f, -10.0f, -4.5f, 9, 2, 9)
				.texOffs(64, 12).addBox(-0.5f, -12.0f, -4.6f, 1, 2, 1)
				.texOffs(64, 12).addBox(-3.5f, -11.5f, -4.6f, 1, 2, 1)
				.texOffs(64, 12).addBox(2.5f, -11.5f, -4.6f, 1, 2, 1),
			PartPose.ZERO);

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8, 12, 4),
			PartPose.ZERO);
		body.addOrReplaceChild("hair_back", CubeListBuilder.create().texOffs(64, 16).addBox(-4.5f, -2.0f, 2.0f, 9, 12, 2),
			PartPose.ZERO);
		body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(64, 32).addBox(-5.0f, 0.0f, 4.0f, 10, 13, 1),
			PartPose.ZERO);
		// Saia em duas camadas, alargando para o chão
		body.addOrReplaceChild("skirt_upper", CubeListBuilder.create().texOffs(0, 32).addBox(-5.0f, 8.0f, -3.0f, 10, 8, 6),
			PartPose.ZERO);
		body.addOrReplaceChild("skirt_lower", CubeListBuilder.create().texOffs(0, 46).addBox(-6.0f, 16.0f, -4.0f, 12, 8, 8),
			PartPose.ZERO);

		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4),
			PartPose.offset(-5.0f, 2.0f, 0.0f));
		rightArm.addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-3.5f, -3.0f, -2.5f, 5, 5, 5),
			PartPose.ZERO);
		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4),
			PartPose.offset(5.0f, 2.0f, 0.0f));
		leftArm.addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(40, 32).mirror().addBox(-1.5f, -3.0f, -2.5f, 5, 5, 5),
			PartPose.ZERO);

		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
			PartPose.offset(-1.9f, 12.0f, 0.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
			PartPose.offset(1.9f, 12.0f, 0.0f));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(BrubinRenderState state) {
		super.setupAnim(state);
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;

		// Postura serena: mãos à frente, quase unidas, com um balanço leve de quem respira
		float breath = Mth.sin(state.ageInTicks * 0.06f) * 0.02f;
		rightArm.xRot = -0.55f + breath;
		leftArm.xRot = -0.55f - breath;
		rightArm.zRot = 0.18f;
		leftArm.zRot = -0.18f;
		hairBack.xRot = 0.04f + Mth.sin(state.ageInTicks * 0.05f) * 0.02f;
	}
}
