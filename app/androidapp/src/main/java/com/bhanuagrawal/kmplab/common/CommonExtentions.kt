package com.bhanuagrawal.kmplab.common

fun <T, R>Result<T>.map(transform: (T) -> R): Result<R> {
        return if(this.isSuccess && this.getOrNull() != null){
            Result.success(transform(this.getOrNull()!!))
        }else{
            Result.failure(this.exceptionOrNull()?: Exception("Something went Wrong"))
        }
}