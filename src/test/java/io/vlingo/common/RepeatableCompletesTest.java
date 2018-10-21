// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.vlingo.common.Completes;
import io.vlingo.common.RepeatableCompletes;

public class RepeatableCompletesTest {
  private Integer andThenValue;

  @Test
  public void testThatCompletesRepeats() {
    final Completes<Integer> completes = new RepeatableCompletes<>(0);

    completes
      .andThen((value) -> value * 2)
      .andThen((Integer value) -> andThenValue = value)
      .repeat();
    
    completes.with(5);
    assertEquals(new Integer(10), andThenValue);
    completes.with(10);
    assertEquals(new Integer(20), andThenValue);
    completes.with(20);
    assertEquals(new Integer(40), andThenValue);
  }
}
