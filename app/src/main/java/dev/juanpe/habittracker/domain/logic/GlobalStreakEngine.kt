package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.StreakState

/**
 * Motor de la racha global de "días completos" y de los comodines.
 *
 * Reglas:
 *  - Un día completo suma 1 a la racha.
 *  - Cada [DAYS_PER_FREEZE] días completos seguidos se gana un comodín (máximo [MAX_FREEZES]).
 *  - Si un día se falla y hay racha y comodín, el comodín se gasta y la racha se conserva.
 *  - Si no, la racha vuelve a cero... y completar el siguiente día cuenta como "regreso".
 *  - Un día de descanso (sin nada programado) no cambia nada.
 *
 * Es una función pura: recibe un estado y devuelve otro, sin tocar la base de datos.
 */
object GlobalStreakEngine {

    const val DAYS_PER_FREEZE = 7
    const val MAX_FREEZES = 2

    data class Result(
        val state: StreakState,
        /** Estado final del día: un MISSED puede convertirse en FROZEN. */
        val status: DayStatus,
        val freezeEarned: Boolean = false,
    )

    /** Aplica el resultado de un día ([status] = COMPLETE, MISSED o REST) al estado. */
    fun applyDay(state: StreakState, status: DayStatus): Result = when (status) {
        DayStatus.COMPLETE -> complete(state)
        DayStatus.MISSED -> miss(state)
        DayStatus.REST, DayStatus.FROZEN -> Result(state, status)
    }

    private fun complete(state: StreakState): Result {
        val current = state.current + 1
        var freezes = state.freezes
        var progress = state.freezeProgress + 1
        var earned = false
        if (progress >= DAYS_PER_FREEZE) {
            progress = 0
            if (freezes < MAX_FREEZES) {
                freezes++
                earned = true
            }
        }
        val newState = state.copy(
            current = current,
            best = maxOf(state.best, current),
            freezes = freezes,
            freezeProgress = progress,
            comebacks = if (state.pendingComeback) state.comebacks + 1 else state.comebacks,
            pendingComeback = false,
        )
        return Result(newState, DayStatus.COMPLETE, earned)
    }

    private fun miss(state: StreakState): Result {
        val canProtect = state.current > 0 && state.freezes > 0
        return if (canProtect) {
            val newState = state.copy(freezes = state.freezes - 1, freezesUsed = state.freezesUsed + 1)
            Result(newState, DayStatus.FROZEN)
        } else {
            val newState = state.copy(current = 0, freezeProgress = 0, pendingComeback = true)
            Result(newState, DayStatus.MISSED)
        }
    }
}
