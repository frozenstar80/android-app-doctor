package com.appdoctor.compose

import com.appdoctor.core.AppDoctorConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDoctorComposePluginFactoryTest {

    private val factory = AppDoctorComposePluginFactory()

    @Test
    fun `creates plugin when captureCompose is enabled`() {
        val plugin = factory.create(AppDoctorConfig(captureCompose = true))
        assertNotNull(plugin)
        assertTrue(plugin is AppDoctorComposePlugin)
    }

    @Test
    fun `returns null when captureCompose is disabled`() {
        assertNull(factory.create(AppDoctorConfig(captureCompose = false)))
    }

    @Test
    fun `config defaults are additive and backward compatible`() {
        val config = AppDoctorConfig()
        assertTrue(config.captureCompose)
        assertFalse(config.enableComposeAnalytics)
        assertFalse(config.enableComposableTracking)
        assertEquals(200, config.trackedComposableLimit)
    }

    @Test
    fun `plugin exposes stable identity and mirrors config flags`() {
        val plugin = AppDoctorComposePlugin(
            AppDoctorConfig(enableComposeAnalytics = true, enableComposableTracking = true),
        )
        assertEquals("compose-inspector", plugin.id)
        assertEquals("compose", plugin.tabKey)
        assertEquals("Compose", plugin.tabTitle)
        assertTrue(plugin.analyticsEnabled)
        assertTrue(plugin.trackingEnabled)
    }
}
