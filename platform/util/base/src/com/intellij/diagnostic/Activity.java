// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.diagnostic;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public interface Activity {
  void end();

  void setDescription(@NonNls @NotNull String description);

  /**
   * Convenient method to end token and start a new sibling one.
   * So, start of new is always equals to this item end and yet another System.nanoTime() call is avoided.
   */
  @NotNull Activity endAndStart(@NonNls @NotNull String name);

  @NotNull Activity startChild(@NonNls @NotNull String name);
}
