package com.appdoctor.network.model

/** Immutable URL query parameter entry. */
public data class QueryParameter(
    public val name: String,
    public val value: String?,
)
