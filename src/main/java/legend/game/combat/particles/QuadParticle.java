package legend.game.combat.particles;

import legend.core.MathHelper;
import legend.core.renderer.QueuedModelStandard;
import legend.core.gpu.Bpp;
import legend.core.gte.MV;
import legend.core.memory.Method;
import legend.core.renderer.QuadBuilder;
import legend.game.combat.deff.DeffPart;
import legend.game.combat.effects.EffectManagerData6c;
import legend.game.combat.effects.EffectManagerParams;
import legend.game.combat.effects.SpriteMetrics08;
import legend.game.combat.types.BattleObject;
import legend.game.scripting.ScriptState;
import legend.core.renderer.Translucency;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Arrays;

import static legend.core.GameEngine.CONFIG;
import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.RENDERER;
import static legend.core.GameEngine.SCRIPTS;
import static legend.game.Graphics.GetClut;
import static legend.game.Graphics.worldToScreenMatrix_800c3548;
import static legend.game.combat.Battle.ZERO;
import static legend.game.combat.Battle.deffManager_800c693c;
import static legend.game.combat.SEffe.FUN_800cfc20;
import static legend.game.combat.particles.ParticleManager.particleSubCounts_800fb794;
import static legend.game.modding.coremod.CoreMod.REDUCE_MOTION_FLASHING_CONFIG;

public class QuadParticle extends ParticleEffectData98 {
  /** ushort */
  private int u_58;
  /** ushort */
  private int v_5a;
  /** ushort */
  private int clut_5c;

  public QuadParticle(final ParticleManager manager, @Nullable final BattleObject parentBobj, final ParticleEffectData98Inner24 effectInner, final int type, final int count) {
    super(manager, parentBobj, effectInner, type, count);
  }

  @Override
  protected void init(final int flags) {
    this.countParticleSub_54 = 0;

    if((this.effectInner_08.particleInnerStuff_1c & 0x6000_0000) != 0) {
      final int index = (this.effectInner_08.particleInnerStuff_1c & 0x6000_0000) >>> 28;
      this.countParticleSub_54 = particleSubCounts_800fb794[index];

      //LAB_80101f2c
      for(int i = 0; i < this.countParticleInstance_50; i++) {
        final ParticleEffectInstance94 inst = this.particleArray_68[i];
        inst.particleInstanceSubArray_80 = new ParticleEffectInstance94Sub10[this.countParticleSub_54];
        Arrays.setAll(inst.particleInstanceSubArray_80, n -> new ParticleEffectInstance94Sub10());
      }
    }

    //LAB_80101f70
    if((flags & 0xf_ff00) == 0xf_ff00) {
      final SpriteMetrics08 metrics = deffManager_800c693c.spriteMetrics_39c[flags & 0xff];
      this.u_58 = metrics.u_00;
      this.v_5a = metrics.v_02;
      this.w_5e = metrics.w_04;
      this.h_5f = metrics.h_05;
      this.clut_5c = metrics.clut_06;
    } else {
      //LAB_80101fec
      final DeffPart.SpriteType spriteType = (DeffPart.SpriteType)deffManager_800c693c.getDeffPart(flags | 0x400_0000);
      final DeffPart.SpriteMetrics deffMetrics = spriteType.metrics_08;
      this.u_58 = deffMetrics.u_00;
      this.v_5a = deffMetrics.v_02;
      this.w_5e = deffMetrics.w_04 * 4;
      this.h_5f = deffMetrics.h_06;
      this.clut_5c = GetClut(deffMetrics.clutX_08, deffMetrics.clutY_0a);
    }
  }

  @Override
  protected void reinitInstanceType(final EffectManagerData6c<EffectManagerParams.ParticleType> manager, final ParticleEffectInstance94 inst) {
    //LAB_801011a0
    //LAB_801011d8
    for(int i = 0; i < this.countParticleSub_54; i++) {
      this.FUN_800fca78(manager, this, inst, inst.managerRotation_68, inst.particlePosition_50, inst.scaleHorizontal_06, inst.scaleVertical_08, inst.angle_0e, inst.particlePositionCopy2_48, inst.particlePosition_50, inst.particleInstanceSubArray_80[i].transforms);

      if((manager.params_10.flags_24 & 0x2) != 0) {
        inst.particlePositionCopy2_48.set(inst.particlePosition_50);
      }
    }

    if(this.countParticleSub_54 != 0) {
      inst.trailTickTransforms.set(inst.particleInstanceSubArray_80[0].transforms);
    }
  }

  @Override
  protected void tickInstance(final EffectManagerData6c<EffectManagerParams.ParticleType> manager, final ParticleEffectInstance94 inst) {
    //LAB_800fe1bc
    // The trail is in screen space so it's shifted by the renderer, see render
    inst.trailShiftPending = true;

    //LAB_800fe1fc
    inst.beforeRender(manager);
    inst.tickAttributes(manager, inst.colour);

    inst.halfBright = (this.effectInner_08.particleInnerStuff_1c & 0x1000_0000) != 0 && (inst.flags_90 & 0x8) != 0;

    //LAB_800fe28c
    inst.flags_90 = inst.flags_90 & 0xffff_fff7 | (~(inst.flags_90 >>> 3) & 0x1) << 3;

    if((manager.params_10.flags_00 & 0x400_0000) == 0) {
      inst.managerRotation_68.y = this.cameraFacingAngle(inst.particlePosition_50);
    }

    // Direction of travel (originally tracked by FUN_800fca78 when drawing)
    if((manager.params_10.flags_24 & 0x2) != 0) {
      inst.headingFrom.set(inst.particlePositionCopy2_48);
      inst.particlePositionCopy2_48.set(inst.particlePosition_50);
    }
  }

  private float cameraFacingAngle(final Vector3f position) {
    // This is super bugged in retail and passes garbage as the last 3 params to both methods.
    // Hopefully this is fine with them all zeroed. This is used for the Glare's bewitching attack.
    return MathHelper.atan2(
      this.manager.camera.refpointRawComponent(0, null, ZERO) - position.x,
      this.manager.camera.refpointRawComponent(2, null, ZERO) - position.z
    ) + MathHelper.TWO_PI / 4.0f;
  }

  @Override
  public void render(final ScriptState<EffectManagerData6c<EffectManagerParams.ParticleType>> state) {
    final EffectManagerData6c<EffectManagerParams.ParticleType> manager = state.innerStruct_00;
    final float alpha = SCRIPTS.getInterpAlpha();

    if(this.obj == null) {
      final QuadBuilder builder = new QuadBuilder("Particle")
        .bpp(Bpp.BITS_4)
        .clut((this.clut_5c & 0b111111) * 16, this.clut_5c >>> 6)
        .vramPos(this.u_58 & 0x3c0, this.v_5a < 256 ? 0 : 256)
        .uv((this.u_58 & 0x3f) * 4, this.v_5a)
        .uvSize(this.w_5e, this.h_5f)
        .pos(-0.5f, -0.5f, 0.0f)
        .posSize(1.0f, 1.0f);

      if((manager.params_10.flags_00 & 0x1 << 30) != 0) {
        builder.translucency(Translucency.of(manager.params_10.flags_00 >>> 28 & 0b11));
      }

      this.obj = builder.build();
    }

    final Vector3f colour = new Vector3f();
    final Vector3f colourMod = new Vector3f();
    final Vector3f colourStep = new Vector3f();
    final Vector3f rotation = new Vector3f();

    //LAB_800fe180
    for(int i = 0; i < this.countParticleInstance_50; i++) {
      final ParticleEffectInstance94 inst = this.particleArray_68[i];

      // Simulation happens on logic ticks, see tickInstance
      if(inst.visible) {
        inst.interpolateVisuals(alpha);

        //LAB_800fe1bc
        // Shift the trail along on the first frame after a logic tick. The newest segment is what was drawn on the
        // last logic tick frame (not the last display frame) so the trail spacing matches the original.
        final boolean shiftTrail = inst.trailShiftPending;
        if(shiftTrail) {
          inst.trailShiftPending = false;

          for(int j = this.countParticleSub_54 - 1; j > 1; j--) {
            inst.particleInstanceSubArray_80[j].copy(inst.particleInstanceSubArray_80[j - 1]);
          }

          if(this.countParticleSub_54 > 1) {
            inst.particleInstanceSubArray_80[1].transforms.set(inst.trailTickTransforms);
          }
        }

        colour.set(inst.renderColour);

        if(!inst.halfBright) {
          //LAB_800fe280
          colourMod.set(0, 0, 0);
        } else {
          colourMod.set(colour).negate().div(2.0f);
        }

        rotation.set(inst.managerRotation_68);
        if((manager.params_10.flags_00 & 0x400_0000) == 0) {
          rotation.y = this.cameraFacingAngle(inst.renderPosition);
        }

        //LAB_800fe300
        MathHelper.clamp(colour.add(colourMod), 0.0f, 1.0f);

        final float instZ = this.FUN_800fca78(manager, this, inst, rotation, inst.renderPosition, inst.renderScaleHorizontal, inst.renderScaleVertical, inst.renderAngle, inst.headingFrom, inst.particlePosition_50, this.transforms) / 4.0f;
        float effectZ = manager.params_10.z_22;
        if(effectZ + instZ >= 160) {
          if(effectZ + instZ >= 4094) {
            effectZ = 4094 - instZ;
          }

          //LAB_800fe548
          this.transforms.transfer.z = (instZ + effectZ) * 4.0f;
          RENDERER.queueOrthoModel(this.obj, this.transforms, QueuedModelStandard.class)
            .screenspaceOffset(GPU.getOffsetX(), GPU.getOffsetY())
            .colour(colour);
        }

        //LAB_800fe564
        if((this.effectInner_08.particleInnerStuff_1c & 0x6000_0000) != 0) {
          ParticleEffectInstance94Sub10 particleSub = inst.particleInstanceSubArray_80[0];
          particleSub.transforms.set(this.transforms);
          colourStep.set(colour).div(this.countParticleSub_54);

          if(shiftTrail) {
            inst.trailTickTransforms.set(this.transforms);
          }

          final int count = Math.min(-inst.framesUntilRender_04, this.countParticleSub_54);

          //LAB_800fe61c
          //LAB_800fe628
          for(int k = 0; k < count; k++) {
            effectZ = manager.params_10.z_22;
            if(effectZ + instZ >= 160) {
              if(effectZ + instZ >= 4094) {
                effectZ = 4094 - instZ;
              }

              particleSub = inst.particleInstanceSubArray_80[k];

              //LAB_800fe644
              //LAB_800fe78c
              particleSub.transforms.transfer.z = instZ + effectZ;
              final QueuedModelStandard model = RENDERER.queueOrthoModel(this.obj, particleSub.transforms, QueuedModelStandard.class)
                .screenspaceOffset(GPU.getOffsetX(), GPU.getOffsetY())
                .colour(colour);

              if(CONFIG.getConfig(REDUCE_MOTION_FLASHING_CONFIG.get())) {
                model.colour(colour.x / 2.0f, colour.y / 2.0f, colour.z / 2.0f);
              }
            }

            colour.sub(colourStep);
          }
        }
      }
      //LAB_800fe7b8
    }

    //LAB_800fe7ec
    // Effect acceleration is scaled on logic ticks, see ParticleEffectData98#tick
    //LAB_800fe848
  }

  @Method(0x800fc8f8L)
  private void FUN_800fc8f8(@Nullable Vector3f in, @Nullable final Vector3f out) {
    if(in == null) {
      in = new Vector3f();
    }

    //LAB_800fc920
    final MV wsTransposed = new MV();
    worldToScreenMatrix_800c3548.transpose(wsTransposed);
    final Vector3f wsNegated = new Vector3f(wsTransposed.transfer).negate();
    wsNegated.mul(wsTransposed, in);

    if(out != null) {
      wsNegated.set(wsTransposed.transfer).negate();
      wsNegated.z += 0x1000;

      final Vector3f sp0x10 = new Vector3f();
      wsNegated.mul(wsTransposed, sp0x10);
      sp0x10.sub(in);
      final float angle = MathHelper.atan2(sp0x10.x, sp0x10.z);
      out.y = angle;

      //LAB_800fca44
      //TODO is this right?
      final float sin = MathHelper.sin(-angle);
      final float cos = MathHelper.cosFromSin(sin, -angle);
      out.x = MathHelper.atan2(-sp0x10.y, cos * sp0x10.z - sin * sp0x10.x);
      out.z = 0.0f;
    }
    //LAB_800fca5c
  }

  /**
   * Returns Z. Takes the visual values explicitly so it can draw interpolated values. The direction of travel is
   * calculated from headingFrom to headingTo (originally the last drawn position and translation, with the last
   * drawn position being updated here - that's now handled by the callers).
   */
  @Method(0x800fca78L)
  private float FUN_800fca78(final EffectManagerData6c<EffectManagerParams.ParticleType> manager, final ParticleEffectData98 particle, final ParticleEffectInstance94 instance, final Vector3f managerRotation, final Vector3f translation, final float scaleHorizontal, final float scaleVertical, final float particleAngle, final Vector3f headingFrom, final Vector3f headingTo, final MV transforms) {
    final Vector2f ref = new Vector2f();
    final float z = FUN_800cfc20(managerRotation, instance.managerTranslation_2c, translation, ref);
    if(z >= 40) {
      final float zScale = (float)0x5000 / z;
      final float horizontalScale = zScale * (manager.params_10.scale_16.x + scaleHorizontal);
      final float verticalScale = zScale * (manager.params_10.scale_16.y + scaleVertical);

      final float angle;
      if((manager.params_10.flags_24 & 0x2) != 0) {
        final Vector3f sp0x38 = new Vector3f();
        this.FUN_800fc8f8(null, sp0x38);

        //LAB_800fcb90
        //LAB_800fcbd4
        final float sp18 = (headingFrom.z - headingTo.z) * -org.joml.Math.abs(MathHelper.sin(sp0x38.y - manager.params_10.rot_10.y)) - (headingTo.x - headingFrom.x) * -Math.abs(MathHelper.cos(sp0x38.y + manager.params_10.rot_10.y));
        final float sp1c = headingTo.y - headingFrom.y;
        angle = -MathHelper.atan2(sp1c, sp18) + MathHelper.TWO_PI / 4.0f;
      } else {
        angle = particleAngle + manager.params_10.rot_10.x - MathHelper.TWO_PI / 1.6f;
      }

      transforms.transfer.x = ref.x;
      transforms.transfer.y = ref.y;
      transforms.scaling(horizontalScale * particle.w_5e, verticalScale * particle.h_5f, 1.0f).rotateLocalZ(angle);
    }

    //LAB_800fcde0
    return z;
  }
}
