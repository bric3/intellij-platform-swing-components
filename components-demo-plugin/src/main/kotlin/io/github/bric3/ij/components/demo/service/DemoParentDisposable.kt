package io.github.bric3.ij.components.demo.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DemoParentDisposable : Disposable {
    override fun dispose() {}

    companion object {
        val Project.demoParentDisposable: DemoParentDisposable
            get() = service()
    }
}
