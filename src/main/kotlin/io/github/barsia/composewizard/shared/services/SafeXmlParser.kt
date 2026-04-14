package io.github.barsia.composewizard.shared.services

import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

object SafeXmlParser {

    fun parse(input: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }

        val builder = factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> org.xml.sax.InputSource(java.io.StringReader("")) }
        }

        return builder.parse(input).also { it.documentElement.normalize() }
    }
}
