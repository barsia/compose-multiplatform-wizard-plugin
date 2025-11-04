package io.github.heisiar.composewizard.generator.xcode

import io.github.heisiar.composewizard.generator.ProjectConfig

class XcodeProjectGenerator(private val uuidGenerator: UUIDGenerator) {
    
    fun generate(config: ProjectConfig, templateContent: String): String {
        val uuidPattern = Regex("[0-9A-F]{24}")
        val foundUuids = uuidPattern.findAll(templateContent)
            .map { it.value }
            .toSet()
        
        val uuidMapping = foundUuids.associateWith { uuidGenerator.generate() }
        
        var result = templateContent
        uuidMapping.forEach { (oldUuid, newUuid) ->
            result = result.replace(oldUuid, newUuid)
        }
        
        result = result.replace("\$PROJECT_NAME\$", config.projectName)
        
        return result
    }
}
