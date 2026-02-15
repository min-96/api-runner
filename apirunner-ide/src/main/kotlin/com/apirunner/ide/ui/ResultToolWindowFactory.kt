package com.apirunner.ide.ui

@Deprecated(
    message = "Use InMemoryResultSink for tests and non-UI capture use cases.",
    replaceWith = ReplaceWith("InMemoryResultSink")
)
class ResultToolWindowFactory : InMemoryResultSink()
