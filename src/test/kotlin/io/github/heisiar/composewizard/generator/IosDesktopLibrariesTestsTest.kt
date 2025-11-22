package io.github.heisiar.composewizard.generator

import io.github.heisiar.composewizard.composer.ProjectComposer
import io.github.heisiar.composewizard.composer.features.GitFeature
import io.github.heisiar.composewizard.composer.features.HotReloadFeature
import io.github.heisiar.composewizard.composer.features.TestsFeature
import io.github.heisiar.composewizard.composer.modules.AndroidModule
import io.github.heisiar.composewizard.composer.modules.DesktopModule
import io.github.heisiar.composewizard.composer.modules.WebModule
import io.github.heisiar.composewizard.composer.modules.iOSModule
import io.github.heisiar.composewizard.testutils.FileTreeComparator
import io.github.heisiar.composewizard.testutils.ProjectFixtures
import io.github.heisiar.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test

class IosDesktopLibrariesTestsTest {

    @Test
    fun `generates ios desktop project with all libraries and tests`() {
        TemporaryProjectHelper.withTempProject { projectDir ->
            val config = ProjectFixtures.IOS_DESKTOP_LIBRARIES_TESTS_CONFIG
            val projectConfig = ProjectConfig.from(config)
            
            val composer = ProjectComposer(projectConfig)
            if (projectConfig.targetAndroid) composer.addModule(AndroidModule())
            if (projectConfig.targetDesktop) composer.addModule(DesktopModule())
            if (projectConfig.targetIOS) composer.addModule(iOSModule())
            if (projectConfig.targetWeb) composer.addModule(WebModule())

            if (projectConfig.includeTests) composer.addFeature(TestsFeature())
            if (projectConfig.initGit) composer.addFeature(GitFeature())
            if (projectConfig.includeHotReload) composer.addFeature(HotReloadFeature())
            
            composer.compose(projectDir.absolutePath)
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("ios-desktop-libraries-tests")
            FileTreeComparator.compareDirectories(fixtureDir, projectDir)
        }
    }
}
