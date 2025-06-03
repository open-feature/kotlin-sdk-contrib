package dev.openfeature.kotlin.contrib.providers.envvar

import kotlin.test.Test
import kotlin.test.assertEquals

internal class EnvironmentKeyTransformerTest {
    @Test
    fun `should transform keys to lower case prior delegating call to actual gateway`() {
        listOf(
            "" to "",
            "a" to "a",
            "A" to "a",
            "ABC_DEF_GHI" to "abc_def_ghi",
            "ABC.DEF.GHI" to "abc.def.ghi",
            "aBc" to "abc",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase

            val actual = EnvironmentKeyTransformer.toLowerCaseTransformer().transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }

    @Test
    fun `should transform keys to upper case prior delegating call to actual gateway`() {
        listOf(
            "" to "",
            "a" to "A",
            "A" to "A",
            "ABC_DEF_GHI" to "ABC_DEF_GHI",
            "ABC.DEF.GHI" to "ABC.DEF.GHI",
            "aBc" to "ABC",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase

            val actual = EnvironmentKeyTransformer.toUpperCaseTransformer().transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }

    @Test
    fun `should not transform key when using doNothing transformer`() {
        listOf(
            "" to "",
            "a" to "a",
            "A" to "A",
            "ABC_DEF_GHI" to "ABC_DEF_GHI",
            "ABC.DEF.GHI" to "ABC.DEF.GHI",
            "aBc" to "aBc",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase
            val actual = EnvironmentKeyTransformer.doNothing().transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }

    @Test
    fun `should transform keys to camel case prior delegating call to actual gateway`() {
        listOf(
            "" to "",
            "a" to "a",
            "A" to "a",
            "ABC_DEF_GHI" to "abcDefGhi",
            "ABC.DEF.GHI" to "abc.def.ghi",
            "aBc" to "abc",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase
            val transformingGateway =
                EnvironmentKeyTransformer.toCamelCaseTransformer()
            val actual = transformingGateway.transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }

    @Test
    fun `should transform keys according to given transformation prior delegating call to actual gateway`() {
        listOf(
            "" to "_",
            "a" to "_a",
            "A" to "_A",
            "ABC_DEF_GHI" to "_ABC_DEF_GHI",
            "ABC.DEF.GHI" to "_ABC.DEF.GHI",
            "aBc_abc" to "_aBc_abc",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase
            val transformingGateway =
                EnvironmentKeyTransformer { "_$it" }

            val actual = transformingGateway.transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }

    @Test
    fun `should compose transformers`() {
        listOf(
            "" to "",
            "ABC_DEF_GHI" to "abc.def.ghi",
            "ABC.DEF.GHI" to "abc.def.ghi",
            "aBc" to "abc",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase
            val transformingGateway =
                EnvironmentKeyTransformer
                    .toLowerCaseTransformer()
                    .andThen(EnvironmentKeyTransformer.replaceUnderscoreWithDotTransformer())

            val actual = transformingGateway.transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }

    @Test
    fun `should support screaming snake to hyphen case keys`() {
        listOf(
            "" to "",
            "abc-def-ghi" to "ABC_DEF_GHI",
            "abc.def.ghi" to "ABC.DEF.GHI",
            "abc" to "ABC",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase
            val transformingGateway =
                EnvironmentKeyTransformer.hyphenCaseToScreamingSnake()

            val actual = transformingGateway.transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }

    @Test
    fun `should support replacing dot with underscore`() {
        listOf(
            "" to "",
            "abc-def-ghi" to "abc-def-ghi",
            "abc.def.ghi" to "abc_def_ghi",
            "abc" to "abc",
        ).forEach { testCase ->
            val (originalKey, expectedTransformedKey) = testCase
            val transformingGateway =
                EnvironmentKeyTransformer.replaceDotWithUnderscoreTransformer()

            val actual = transformingGateway.transformKey(originalKey)

            assertEquals(expectedTransformedKey, actual)
        }
    }
}
