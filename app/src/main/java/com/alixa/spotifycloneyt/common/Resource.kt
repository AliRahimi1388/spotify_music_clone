package com.alixa.spotifycloneyt.common

data class Resource<out T>(
    val status: Status,
    val data: T?,
    val message: String?
) {

    companion object {
        fun <T> success(data: T?) = Resource(status = Status.SUCCESS, data, null)
        fun <T> error(data: T?, message: String?) = Resource(status = Status.ERROR, data, message)
        fun <T> loading(data: T?) = Resource(status = Status.LOADING, data, null)

    }
}

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}