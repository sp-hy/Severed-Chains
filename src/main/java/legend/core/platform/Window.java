package legend.core.platform;

import legend.core.platform.input.InputClass;
import legend.core.renderer.RenderApi;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

public abstract class Window {
  private final WindowEvents events = new WindowEvents(this);
  private BooleanSupplier shouldClearPressedInput = () -> true;

  public abstract RenderApi getRenderApi();

  protected abstract void destroy();
  protected abstract boolean shouldClose();

  public WindowEvents events() {
    return this.events;
  }

  /**
   * One-frame input (pressed actions) is cleared after each draw. The renderer uses this to keep presses around on
   * display frames where the game didn't run, so they aren't lost.
   */
  public void setShouldClearPressedInput(final BooleanSupplier shouldClearPressedInput) {
    this.shouldClearPressedInput = shouldClearPressedInput;
  }

  protected boolean shouldClearPressedInput() {
    return this.shouldClearPressedInput.getAsBoolean();
  }

  public abstract void show();
  public abstract void close();

  public abstract void updateMonitor();
  public abstract void makeFullscreen();
  public abstract void makeWindowed();
  public abstract void centerWindow();

  public abstract void setTitle(final String title);

  public abstract int getWidth();
  public abstract int getHeight();
  public abstract boolean hasFocus();

  public abstract InputClass getInputClass();
  public abstract void startTextInput();
  public abstract void stopTextInput();

  public abstract void disableCursor();
  public abstract void showCursor();
  public abstract void hideCursor();
  public abstract void useNormalCursor();
  public abstract void usePointerCursor();
  public abstract void setWindowIcon(final Path path);

  public abstract void setFpsLimit(final int limit);
  public abstract int getFpsLimit();

  /** Refresh rate of the display the window is currently on, or 60 if it can't be determined */
  public abstract float getRefreshRate();
}
