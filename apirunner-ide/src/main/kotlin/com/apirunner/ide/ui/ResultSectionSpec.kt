package com.apirunner.ide.ui

data class ResultSectionSpec(
    val key: String,
    val title: String,
    val maxLength: Int,
    val extractor: (ResultViewModel) -> String,
    val formatter: (String) -> String = { it }
)
