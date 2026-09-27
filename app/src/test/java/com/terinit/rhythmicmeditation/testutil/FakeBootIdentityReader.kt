package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.runtime.BootIdentityReader

/**
 * Controllable [BootIdentityReader] for unit tests. Set [bootCount] to a new
 * value to simulate a device reboot.
 */
class FakeBootIdentityReader(var bootCount: Int? = 1) : BootIdentityReader {
    override fun bootCount(): Int? = bootCount
}
