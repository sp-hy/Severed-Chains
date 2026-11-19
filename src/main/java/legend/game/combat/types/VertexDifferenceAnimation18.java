package legend.game.combat.types;

import legend.core.memory.Method;
import legend.core.renderer.Mesh;
import legend.core.renderer.MeshObj;
import legend.game.scripting.ScriptState;
import legend.game.scripting.ScriptedObject;
import legend.game.tmd.TmdObjLoader;
import legend.game.tmd.TmdObjTable1c;
import org.joml.Vector3f;

import static legend.core.GameEngine.SCRIPTS;

public class VertexDifferenceAnimation18 implements ScriptedObject {
  public int ticksRemaining_00;
  public float embiggener_04;
  public int vertexCount_08;
  public Vector3f[] sourceVertices_0c;
  public Vector3f[] step_10;
  public Vector3f[] current_14;
  /** Not retail - {@link #current_14} on the previous logic tick so the renderer can interpolate */
  public Vector3f[] previous;

  public TmdObjTable1c tmd;

  @Method(0x80109b44L)
  public static void applyVertexDifferenceAnimation(final ScriptState<VertexDifferenceAnimation18> state, final VertexDifferenceAnimation18 animation) {
    animation.ticksRemaining_00--;

    if(animation.ticksRemaining_00 < 0) {
      // Make sure the mesh ends up exactly on the final pose since the renderer won't run again
      animation.updateMesh(1.0f);
      state.deallocateWithChildren();
      return;
    }

    //LAB_80109b7c
    //LAB_80109b90
    for(int i = 0; i < animation.vertexCount_08; i++) {
      final Vector3f source = animation.sourceVertices_0c[i];
      final Vector3f step = animation.step_10[i];
      final Vector3f current = animation.current_14[i];

      animation.previous[i].set(current);
      current.add(step);
      source.set(current);

      step.x = Math.fma(animation.embiggener_04, step.x, step.x);
      step.y = Math.fma(animation.embiggener_04, step.y, step.y);
      step.z = Math.fma(animation.embiggener_04, step.z, step.z);
    }

    //LAB_80109ce0
    // Mesh is updated by the renderer so it can interpolate between logic ticks
  }

  /** Renderer - lerps the mesh between the previous logic tick's vertices and the current ones */
  public static void renderVertexDifferenceAnimation(final ScriptState<VertexDifferenceAnimation18> state, final VertexDifferenceAnimation18 animation) {
    animation.updateMesh(SCRIPTS.getInterpAlpha());
  }

  private void updateMesh(final float alpha) {
    final MeshObj obj = (MeshObj)this.tmd.getObj();
    final Vector3f vertex = new Vector3f();

    for(final Mesh mesh : obj.meshes) {
      for(int offset = 0; offset < mesh.vertices().length; offset += TmdObjLoader.VERTEX_SIZE) {
        final int vertexIndex = (int)mesh.vertices()[offset + 0x3];

        if(vertexIndex < this.vertexCount_08) {
          this.previous[vertexIndex].lerp(this.current_14[vertexIndex], alpha, vertex);
        } else {
          vertex.set(this.tmd.vert_top_00[vertexIndex]);
        }

        mesh.vertices()[offset] = vertex.x;
        mesh.vertices()[offset + 0x1] = vertex.y;
        mesh.vertices()[offset + 0x2] = vertex.z;
      }

      mesh.update();
    }
  }

  @Override
  public Vector3f getPosition() {
    return null;
  }

  @Override
  public void renderScriptDebug(final ScriptState<? extends ScriptedObject> state) {

  }
}
