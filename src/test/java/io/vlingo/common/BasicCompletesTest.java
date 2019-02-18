// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BasicCompletesTest {
  private Integer andThenValue;
  private Integer failureValue;

  @Test
  public void testCompletesWith() {
    final Completes<Integer> completes = new BasicCompletes<>(5, false);

    assertEquals(new Integer(5), completes.outcome());
  }

  @Test
  public void testCompletesAfterFunction() {
    final Completes<Integer> completes = new BasicCompletes<>(0);

    completes.andThen((value) -> value * 2);

    completes.with(5);

    assertEquals(new Integer(10), completes.outcome());
  }

  @Test
  public void testCompletesAfterConsumer() {
    final Completes<Integer> completes = new BasicCompletes<>(0);
    
    completes.andThen((value) -> andThenValue = value);
    
    completes.with(5);
    
    assertEquals(new Integer(5), completes.outcome());
  }

  @Test
  public void testCompletesAfterAndThen() {
    final Completes<Integer> completes = new BasicCompletes<>(0);

    completes
      .andThen((value) -> value * 2)
      .andThen((value) -> andThenValue = value);
    
    completes.with(5);
    
    assertEquals(new Integer(10), andThenValue);
    assertEquals(new Integer(10), completes.outcome());
  }

  @Test
  public void testCompletesAfterAndThenMessageOut() {
    final Completes<Integer> completes = new BasicCompletes<>(0);

    final Holder holder = new Holder();

    completes
      .andThen((value) -> value * 2)
      .andThen((value) -> { holder.hold(value); return value; } );

    completes.with(5);

    completes.await();

    assertEquals(new Integer(10), andThenValue);
  }

  @Test
  public void testOutcomeBeforeTimeout() {
    final Completes<Integer> completes = new BasicCompletes<>(new Scheduler());
    
    completes
      .andThen(1000, (value) -> value * 2)
      .andThen((value) -> andThenValue = value);
    
    completes.with(5);
    
    completes.await(10);

    assertEquals(new Integer(10), andThenValue);
  }

  @Test
  public void testTimeoutBeforeOutcome() throws Exception {
    final Completes<Integer> completes = new BasicCompletes<>(new Scheduler());
    
    completes
      .andThen(1, 0, (value) -> value * 2)
      .andThen((value) -> andThenValue = value);
    
    Thread.sleep(100);
    
    completes.with(5);

    completes.await();

    assertTrue(completes.hasFailed());
    assertNotEquals(new Integer(10), andThenValue);
    assertNull(andThenValue);
  }

  @Test
  public void testThatNullFailureOutcomeFails() {
    final Completes<Integer> completes = new BasicCompletes<>(new Scheduler());
    
    completes
      .andThen(null, (value) -> value * 2)
      .andThen((Integer value) -> andThenValue = value)
      .otherwise((failedValue) -> failureValue = 1000);

    completes.with(null);

    completes.await();

    assertTrue(completes.hasFailed());
    assertNull(andThenValue);
    assertEquals(new Integer(1000), failureValue);
  }

  @Test
  public void testThatNonNullFailureOutcomeFails() {
    final Completes<Integer> completes = new BasicCompletes<>(new Scheduler());
    
    completes
      .andThen(new Integer(20000), (value) -> value * 2)
      .andThen((Integer value) -> andThenValue = value)
      .otherwise((failedValue) -> failureValue = 1000);

    completes.with(new Integer(20000));

    completes.await();

    assertTrue(completes.hasFailed());
    assertNull(andThenValue);
    assertEquals(new Integer(1000), failureValue);
  }

  @Test
  public void testThatExceptionOutcomeFails() {
    final Completes<Integer> completes = new BasicCompletes<>(new Scheduler());
    
    completes
      .andThen(null, (value) -> value * 2)
      .andThen((Integer value) -> { throw new IllegalStateException("" + (value * 2)); })
      .recoverFrom((e) -> failureValue = Integer.parseInt(e.getMessage()));

    completes.with(2);

    completes.await();

    assertTrue(completes.hasFailed());
    assertNull(andThenValue);
    assertEquals(new Integer(8), failureValue);
  }

  @Test
  public void testThatAwaitTimesout() throws Exception {
    final Completes<Integer> completes = new BasicCompletes<>(new Scheduler());
    
    final Integer completed = completes.await(10);
    
    completes.with(5);
    
    assertNotEquals(new Integer(5), completed);
    assertNull(completed);
  }

  @Test
  public void testThatAwaitCompletes() throws Exception {
    final Completes<Integer> completes = new BasicCompletes<>(new Scheduler());
    
    new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(100);
          completes.with(5);
        } catch (Exception e) {
          // ignore
        }
      }
    }.start();

    final Integer completed = completes.await();

    assertEquals(new Integer(5), completed);
  }

  private class Holder {
    private void hold(final Integer value) {
      andThenValue = value;
    }
  }
}
