package io.legado.app.lib.cronet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple instrumentation test to verify Cronet initialization path does not crash
 * on devices with different Cronet implementations. The test passes as long as
 * accessing the lazy `cronetEngine` does not throw an exception. It allows the
 * engine to be null (meaning Cronet not available), which is a valid fallback.
 */
@RunWith(AndroidJUnit4::class)
class CronetInitTest {
    @Test
    fun cronetInitializationDoesNotThrow() {
        // Ensure app context is available; accessing cronetEngine should not throw.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Access the lazy property. We don't assert non-null because availability
        // depends on device and Cronet artifacts; the goal is to ensure no crash.
        val engine = cronetEngine
        // No assertions — test will fail if an exception is thrown during initialization.
    }
}
