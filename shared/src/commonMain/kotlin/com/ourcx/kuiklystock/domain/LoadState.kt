package com.ourcx.kuiklystock.domain

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Content<T>(val value: T) : LoadState<T>

    data object Empty : LoadState<Nothing>

    data class Error(val message: String) : LoadState<Nothing>
}
