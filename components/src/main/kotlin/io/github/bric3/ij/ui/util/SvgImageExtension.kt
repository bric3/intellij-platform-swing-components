/*
 * IntelliJ Platform Swing Components
 *
 * Copyright (c) 2023 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package io.github.bric3.ij.ui.util

import com.intellij.ui.scale.JBUIScale.sysScale
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.SVGLoader
import com.intellij.util.ui.ExtendableHTMLViewFactory
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Graphics
import java.awt.Image
import java.awt.Rectangle
import java.awt.Shape
import java.net.URL
import java.util.concurrent.CompletableFuture
import javax.swing.SwingUtilities
import javax.swing.text.AbstractDocument
import javax.swing.text.Element
import javax.swing.text.View
import javax.swing.text.html.HTML
import javax.swing.text.html.ImageView

/**
 * An extension to be used with [HTMLEditorKitBuilder] that supports loading SVG images.
 *
 * This form in particular is supported
 * ```html
 * <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build Passing" />
 * ```
 *
 * Add this extension when building the editor kit :
 *
 * ```kotlin
 * JEditorPane().apply {
 *     contentType = "text/html"
 *     editorKit = HTMLEditorKitBuilder()
 *         .withViewFactoryExtensions(
 *             SvgImageExtension(),
 *             // other extensions
 *         )
 *         .withFontResolver(EditorCssFontResolver.getGlobalInstance())
 *         .build()
 *     text = ...
 * }
 * ```
 *
 * This is using the internal [SVGLoader] to load SVG images.
 *
 * @param svgImageProvider a function that provides a [CompletableFuture] of an [Image] given
 *  the `src` attribute of an image, provided to customize caching (e.g., following `Cache-Control` directive),
 *  or customize the rendering engine.
 */
class SvgImageExtension(private val svgImageProvider: ((key: String) -> (() -> CompletableFuture<Image?>))? = null) :
    ExtendableHTMLViewFactory.Extension {

    /**
     * Creates a [SvgImageExtension] with a preloaded SVGs cache.
     * @param preloadedSvgs a map of preloaded SVGs images, to be used as a cache.
     */
    constructor(preloadedSvgs: Map<String, Image>) : this({ src ->
        { CompletableFuture.completedFuture(preloadedSvgs[src]) }
    })

    override fun invoke(element: Element, defaultView: View): View? {
        // example: <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build Passing" />
        if ("img" != element.name) return null

        val src = (element.attributes.getAttribute(HTML.Attribute.SRC) as? String) ?: return null

        val completableFuture = svgImageProvider?.invoke(src)
            ?: defaultImageProvider(src)

        return SvgImageView(element, completableFuture)
    }

    // Default implementation, internal API usage because it's a basis for PR https://github.com/JetBrains/intellij-community/pull/2777
    private fun defaultImageProvider(src: String): () -> CompletableFuture<Image?> = {
        try {
            CompletableFuture.supplyAsync {
                try {
                    SVGLoader.load(URL(src), sysScale())
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            CompletableFuture.completedFuture(null)
        }
    }

    private class SvgImageView(element: Element, deferredImage: () -> CompletableFuture<Image?>) :
        ImageView(element) {

        private val svgImageProvider = deferredImage().thenApply {
            SwingUtilities.invokeLater {
                safePreferenceChanged()
                container?.revalidate()
                container?.repaint(
                    imageBounds.x,
                    imageBounds.y,
                    it?.getWidth(container) ?: imageBounds.width,
                    it?.getHeight(container) ?: imageBounds.height,
                )
            }
            it
        }

        private val svgImage: Image?
            get() = if (svgImageProvider.isDone) {
                svgImageProvider.get()
            } else {
                null
            }

        private val imageBounds = Rectangle()

        override fun getMinimumSpan(axis: Int) = getPreferredSpan(axis)

        override fun getMaximumSpan(axis: Int) = getPreferredSpan(axis)

        override fun getPreferredSpan(axis: Int) = when (val img = svgImage) {
            null -> super.getPreferredSpan(axis)
            else -> {
                when (axis) {
                    X_AXIS -> img.getWidth(container) / sysScale()
                    Y_AXIS -> img.getHeight(container) / sysScale()
                    else -> throw IllegalArgumentException("Invalid axis: $axis")
                }
            }
        }

        override fun paint(g: Graphics, a: Shape) {
            val rect = a.bounds
            imageBounds.bounds = rect

            when (val img = svgImage) {
                null -> super.paint(g, a)
                else -> {
                    UIUtil.drawImage(
                        g,
                        ImageUtil.ensureHiDPI(img, ScaleContext.create(null as Component?)),
                        rect.x,
                        rect.y,
                        container
                    )
                }
            }
        }

        // This view controls the loading, using null avoids loading anything in parent ImageView
        override fun getImageURL(): URL? = null

        /**
         * Invokes `preferenceChanged` on the event dispatching thread.
         */
        private fun safePreferenceChanged() {
            if (SwingUtilities.isEventDispatchThread()) {
                val doc = document as? AbstractDocument
                doc?.readLock()
                preferenceChanged(null, true, true)
                doc?.readUnlock()
            } else {
                SwingUtilities.invokeLater { safePreferenceChanged() }
            }
        }
    }
}