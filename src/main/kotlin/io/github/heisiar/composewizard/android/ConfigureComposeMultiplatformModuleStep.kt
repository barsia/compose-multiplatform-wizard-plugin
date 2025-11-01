package io.github.heisiar.composewizard

import com.android.sdklib.SdkVersionInfo
import com.android.tools.adtui.device.FormFactor
import com.android.tools.idea.npw.module.ConfigureModuleStep
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI.Borders.empty

class ConfigureComposeMultiplatformModuleStep(
  model: ComposeMultiplatformModuleModel,
  title: String
) : ConfigureModuleStep<ComposeMultiplatformModuleModel>(
  model,
  FormFactor.MOBILE,
  SdkVersionInfo.LOWEST_ACTIVE_API,
  title = title
) {
  
  override fun createMainPanel(): DialogPanel =
    panel {
      row("Module name") { 
        cell(moduleName).align(AlignX.FILL) 
      }
      row("Package name") { 
        cell(packageName).align(AlignX.FILL) 
      }
    }
    .withBorder(empty(6))
  
  override fun getPreferredFocusComponent() = moduleName
}
