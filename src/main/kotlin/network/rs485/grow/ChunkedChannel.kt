/*
 * Copyright (c) 2020  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2020  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.grow

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking

abstract class ChunkedChannel<T, O>(val channel: Channel<T>) : Runnable {

    /**
     * Returns a session object for this run.
     */
    abstract fun newSession(): O

    /**
     * Returns true, if there is more to do. False otherwise.
     */
    abstract fun hasWork(session: O): Boolean

    /**
     * Returns a Sequence based on the current session.
     */
    abstract fun sequenceFactory(session: O): Sequence<T>

    /**
     * Is called if there is more work to do. Should be used with some kind of work queue.
     */
    abstract fun rerun(r: Runnable)

    private fun checkWork(session: O): Boolean = if (hasWork(session)) false else true.also { channel.close() }

    override fun run() {
        try {
            val session = newSession()
            if (checkWork(session)) return
            sequenceFactory(session).forEach(channel::sendBlocking)
            if (checkWork(session)) return
            rerun(this)
        } catch (e: Throwable) {
            channel.close(e)
        }
    }

}
