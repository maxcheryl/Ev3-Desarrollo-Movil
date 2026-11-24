package cl.duocuc.evaluacion2

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Comprueba que la aplicación puede abrirse en un dispositivo Android.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Obtiene el contexto de la aplicación que se está probando.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("cl.duocuc.evaluacion2", appContext.packageName)
    }
}