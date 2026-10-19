package com.bhanuagrawal.kmplab.presentation.common

sealed class EvenResult<out S: ViewState, out E: SideEffect>{

    // TransformState declares its OWN invariant S (S.() -> S uses S in/out);
    // it passes that S into the parent's covariant slot, which is allowed.
    data class TransformState<S: ViewState>(val transform: S.() -> S): EvenResult<S, Nothing>()
    data class SideEffectEvent<E: SideEffect>(val sideEffect: E): EvenResult<Nothing, E>()
}

interface SideEffect

