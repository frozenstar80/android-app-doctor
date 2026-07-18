package com.appdoctor.network.model

/** Immutable HTTP header entry. */
public data class HttpHeader(
    public val name: String,
    public val value: String,
)
