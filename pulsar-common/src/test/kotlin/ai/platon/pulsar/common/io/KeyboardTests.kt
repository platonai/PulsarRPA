package ai.platon.pulsar.common.io

import java.awt.Robot
import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardTests {
    private val keyboard = KeyboardDescription.US_KEYBOARD_LAYOUT
    private val robot = Robot()
    
    @Test
    fun testKeyboard() {
        assertEquals(keyboard["KeyA"]!!.keyCode, KeyEvent.VK_A)
        assertEquals(keyboard["KeyW"]!!.keyCode, KeyEvent.VK_W)
        
        assertEquals(keyboard["Enter"]!!.keyCode, KeyEvent.VK_ENTER)
        
//        robot.keyPress(keyboard["KeyA"]!!.keyCode)
//        robot.keyPress(KeyEvent.VK_A)
    }
}