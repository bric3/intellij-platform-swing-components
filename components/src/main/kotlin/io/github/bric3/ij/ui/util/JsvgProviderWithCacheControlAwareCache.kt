/*
 * IntelliJ Platform Swing Components
 *
 * Copyright (c) 2024 - Brice Dutheil
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
import com.github.weisj.jsvg.SVGRenderingHints
import com.github.weisj.jsvg.attributes.ViewBox
import com.github.weisj.jsvg.parser.SVGLoader
import com.github.weisj.jsvg.util.ImageUtil
import com.intellij.ui.scale.JBUIScale
import java.awt.Component
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/**
 * To be used in conjunction with [SvgImageExtension] that supports loading SVG images.
 *
 * The cache supports the `Cache-Control` header's `max-age`.
 * Also, the current scale is part of the cache key.
 */
class JsvgProviderWithCacheControlAwareCache : (String) -> () -> CompletableFuture<Image?> {
    private data class CacheKey(
        val src: String,
        val scale: Float
    )

    private data class CacheValue(
        val svgDocument: Image,
        val ttlInSeconds: Long
    )

    private val cache = Caffeine.newBuilder()
        .expireAfter(object : Expiry<CacheKey, CacheValue> {
            override fun expireAfterCreate(k: CacheKey?, v: CacheValue?, currentTime: Long): Long {
                return v?.ttlInSeconds?.let { TimeUnit.SECONDS.toNanos(it) } ?: TimeUnit.DAYS.toNanos(1)
            }

            override fun expireAfterUpdate(k: CacheKey?, v: CacheValue?, currentTime: Long, currentDuration: Long) =
                currentDuration

            override fun expireAfterRead(k: CacheKey?, v: CacheValue?, currentTime: Long, currentDuration: Long) =
                currentDuration
        })
        .maximumSize(30)
        .buildAsync<CacheKey, CacheValue?>()

    override fun invoke(src: String): () -> CompletableFuture<Image?> {
        // Loading is deferred to avoid blocking JEditorPane waiting on IO
        return {
            cache.get(CacheKey(src, JBUIScale.sysScale())) { key ->
                try {
                    URL(key.src).openConnection().run {
                        // Cache-Control: max-age=300, private
                        val cacheTtl = getHeaderField("cache-control")
                            ?.substringBefore(",")
                            ?.substringAfter("max-age=")
                            ?.toLongOrNull()

                        getInputStream().buffered().use { stream ->
                            SVGLoader().load(stream)?.let { svgDoc ->
                                val size = svgDoc.size()

                                val scaledWidth = ceil(size.width * JBUIScale.sysScale())
                                val scaledHeight = ceil(size.height * JBUIScale.sysScale())

                                val img = ImageUtil.createCompatibleTransparentImage(
                                    scaledWidth.toInt(),
                                    scaledHeight.toInt(),
                                ).apply {
                                    val graphics = createGraphics()
                                    graphics.setRenderingHint(
                                        RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON
                                    )
                                    graphics.setRenderingHint(
                                        SVGRenderingHints.KEY_SOFT_CLIPPING,
                                        SVGRenderingHints.VALUE_SOFT_CLIPPING_ON
                                    )
                                    svgDoc.render(
                                        null as Component?,
                                        graphics as Graphics2D,
                                        ViewBox(scaledWidth, scaledHeight)
                                    )
                                    graphics.dispose()
                                }

                                CacheValue(img, cacheTtl ?: TimeUnit.DAYS.toSeconds(1))
                            }
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }.thenApply { it?.svgDocument }
        }
    }
}