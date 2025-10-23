package com.radiantbyte.novaclient.render

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.radiantbyte.novaclient.game.ModuleManager
import com.radiantbyte.novaclient.game.module.visual.ESPModule

class RenderOverlayView(context: Context) : View(context) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidate() // Initial render
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        ModuleManager.modules
            .filterIsInstance<ESPModule>()
            .filter { it.isEnabled && it.isSessionCreated } //isSessionCreated check
            .forEach { it.render(canvas) }

        if (ModuleManager.modules.any {
                it is ESPModule && it.isEnabled && it.isSessionCreated
            }) {
            postInvalidateOnAnimation()
        }
    }
}