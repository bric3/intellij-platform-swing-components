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

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.weisj.jsvg.attributes.ViewBox
import com.github.weisj.jsvg.parser.SVGLoader
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.ui.scale.JBUIScale.sysScale
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.ui.ExtendableHTMLViewFactory
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.image.BufferedImage
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
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
 * This extension caches the SVG images, and honors the `Cache-Control` header's `max-age`.
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
 */
class SvgImageExtension(private val existingSvgImageProvider: (key: String) -> Image?) :
    ExtendableHTMLViewFactory.Extension {
    constructor(preloadedSvgs: Map<String, Image> = emptyMap()) : this(preloadedSvgs::get)

    private data class CacheValue(
        val svgDocument: Image,
        val ttlInSeconds: Long
    )

    private val cache = Caffeine.newBuilder()
        .expireAfter(object : Expiry<String, CacheValue> {
            override fun expireAfterCreate(k: String?, v: CacheValue?, currentTime: Long): Long {
                return v?.ttlInSeconds?.let { SECONDS.toNanos(it) } ?: DAYS.toNanos(1)
            }

            override fun expireAfterUpdate(k: String?, v: CacheValue?, currentTime: Long, currentDuration: Long) =
                currentDuration

            override fun expireAfterRead(k: String?, v: CacheValue?, currentTime: Long, currentDuration: Long) =
                currentDuration
        })
        .maximumSize(30)
        .buildAsync<String, CacheValue?>()

    override fun invoke(element: Element, defaultView: View): View? {
        // example: <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build Passing" />
        if ("img" != element.name) return null

        val src = (element.attributes.getAttribute(HTML.Attribute.SRC) as? String) ?: return null

        // Loading is deferred to avoid blocking JEditorPane waiting on IO
        val deferredDoc = {
            cache.get(src) { srcValue ->
                try {
                    URL(srcValue).openConnection().run {
                        // Cache-Control: max-age=300, private
                        val cacheTtl = getHeaderField("cache-control")
                            ?.substringBefore(",")
                            ?.substringAfter("max-age=")
                            ?.toLongOrNull()

                        getInputStream().buffered().use { stream ->
                            SVGLoader().load(stream)?.let { svgDoc ->
                                val size = svgDoc.size()
                                val img = BufferedImage(
                                    size.width.toInt(),
                                    size.height.toInt(),
                                    BufferedImage.TYPE_INT_ARGB
                                ).apply {
                                    val graphics = createGraphics()
                                    graphics.setRenderingHint(
                                        RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON
                                    )
                                    svgDoc.render(
                                        null as Component?,
                                        graphics as Graphics2D,
                                        ViewBox(size)
                                    )
                                    graphics.dispose()
                                }

                                CacheValue(img, cacheTtl ?: DAYS.toSeconds(1))
                            }
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }.thenApply { it?.svgDocument }
        }

        return SvgImageView(element, deferredDoc)
    }

    private class SvgImageView(element: Element, deferredDoc: () -> CompletableFuture<Image?>) :
        ImageView(element) {

        private val svgImageProvider = deferredDoc().successOnEdt() {
            safePreferenceChanged()
            container?.revalidate()
            container?.repaint(
                imageBounds.x,
                imageBounds.y,
                it?.getWidth(container) ?: imageBounds.width,
                it?.getHeight(container) ?: imageBounds.height,
            )
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