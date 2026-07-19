package com.appdoctor.extension

/**
 * Base extension runtime exception.
 */
public class ExtensionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
