package legend.game.combat.deff;

import legend.core.gte.MV;
import legend.core.memory.Method;
import legend.game.types.Model124;
import legend.game.unpacker.FileData;
import org.joml.Math;
import org.joml.Vector3f;

import static legend.game.EngineStates.currentEngineState_8004dd04;

public class LmbType0 extends Lmb {
  public final PartInfo0c[] partAnimations_08;

  public LmbType0(final FileData data) {
    super(data);

    this.partAnimations_08 = new PartInfo0c[this.objectCount_04];

    for(int i = 0; i < this.objectCount_04; i++) {
      this.partAnimations_08[i] = new PartInfo0c(data.slice(0x8 + i * 0xc, 0xc), data);
    }
  }

  @Override
  public void loadIntoModel(final Model124 model) {
    model.anim_08 = model.new LmbAnim(this);
    model.partCount_98 = this.objectCount_04;
    model.totalFrames_9a = this.partAnimations_08[0].keyframeCount_04 * 2;
    model.animationState_9c = 1;
    model.remainingFrames_9e = this.partAnimations_08[0].keyframeCount_04 * 2;
    model.subFrameIndex = 0;
  }

  @Override
  @Method(0x800dd638L)
  public void apply(final Model124 model, final int animationTicks) {
    if(model.animationState_9c == 2) {
      return;
    }

    //LAB_800dd680
    final int count = Math.min(model.modelParts_00.length, model.partCount_98);

    // animationTicks are display ticks (see EngineState#animationDisplayTicks) - tickMultiplier of them per logic tick.
    // Retail advanced 2 logic ticks per keyframe (1 with interpolation disabled) and blended halfway on odd ticks,
    // this blends by however far through the keyframe we are so it's smooth at any display rate.
    final int tickMultiplier = currentEngineState_8004dd04 != null ? Math.max(1, currentEngineState_8004dd04.tickMultiplier()) : 1;
    final int keyframeCount = Math.max(1, model.totalFrames_9a / 2);
    final int logicFramesPerKeyframe = model.disableInterpolation_a2 ? 1 : 2;
    final int framesPerKeyframe = logicFramesPerKeyframe * tickMultiplier;

    //LAB_800dd69c
    //LAB_800dd6dc
    final int frame = java.lang.Math.floorMod(animationTicks, keyframeCount * framesPerKeyframe);
    final int a0_0 = frame / framesPerKeyframe;
    final int subFrame = model.disableInterpolation_a2 ? 0 : frame % framesPerKeyframe;
    // Counted in logic ticks so it reaches 0 on the last logic tick no matter which display frames get rendered
    final int remainingFrames = keyframeCount * logicFramesPerKeyframe - frame / tickMultiplier;

    //LAB_800dd700
    model.remainingFrames_9e = remainingFrames - 1;
    model.subFrameIndex = 0;

    //LAB_800dd720
    for(int i = 0; i < count; i++) {
      final LmbTransforms14 transforms = this.partAnimations_08[i].keyframes_08[a0_0];
      final MV matrix = model.modelParts_00[i].coord2_04.coord;

      final Vector3f trans = new Vector3f();
      trans.set(transforms.trans_06);

      if(subFrame != 0) { // Interpolation frame
        final LmbTransforms14 nextFrame;
        if(a0_0 + 1 >= this.partAnimations_08[i].keyframes_08.length) {
          nextFrame = this.partAnimations_08[i].keyframes_08[0]; // Wrap around to frame 0
        } else {
          //LAB_800dd7cc
          nextFrame = this.partAnimations_08[i].keyframes_08[a0_0 + 1];
        }

        //LAB_800dd7d0
        trans.lerp(nextFrame.trans_06, subFrame / (float)framesPerKeyframe);
      }

      //LAB_800dd818
      matrix.rotationZYX(transforms.rot_0c);
      matrix.transfer.set(trans);
      matrix.scaleLocal(transforms.scale_00);
    }

    //LAB_800dd84c
    if(model.remainingFrames_9e == 0) {
      model.animationState_9c = 0;
    } else {
      //LAB_800dd864
      model.animationState_9c = 1;
    }
  }

  public static class PartInfo0c {
    public final short partFlagIndex_00;

    public final short keyframeCount_04;

    public final LmbTransforms14[] keyframes_08;

    public PartInfo0c(final FileData data, final FileData baseData) {
      this.partFlagIndex_00 = data.readShort(0x0);

      this.keyframeCount_04 = data.readShort(0x4);

      final int transformsOffset = data.readInt(0x8);
      this.keyframes_08 = new LmbTransforms14[this.keyframeCount_04];
      for(int i = 0; i < this.keyframes_08.length; i++) {
        this.keyframes_08[i] = new LmbTransforms14(baseData.slice(transformsOffset + i * 0x14, 0x14));
      }
    }
  }
}
