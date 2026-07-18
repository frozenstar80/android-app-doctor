package com.appdoctor.network.model

/** Immutable captured network transaction. */
public data class NetworkRequestRecord(
    public val id: Long,
    public val timestampMillis: Long,
    public val method: String,
    public val url: String,
    public val queryParameters: List<QueryParameter>,
    public val requestHeaders: List<HttpHeader>,
    public val requestBody: CapturedBody?,
    public val statusCode: Int?,
    public val responseHeaders: List<HttpHeader>,
    public val responseBody: CapturedBody?,
    public val responseTimeMillis: Long,
    public val contentLength: Long?,
    public val success: Boolean,
    public val failureMessage: String?,
)
