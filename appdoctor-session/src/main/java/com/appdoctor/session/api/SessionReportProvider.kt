package com.appdoctor.session.api

import com.appdoctor.session.model.SessionReport

public interface SessionReportProvider {
    public suspend fun buildReport(): SessionReport
}
