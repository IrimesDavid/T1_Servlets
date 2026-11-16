package com.csvplatform.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a .properties file from the classpath and resolves ${NAME:default} placeholders,
 * similar to docker-compose's ${NAME:-default} or Spring's ${NAME:default}.
 * The value of NAME is looked up in the environment, then in JVM system properties;
 * if neither is set (or blank), the default after the colon is used.
 */
public final class PropertiesLoader {

    // ${NAME} or ${NAME:default}; the default may itself contain colons (URLs, C:/paths)
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    private PropertiesLoader() {
    }

    /** Loads the resource (e.g. "application.properties") resolving placeholders from env / system properties. */
    public static Properties load(String resource) {
        return load(resource, PropertiesLoader::envOrProperty);
    }

    /** Same, with a custom lookup (used by tests). */
    public static Properties load(String resource, Function<String, String> lookup) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = PropertiesLoader.class.getClassLoader();
        }
        Properties raw = new Properties();
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found on classpath: " + resource);
            }
            raw.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
        Properties resolved = new Properties();
        for (String name : raw.stringPropertyNames()) {
            resolved.setProperty(name, resolve(raw.getProperty(name), lookup));
        }
        return resolved;
    }

    /** Replaces every ${NAME:default} in value. Throws if a placeholder has no value and no default. */
    static String resolve(String value, Function<String, String> lookup) {
        Matcher m = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String found = lookup.apply(m.group(1));
            if (found == null || found.isBlank()) {
                found = m.group(2);
            }
            if (found == null) {
                throw new IllegalStateException(
                        "No value for ${" + m.group(1) + "} and no default was given");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(found.trim()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String envOrProperty(String key) {
        String value = System.getenv(key);
        return (value != null) ? value : System.getProperty(key);
    }
}