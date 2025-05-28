package dev.openfeature.kotlin.contrib.providers.envvar

/**
 * This class provides a way to transform any given key to another value. This is helpful, if keys in the code have a
 * different representation as in the actual environment, e.g. SCREAMING_SNAKE_CASE env vars vs. hyphen-case keys
 * for feature flags.
 *
 *
 * This class also supports chaining/combining different transformers incl. self-written ones by providing
 * a transforming function in the constructor. <br></br>
 * Currently, the following transformations are supported out of the box:
 *
 *  * [converting to lower case][.toLowerCaseTransformer]
 *  * [converting to UPPER CASE][.toUpperCaseTransformer]
 *  * [converting hyphen-case to SCREAMING_SNAKE_CASE][.hyphenCaseToScreamingSnake]
 *  * [convert to camelCase][.toCamelCaseTransformer]
 *  * [replace &#39;_&#39; with &#39;.&#39;][.replaceUnderscoreWithDotTransformer]
 *  * [replace &#39;.&#39; with &#39;_&#39;][.replaceDotWithUnderscoreTransformer]
 *
 *
 *
 * **Examples:**
 *
 *
 * 1. hyphen-case feature flag names to screaming snake-case environment variables:
 * <pre>
 * `// Definition of the EnvVarProvider:
 * EnvironmentKeyTransformer transformer = EnvironmentKeyTransformer
 * .hyphenCaseToScreamingSnake();
 *
 * FeatureProvider provider = new EnvVarProvider(transformer);
` *
</pre> *
 * 2. chained/composed transformations:
 * <pre>
 * `// Definition of the EnvVarProvider:
 * EnvironmentKeyTransformer transformer = EnvironmentKeyTransformer
 * .toLowerCaseTransformer()
 * .andThen(EnvironmentKeyTransformer.replaceUnderscoreWithDotTransformer());
 *
 * FeatureProvider provider = new EnvVarProvider(transformer);
` *
</pre> *
 * 3. freely defined transformation function:
 * <pre>
 * `// Definition of the EnvVarProvider:
 * EnvironmentKeyTransformer transformer = new EnvironmentKeyTransformer(key -> "constant");
 *
 * FeatureProvider provider = new EnvVarProvider(keyTransformer);
` *
</pre> *
 */
fun interface EnvironmentKeyTransformer {
    fun transformKey(key: String): String

    fun andThen(another: EnvironmentKeyTransformer): EnvironmentKeyTransformer =
        EnvironmentKeyTransformer { key ->
            another.transformKey(
                this.transformKey(key),
            )
        }

    companion object {
        fun toLowerCaseTransformer(): EnvironmentKeyTransformer = EnvironmentKeyTransformer { key -> key.lowercase() }

        fun toUpperCaseTransformer(): EnvironmentKeyTransformer = EnvironmentKeyTransformer { key -> key.uppercase() }

        fun toCamelCaseTransformer(): EnvironmentKeyTransformer = EnvironmentKeyTransformer { key -> key.camelCase() }

        fun replaceUnderscoreWithDotTransformer(): EnvironmentKeyTransformer = EnvironmentKeyTransformer { key -> key.replace('_', '.') }

        fun replaceDotWithUnderscoreTransformer(): EnvironmentKeyTransformer = EnvironmentKeyTransformer { key -> key.replace('.', '_') }

        fun hyphenCaseToScreamingSnake(): EnvironmentKeyTransformer =
            EnvironmentKeyTransformer { key ->
                key.replace('-', '_').uppercase()
            }

        fun doNothing(): EnvironmentKeyTransformer = EnvironmentKeyTransformer { s -> s }
    }
}
