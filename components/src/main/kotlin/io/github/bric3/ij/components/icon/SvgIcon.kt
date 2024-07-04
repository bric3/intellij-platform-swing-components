/**
 * MIT License
 *
 * Copyright (c) 2023 RE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.bric3.ij.components.icon

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.attributes.ViewBox
import com.github.weisj.jsvg.parser.SVGLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColorUtil
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.io.InputStream
import java.net.URLDecoder
import javax.swing.Icon

private const val ICON_SIZE = 20

private fun svgDataUrlToSvgElement(url: String): String {
    val decoded = URLDecoder.decode(url, "UTF-8")
    val index = decoded.indexOf("<svg")


    val iconColor = ColorUtil.toHtmlColor(JBUI.CurrentTheme.Label.foreground())
    return decoded.substring(index).replace("currentColor", iconColor)
}

private val loader = SVGLoader()

class SvgIcon(
    private val image: SVGDocument,
    var size: Int = ICON_SIZE
) : Icon {
    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
        // enable anti-aliasing
        (g as Graphics2D).setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON
        )

        val iconSizeFloat = JBUIScale.scale(size.toFloat())
        image.render(c, g, ViewBox(x.toFloat(), y.toFloat(), iconSizeFloat, iconSizeFloat))
    }

    override fun getIconWidth(): Int {
        return JBUIScale.scale(size)
    }

    override fun getIconHeight(): Int {
        return JBUIScale.scale(size)
    }

    companion object {
        @JvmStatic
        fun fromVirtualFile(virtualFile: VirtualFile, size: Int = ICON_SIZE) =
            fromStream(
                stream = virtualFile.inputStream,
                size = size,
            )

        @JvmStatic
        fun fromStream(stream: InputStream, size: Int = ICON_SIZE): SvgIcon {
            val image = loader.load(stream) ?: error("Failed to load svg")
            return SvgIcon(image, size)
        }
    }
}