package org.fruit.alayer.webdriver;

import org.fruit.Assert;
import org.fruit.alayer.*;
import org.fruit.alayer.exceptions.StateBuildException;

import java.util.concurrent.*;

public class WdStateBuilder implements StateBuilder {
  private static final long serialVersionUID = -8640937524707589772L;

  private static final int _defaultThreadPoolCount = 1;

  private final double timeOut;
  private transient ExecutorService executor;

  public WdStateBuilder(double timeOut) {
    Assert.isTrue(timeOut > 0);
    this.timeOut = timeOut;

    // Needed to be able to schedule asynchornous tasks conveniently.
    executor = Executors.newFixedThreadPool(_defaultThreadPoolCount);
  }

  @Override
  public WdState apply(SUT system) throws StateBuildException {
    try {
      Future<WdState> future = executor.submit(new WdStateFetcher(system));
      return future.get((long) (timeOut), TimeUnit.SECONDS);
    }
    catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      throw new StateBuildException(e);
    }
    catch (TimeoutException e) {
      System.out.println();
      e.printStackTrace();
      System.out.println();

      WdState wdState = new WdState(WdStateFetcher.buildRoot(system));
      wdState.set(Tags.Role, Roles.Process);
      wdState.set(Tags.NotResponding, true);
      return wdState;
    }
  }
}
