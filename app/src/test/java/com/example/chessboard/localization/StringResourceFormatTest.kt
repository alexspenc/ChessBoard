package com.example.chessboard.localization

/*
 * Unit tests for localized Android string resource formatting.
 * Keep XML resource parsing and format-placeholder validation here.
 * Do not add UI assertions, Android runtime dependencies, or translation wording checks to this file.
 * Validation date: 2026-06-04
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class StringResourceFormatTest {

    @Test
    fun `localized format strings can be formatted with dummy arguments`() {
        // Guards against malformed Android format placeholders, such as %1 instead of %1$d.
        // The test loads every values*/strings.xml file, so new locales are covered automatically.
        val failures = localizedResourceTexts()
            .filter { resource -> resource.value.contains("%") }
            .mapNotNull { resource -> resource.formatFailure() }

        assertTrue(
            failures.joinToString(separator = "\n"),
            failures.isEmpty(),
        )
    }

    @Test
    fun `localized strings keep the same format arguments as default strings`() {
        // A translation may reorder or reword text, but a resource with the same name and plural
        // quantity must keep the same argument types as the default locale. Plural quantities are
        // compared separately because Android formats only the selected quantity branch.
        val defaultResources = resourceTexts(DefaultStringsPath).associateBy { resource -> resource.key }
        val localizedResources = localizedStringsPaths()
            .filter { path -> path != DefaultStringsPath }
            .flatMap { path -> resourceTexts(path) }
        val failures = localizedResources.mapNotNull { localizedResource ->
            val defaultResource = defaultResources[localizedResource.key] ?: return@mapNotNull null
            val defaultSignature = defaultResource.formatSignature()
            val localizedSignature = localizedResource.formatSignature()
            if (defaultSignature == localizedSignature) {
                return@mapNotNull null
            }

            "${localizedResource.label}: expected $defaultSignature, actual $localizedSignature"
        }

        assertEquals(failures.joinToString(separator = "\n"), emptyList<String>(), failures)
    }

    private fun LocalizedResourceText.formatFailure(): String? {
        return try {
            String.format(Locale.ROOT, value, *dummyFormatArguments())
            null
        } catch (error: RuntimeException) {
            "$label: ${error::class.java.simpleName}: ${error.message}"
        }
    }

    private fun LocalizedResourceText.dummyFormatArguments(): Array<Any> {
        val specs = formatSpecs()
        if (specs.isEmpty()) {
            return emptyArray()
        }
        val indexedSpecs = specs.filter { spec -> spec.index != null }
        if (indexedSpecs.isNotEmpty()) {
            val maxIndex = indexedSpecs.maxOf { spec -> spec.index ?: 0 }
            val args = Array<Any>(maxIndex) { 1 }
            indexedSpecs.forEach { spec -> args[(spec.index ?: return@forEach) - 1] = spec.dummyArgument() }
            return args
        }

        return specs.map { spec -> spec.dummyArgument() }.toTypedArray()
    }

    private fun LocalizedResourceText.formatSignature(): List<FormatSignatureItem> {
        return formatSpecs().mapIndexed { sequenceIndex, spec ->
            FormatSignatureItem(
                index = spec.index ?: sequenceIndex + 1,
                conversion = spec.normalizedConversion,
            )
        }
    }

    private fun LocalizedResourceText.formatSpecs(): List<FormatSpec> {
        return FormatSpecRegex.findAll(value).mapNotNull { matchResult ->
            val conversion = matchResult.groupValues[5]
            if (conversion == "%" || conversion == "n") {
                return@mapNotNull null
            }

            val index = matchResult.groupValues[1].takeIf { group -> group.isNotBlank() }?.toInt()
            FormatSpec(index = index, conversion = conversion)
        }.toList()
    }

    private fun FormatSpec.dummyArgument(): Any {
        return when (normalizedConversion) {
            "d", "o", "x" -> 1
            "e", "f", "g", "a" -> 1.0
            "b" -> true
            "c" -> "a"
            "s", "h" -> "text"
            else -> "text"
        }
    }

    private fun localizedResourceTexts(): List<LocalizedResourceText> {
        return localizedStringsPaths().flatMap { path -> resourceTexts(path) }
    }

    private fun localizedStringsPaths(): List<String> {
        val resDirectory = resolveProjectFile(MainResPath)
        return resDirectory
            .listFiles { file -> file.isDirectory && file.name.startsWith("values") }
            .orEmpty()
            .mapNotNull { directory ->
                val stringsFile = File(directory, "strings.xml")
                if (!stringsFile.isFile) {
                    return@mapNotNull null
                }

                stringsFile.toRelativeString(resolveModuleDirectory(resDirectory))
            }
            .sortedWith(compareBy<String> { path -> path != DefaultStringsPath }.thenBy { path -> path })
    }

    private fun resolveModuleDirectory(resDirectory: File): File {
        return resDirectory.parentFile.parentFile.parentFile
    }

    private fun resourceTexts(resourcePath: String): List<LocalizedResourceText> {
        val file = resolveProjectFile(resourcePath)
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val document = documentBuilderFactory.newDocumentBuilder().parse(file)
        val root = document.documentElement
        val resources = mutableListOf<LocalizedResourceText>()
        val children = root.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index) as? Element ?: continue
            when (child.tagName) {
                "string" -> resources.add(child.stringResourceText(resourcePath))
                "plurals" -> resources.addAll(child.pluralResourceTexts(resourcePath))
            }
        }

        return resources
    }

    private fun Element.stringResourceText(resourcePath: String): LocalizedResourceText {
        return LocalizedResourceText(
            source = resourcePath,
            name = getAttribute("name"),
            quantity = null,
            value = textContent,
        )
    }

    private fun Element.pluralResourceTexts(resourcePath: String): List<LocalizedResourceText> {
        val pluralName = getAttribute("name")
        val resources = mutableListOf<LocalizedResourceText>()
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index) as? Element ?: continue
            if (child.tagName != "item") {
                continue
            }

            resources.add(
                LocalizedResourceText(
                    source = resourcePath,
                    name = pluralName,
                    quantity = child.getAttribute("quantity"),
                    value = child.textContent,
                )
            )
        }

        return resources
    }

    private fun resolveProjectFile(path: String): File {
        val startDirectory = File(".").absoluteFile
        var directory: File? = startDirectory
        while (directory != null) {
            val directCandidate = File(directory, path)
            if (directCandidate.exists()) {
                return directCandidate
            }

            val appCandidate = File(directory, "app/$path")
            if (appCandidate.exists()) {
                return appCandidate
            }

            directory = directory.parentFile
        }

        error("Could not find $path from ${startDirectory.absolutePath}")
    }

    private data class LocalizedResourceText(
        val source: String,
        val name: String,
        val quantity: String?,
        val value: String,
    ) {
        val key: String = if (quantity == null) name else "$name[$quantity]"
        val label: String = if (quantity == null) "$source:$name" else "$source:$name[$quantity]"
    }

    private data class FormatSpec(
        val index: Int?,
        val conversion: String,
    ) {
        val normalizedConversion: String = conversion.lowercase(Locale.ROOT)
    }

    private data class FormatSignatureItem(
        val index: Int,
        val conversion: String,
    )

    private companion object {
        private const val MainResPath = "src/main/res"
        private const val DefaultStringsPath = "src/main/res/values/strings.xml"
        private val FormatSpecRegex = Regex("%(?:(\\d+)\\$)?[-#+ 0,(<]*(\\d+)?(?:\\.(\\d+))?([tT])?([a-zA-Z%])")
    }
}
