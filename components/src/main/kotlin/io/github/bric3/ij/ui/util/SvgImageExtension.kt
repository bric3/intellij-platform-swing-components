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
import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.attributes.ViewBox
import com.github.weisj.jsvg.parser.SVGLoader
import com.intellij.util.ui.ExtendableHTMLViewFactory
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.Shape
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
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
 *             SvgImageExtension,
 *             // other extensions
 *         )
 *         .withFontResolver(EditorCssFontResolver.getGlobalInstance())
 *         .build()
 *     text = ...
 * }
 * ```
 */
class SvgImageExtension : ExtendableHTMLViewFactory.Extension {
    private data class CacheValue(
        val svgDocument: SVGDocument,
        val ttlInSeconds: Long
    )

    private val cache = Caffeine.newBuilder()
        .expireAfter(object : Expiry<String, CacheValue> {
            override fun expireAfterCreate(k: String?, v: CacheValue?, currentTime: Long): Long {
                return v?.ttlInSeconds?.let { TimeUnit.SECONDS.toNanos(it) } ?: TimeUnit.DAYS.toNanos(1)
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

        val src = (element.attributes.getAttribute(HTML.Attribute.SRC) as? String)?.takeIf {
            it.startsWith("https://")
        } ?: return null

        val url = try {
            URL(src)
        } catch (e: Exception) {
            null
        } ?: return null

        // Loading is deferred to avoid blocking JEditorPane waiting on IO
        val deferredDoc = cache.get(url.toString()) { _ ->
            try {
                url.openConnection().run {
                    // Cache-Control: max-age=300, private
                    val cacheTtl = getHeaderField("cache-control")
                        ?.substringBefore(",")
                        ?.substringAfter("max-age=")
                        ?.toLongOrNull()

                    getInputStream().buffered().use { stream ->
                        SVGLoader().load(stream)?.let {
                            CacheValue(it, cacheTtl ?: TimeUnit.DAYS.toSeconds(1))
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }.thenApply { it?.svgDocument }

        return SvgImageView(element, deferredDoc)
    }

    private class SvgImageView(element: Element, val deferredDoc: CompletableFuture<SVGDocument?>) :
        ImageView(element) {
        private val svgDocument: SVGDocument?
            get() = if (deferredDoc.isDone) deferredDoc.get() else null

        private val imageBounds = Rectangle()

        init {
            deferredDoc.thenAccept {
                safePreferenceChanged()
                container.repaint(
                    imageBounds.x,
                    imageBounds.y,
                    imageBounds.width,
                    imageBounds.height,
                )
            }
        }

        override fun getPreferredSpan(axis: Int): Float {
            return when (val doc = svgDocument) {
                null -> {
                    super.getPreferredSpan(axis)
                }

                else -> {
                    when (axis) {
                        X_AXIS -> doc.size().width
                        Y_AXIS -> doc.size().height
                        else -> throw IllegalArgumentException("Invalid axis: $axis")
                    }
                }
            }
        }

        override fun paint(g: Graphics, a: Shape) {
            val rect = if ((a is Rectangle)) a else a.bounds
            imageBounds.bounds = rect

            when (val doc = svgDocument) {
                null -> super.paint(g, a)
                else -> {
                    val config = GraphicsUtil.setupAAPainting(g)

                    doc.render(
                        this.container,
                        g.create() as Graphics2D,
                        ViewBox(
                            rect.x.toFloat(),
                            rect.y.toFloat(),
                            doc.size().width,
                            doc.size().height
                        )
                    )

                    config.restore()
                }
            }
        }

        // This view controls the loading, so avoid loading anything in parent ImageView
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