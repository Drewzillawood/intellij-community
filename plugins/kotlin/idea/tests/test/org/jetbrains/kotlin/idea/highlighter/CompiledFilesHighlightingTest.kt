// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.base.plugin.artifacts.TestKotlinArtifacts
import org.jetbrains.kotlin.idea.base.test.TestRoot
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.runner.RunWith
import java.io.File

@TestRoot("idea/tests")
@TestDataPath("\$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
@TestMetadata("testData/highlighter/compiled")
class CompiledFilesHighlightingTest: KotlinLightCodeInsightFixtureTestCase() {
    @TestMetadata("kotlin/collections/CollectionsKt.kotlin_metadata")
    fun testKotlinCollectionsCollectionsKtKotlinMetadata() {
        doTestWithLibraryFile(TestKotlinArtifacts.kotlinStdlibCommon)
    }

    @TestMetadata("kotlin/time/TimeSource.class")
    fun testKotlinTimeTimeSourceClass() {
        doTestWithLibraryFile(TestKotlinArtifacts.kotlinStdlib)
    }

    @TestMetadata("default/linkdata/package_kotlin.io/0_io.knm")
    fun testKotlinNativeLinkdataPackageKotlinIO0ioKnm() {
        doTestWithLibraryFile(TestKotlinArtifacts.kotlinStdlibNative)
    }

    private fun doTestWithLibraryFile(libraryFile: File) {
        val libraryVirtualFile =
            if (libraryFile.extension == "jar") {
                StandardFileSystems.jar().findFileByPath(libraryFile.absolutePath + URLUtil.JAR_SEPARATOR)
            } else {
                VirtualFileManager.getInstance().findFileByNioPath(libraryFile.toPath())
            } ?: error("unable to locate ${libraryFile.name}")
        var virtualFile: VirtualFile = libraryVirtualFile
        for (childName in fileName().split("/")) {
            virtualFile = virtualFile.findChild(childName) ?: error("unable to locate $childName in $virtualFile")
        }
        val libraryName = "library ${libraryFile.name}"
        ConfigLibraryUtil.addLibrary(module, libraryName) {
            addRoot(libraryFile, OrderRootType.CLASSES)
        }
        try {
            myFixture.openFileInEditor(virtualFile)
            doTest()
        } finally {
            ConfigLibraryUtil.removeLibrary(module, libraryName)
        }
    }

    private fun doTest() {
        val fileText = FileUtil.loadFile(File(dataFilePath(fileName().replace('/', '.') + ".txt")), true)
        try {
            withCustomCompilerOptions(fileText, project, module) {
                (myFixture as CodeInsightTestFixtureImpl).canChangeDocumentDuringHighlighting(false)
                val data = ExpectedHighlightingData(DocumentImpl(fileText), true, true, true, true)
                data.init()
                (myFixture as CodeInsightTestFixtureImpl).collectAndCheckHighlighting(data)
            }
        } catch (e: FileComparisonFailure) {
            val highlights =
                DaemonCodeAnalyzerImpl.getHighlights(myFixture.getDocument(myFixture.getFile()), null, myFixture.getProject())
            val text = myFixture.getFile().getText()
            println(TagsTestDataUtil.insertInfoTags(highlights, text))
            throw e
        }
    }
}