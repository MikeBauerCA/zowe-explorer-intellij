/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.utils

interface QueueExecutor<V> {

  fun launch(execution: (V) -> Unit)

  fun accept(input: V)

  fun shutdown()

  fun pause()

  fun resume()

}
