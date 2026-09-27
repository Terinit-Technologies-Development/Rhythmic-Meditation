package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.runtime.ScreenStateReader

/**
 * Controllable [ScreenStateReader] for unit tests.
 * `interactive == false` simulates the screen being off.
 */
class FakeScreenStateReader(var interactive: Boolean = true) : ScreenStateReader {
    override fun isInteractive(): Boolean = interactive
}
