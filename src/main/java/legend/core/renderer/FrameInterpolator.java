package legend.core.renderer;

import legend.core.QueuePool;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Lets engine states that simulate at a fixed rate (e.g. 60 Hz) be presented at a higher display refresh rate.
 * <p>
 * Each simulation step queues its models as usual. Before the next step resets the render queues, the transforms of
 * everything that was queued are snapshotted. When the next step has queued its models, each one is matched with the
 * same model from the previous step (same {@link Obj}, vertex range, queue and order). On every display frame the
 * matched models are drawn with their transforms blended between the previous and latest step, and display frames
 * that fall between steps simply redraw the latest step's queues. Since the camera is baked into the model
 * transforms, this smooths camera movement as well as model movement.
 */
final class FrameInterpolator {
  /** Models that move further than this (in a single step) are treated as having teleported and aren't blended */
  private static final float MAX_ORTHO_DISTANCE_SQ = 64.0f * 64.0f;
  private static final float MAX_PERSPECTIVE_DISTANCE_SQ = 2048.0f * 2048.0f;

  private static final class Snapshot {
    QueuePool<QueuedModel<?, ?>> pool;
    int sequence;
    int startVertex;
    int vertexCount;
    boolean used;
    final Matrix4f transforms = new Matrix4f();
    final Vector3f screenspaceOffset = new Vector3f();
  }

  private static final class SnapshotList {
    final List<Snapshot> snapshots = new ArrayList<>();
    int cursor;
  }

  private static final Comparator<Snapshot> SNAPSHOT_ORDER = Comparator.<Snapshot>comparingInt(s -> System.identityHashCode(s.pool)).thenComparingInt(s -> s.sequence);

  private final IdentityHashMap<Obj, SnapshotList> previous = new IdentityHashMap<>();
  private final List<Snapshot> snapshotPool = new ArrayList<>();
  private final List<SnapshotList> listPool = new ArrayList<>();
  private int snapshotsUsed;
  private int listsUsed;

  /** Remember the transforms of everything currently queued as the "previous" step */
  void snapshot(final RenderBatch mainBatch, final List<RenderBatch> batches) {
    this.previous.clear();
    this.snapshotsUsed = 0;
    this.listsUsed = 0;

    this.snapshotPool(mainBatch.modelPool);
    this.snapshotPool(mainBatch.orthoPool);

    for(int i = 0; i < batches.size(); i++) {
      this.snapshotPool(batches.get(i).modelPool);
      this.snapshotPool(batches.get(i).orthoPool);
    }

    // Pools are sorted by depth when rendered, put each object's entries back into queue order for matching
    for(final SnapshotList list : this.previous.values()) {
      list.snapshots.sort(SNAPSHOT_ORDER);
    }
  }

  /** Forget the previous step (e.g. after a scene change) so nothing is blended */
  void clear() {
    this.previous.clear();
    this.snapshotsUsed = 0;
    this.listsUsed = 0;
  }

  private void snapshotPool(final QueuePool<QueuedModel<?, ?>> pool) {
    for(int i = 0; i < pool.size(); i++) {
      final QueuedModel<?, ?> model = pool.get(i);

      if(model.obj == null) {
        continue;
      }

      final Snapshot snapshot = this.allocSnapshot();
      snapshot.pool = pool;
      snapshot.sequence = model.sequence;
      snapshot.startVertex = model.startVertex;
      snapshot.vertexCount = model.vertexCount;
      snapshot.used = false;
      // Snapshot the actual step transforms, not the blended ones
      snapshot.transforms.set(model.stepTransforms);
      snapshot.screenspaceOffset.set(model.stepScreenspaceOffset);

      SnapshotList list = this.previous.get(model.obj);
      if(list == null) {
        list = this.allocList();
        this.previous.put(model.obj, list);
      }

      list.snapshots.add(snapshot);
    }
  }

  /** Match everything queued by the step that just ran against the previous step */
  void match(final RenderBatch mainBatch, final List<RenderBatch> batches) {
    this.matchPool(mainBatch.modelPool, false);
    this.matchPool(mainBatch.orthoPool, true);

    for(int i = 0; i < batches.size(); i++) {
      this.matchPool(batches.get(i).modelPool, false);
      this.matchPool(batches.get(i).orthoPool, true);
    }
  }

  private void matchPool(final QueuePool<QueuedModel<?, ?>> pool, final boolean ortho) {
    for(int i = 0; i < pool.size(); i++) {
      final QueuedModel<?, ?> model = pool.get(i);
      model.stepTransforms.set(model.transforms);
      model.stepScreenspaceOffset.set(model.screenspaceOffset);
      model.hasPreviousStep = false;

      final SnapshotList list = model.obj != null ? this.previous.get(model.obj) : null;
      if(list == null) {
        continue;
      }

      final Snapshot snapshot = this.find(list, pool, model);
      if(snapshot == null) {
        continue;
      }

      snapshot.used = true;

      final float dx = model.transforms.m30() - snapshot.transforms.m30() + model.screenspaceOffset.x - snapshot.screenspaceOffset.x;
      final float dy = model.transforms.m31() - snapshot.transforms.m31() + model.screenspaceOffset.y - snapshot.screenspaceOffset.y;
      final float dz = model.transforms.m32() - snapshot.transforms.m32();
      final float distSq = dx * dx + dy * dy + dz * dz;

      if(distSq > (ortho ? MAX_ORTHO_DISTANCE_SQ : MAX_PERSPECTIVE_DISTANCE_SQ)) {
        continue;
      }

      model.previousTransforms.set(snapshot.transforms);
      model.previousScreenspaceOffset.set(snapshot.screenspaceOffset);
      model.hasPreviousStep = true;
    }
  }

  private Snapshot find(final SnapshotList list, final QueuePool<QueuedModel<?, ?>> pool, final QueuedModel<?, ?> model) {
    final List<Snapshot> snapshots = list.snapshots;

    // Entries are normally queued in the same order every step, so search forward from the last match first
    for(int i = list.cursor; i < snapshots.size(); i++) {
      final Snapshot snapshot = snapshots.get(i);

      if(!snapshot.used && snapshot.pool == pool && snapshot.startVertex == model.startVertex && snapshot.vertexCount == model.vertexCount) {
        list.cursor = i + 1;
        return snapshot;
      }
    }

    for(int i = 0; i < list.cursor && i < snapshots.size(); i++) {
      final Snapshot snapshot = snapshots.get(i);

      if(!snapshot.used && snapshot.pool == pool && snapshot.startVertex == model.startVertex && snapshot.vertexCount == model.vertexCount) {
        return snapshot;
      }
    }

    return null;
  }

  /** Blend every matched model between the previous and latest step */
  void apply(final RenderBatch mainBatch, final List<RenderBatch> batches, final float alpha) {
    this.applyPool(mainBatch.modelPool, alpha);
    this.applyPool(mainBatch.orthoPool, alpha);

    for(int i = 0; i < batches.size(); i++) {
      this.applyPool(batches.get(i).modelPool, alpha);
      this.applyPool(batches.get(i).orthoPool, alpha);
    }
  }

  private void applyPool(final QueuePool<QueuedModel<?, ?>> pool, final float alpha) {
    for(int i = 0; i < pool.size(); i++) {
      final QueuedModel<?, ?> model = pool.get(i);

      if(model.hasPreviousStep) {
        model.previousTransforms.lerp(model.stepTransforms, alpha, model.transforms);
        model.previousScreenspaceOffset.lerp(model.stepScreenspaceOffset, alpha, model.screenspaceOffset);
      } else {
        model.transforms.set(model.stepTransforms);
        model.screenspaceOffset.set(model.stepScreenspaceOffset);
      }
    }
  }

  /** Put the latest step's transforms back */
  void restore(final RenderBatch mainBatch, final List<RenderBatch> batches) {
    this.restorePool(mainBatch.modelPool);
    this.restorePool(mainBatch.orthoPool);

    for(int i = 0; i < batches.size(); i++) {
      this.restorePool(batches.get(i).modelPool);
      this.restorePool(batches.get(i).orthoPool);
    }
  }

  private void restorePool(final QueuePool<QueuedModel<?, ?>> pool) {
    for(int i = 0; i < pool.size(); i++) {
      final QueuedModel<?, ?> model = pool.get(i);
      model.transforms.set(model.stepTransforms);
      model.screenspaceOffset.set(model.stepScreenspaceOffset);
    }
  }

  private Snapshot allocSnapshot() {
    if(this.snapshotsUsed == this.snapshotPool.size()) {
      this.snapshotPool.add(new Snapshot());
    }

    return this.snapshotPool.get(this.snapshotsUsed++);
  }

  private SnapshotList allocList() {
    if(this.listsUsed == this.listPool.size()) {
      this.listPool.add(new SnapshotList());
    }

    final SnapshotList list = this.listPool.get(this.listsUsed++);
    list.snapshots.clear();
    list.cursor = 0;
    return list;
  }
}
