package io.github.barsia.composewizard.shared.services

import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SafeXmlParserTest {

    @Test
    fun `safe parser reads simple maven metadata`() {
        val xml = """
            <metadata>
              <versioning>
                <versions>
                  <version>1.0.0</version>
                  <version>1.1.0</version>
                </versions>
              </versioning>
            </metadata>
        """.trimIndent()

        val document = SafeXmlParser.parse(ByteArrayInputStream(xml.toByteArray()))
        val versions = document.getElementsByTagName("version")

        assertEquals(2, versions.length)
        assertEquals("1.0.0", versions.item(0).textContent)
        assertEquals("1.1.0", versions.item(1).textContent)
    }

    @Test
    fun `safe parser rejects doctype declarations`() {
        val xml = """
            <!DOCTYPE foo [
              <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <metadata>
              <versioning>
                <versions>
                  <version>&xxe;</version>
                </versions>
              </versioning>
            </metadata>
        """.trimIndent()

        assertFailsWith<Exception> {
            SafeXmlParser.parse(ByteArrayInputStream(xml.toByteArray()))
        }
    }
}
