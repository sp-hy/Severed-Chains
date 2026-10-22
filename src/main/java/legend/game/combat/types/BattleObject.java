package legend.game.combat.types;

import legend.core.MathHelper;
import legend.core.gte.MV;
import legend.core.memory.Method;
import legend.game.combat.Battle;
import legend.game.scripting.ScriptedObject;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static legend.core.GameEngine.SCRIPTS;

public abstract class BattleObject implements ScriptedObject {
  public static String EM__ = "EM  ";
  public static String BOBJ = "BOBJ";

  public final Battle battle;
  public final String magic_00;

  /** Skip interpolation for teleports / first placement (squared distance). */
  private static final float INTERP_TELEPORT_DISTANCE_SQUARED = 4096.0f * 4096.0f;

  private final Vector3f interpStartPos = new Vector3f();
  private final Vector3f interpEndPos = new Vector3f();
  private final Vector3f interpStartRot = new Vector3f();
  private final Vector3f interpEndRot = new Vector3f();
  private final Vector3f interpStartScale = new Vector3f();
  private final Vector3f interpEndScale = new Vector3f();
  private boolean interpInitialized;

  protected BattleObject(final Battle battle, final String magic) {
    this.battle = battle;
    this.magic_00 = magic;
  }

  /** Lerp root transform toward the latest 20 Hz pose. Call from the renderer, restore afterwards. */
  public void applyVisualInterpolation() {
    if(this.battle.tickMultiplier() <= 1) {
      return;
    }

    final Vector3f pos = this.getPosition();
    final Vector3f rot = this.getRotation();
    final Vector3f scale = this.getScale();

    if(!this.interpInitialized) {
      this.interpStartPos.set(pos);
      this.interpEndPos.set(pos);
      this.interpStartRot.set(rot);
      this.interpEndRot.set(rot);
      this.interpStartScale.set(scale);
      this.interpEndScale.set(scale);
      this.interpInitialized = true;
    }

    if(SCRIPTS.willTick()) {
      this.interpStartPos.set(this.interpEndPos);
      this.interpStartRot.set(this.interpEndRot);
      this.interpStartScale.set(this.interpEndScale);
      this.interpEndPos.set(pos);
      this.interpEndRot.set(rot);
      this.interpEndScale.set(scale);

      if(this.interpStartPos.distanceSquared(this.interpEndPos) > INTERP_TELEPORT_DISTANCE_SQUARED) {
        this.interpStartPos.set(this.interpEndPos);
        this.interpStartRot.set(this.interpEndRot);
        this.interpStartScale.set(this.interpEndScale);
      }
    }

    final float alpha = SCRIPTS.getInterpAlpha();
    pos.set(this.interpStartPos).lerp(this.interpEndPos, alpha);
    rot.x = this.interpStartRot.x + MathHelper.closestAngle(this.interpEndRot.x, this.interpStartRot.x) * alpha;
    rot.y = this.interpStartRot.y + MathHelper.closestAngle(this.interpEndRot.y, this.interpStartRot.y) * alpha;
    rot.z = this.interpStartRot.z + MathHelper.closestAngle(this.interpEndRot.z, this.interpStartRot.z) * alpha;
    scale.set(this.interpStartScale).lerp(this.interpEndScale, alpha);
  }

  public void restoreLogicTransform() {
    if(!this.interpInitialized) {
      return;
    }

    this.getPosition().set(this.interpEndPos);
    this.getRotation().set(this.interpEndRot);
    this.getScale().set(this.interpEndScale);
  }

  @Override
  public abstract Vector3f getPosition();
  public abstract Vector3f getRotation();
  public abstract Vector3f getScale();
  @Override
  public abstract Vector3i getColour();

  /** Translates a position relative to this BattleObject's local coordinate system */
  @Method(0x801105ccL)
  public void getRelativePosition(final Vector3f in, final Vector3f out) {
    final MV parentRotationMatrix = new MV();
    parentRotationMatrix.rotationXYZ(this.getRotation());
    in.mul(parentRotationMatrix, out).add(this.getPosition());
  }

  /** Translates a position relative to this BattleObject's local coordinate system */
  @Method(0x801105ccL)
  public void getRelativePosition(final Vector3f inOut) {
    this.getRelativePosition(inOut, inOut);
  }

  /** Returns the relative position from a parent BattleObject's local coordinate system */
  public Vector3f getRelativePositionTo(final BattleObject child, final Vector3f out) {
    return getRelativePositionBetween(child, this, out);
  }

  /** Returns the relative position from a parent BattleObject's local coordinate system */
  @Method(0x8011035cL)
  public Vector3f getRelativePositionFrom(final BattleObject parent, final Vector3f out) {
    return getRelativePositionBetween(this, parent, out);
  }

  private static Vector3f getRelativePositionBetween(final BattleObject child, final BattleObject parent, final Vector3f out) {
    final Vector3f deltaPosition = new Vector3f().set(child.getPosition()).sub(parent.getPosition());
    final MV parentRotationMatrix = new MV();
    parentRotationMatrix.rotationXYZ(parent.getRotation());

    parentRotationMatrix.transpose();
    deltaPosition.mul(parentRotationMatrix, out);
    return out;
  }

  @Method(0x80112474L)
  public Vector3f getRotationDifference(final BattleObject other, final Vector3f outRotation) {
    MathHelper.floorMod(outRotation.set(this.getRotation()).sub(other.getRotation()), MathHelper.TWO_PI);
    return outRotation;
  }

  @Method(0x801137f8L)
  public Vector3f getScaleDifference(final BattleObject other, final Vector3f out) {
    return out.set(this.getScale()).sub(other.getScale());
  }

  /** NOTE: fixed a retail bug using scriptIndex1 instead of scriptIndex2 */
  @Method(0x80113624L)
  public Vector3f getScaleRatio(final BattleObject other, final Vector3f outScale) {
    final Vector3f scale = this.getScale();
    final Vector3f otherScale = other.getScale();

    if(otherScale.x == 0.0f) {
      otherScale.x = Float.MIN_NORMAL;
    }

    if(otherScale.y == 0.0f) {
      otherScale.y = Float.MIN_NORMAL;
    }

    if(otherScale.z == 0.0f) {
      otherScale.z = Float.MIN_NORMAL;
    }

    outScale.x = scale.x / otherScale.x;
    outScale.y = scale.y / otherScale.y;
    outScale.z = scale.z / otherScale.z;

    return outScale;
  }

  @Method(0x8011441cL)
  public Vector3i getColourDifference(final BattleObject other, final Vector3i outColour) {
    return outColour.set(this.getColour()).sub(other.getColour());
  }
}
