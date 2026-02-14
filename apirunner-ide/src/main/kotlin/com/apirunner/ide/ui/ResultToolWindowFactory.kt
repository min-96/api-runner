package com.apirunner.ide.ui

class ResultToolWindowFactory : ResultSink {
    private var latest: ResultViewModel? = null

    override fun update(result: ResultViewModel) {
        latest = result
    }

    fun latestResult(): ResultViewModel? = latest
}
