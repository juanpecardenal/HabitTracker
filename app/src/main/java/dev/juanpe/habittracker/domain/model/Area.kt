package dev.juanpe.habittracker.domain.model

/**
 * Las cuatro áreas de mejora de la app.
 *
 * El dominio solo conoce el identificador; el color, el icono y el nombre
 * visible de cada área viven en la capa de UI (ver `ui/theme/AreaStyle.kt`).
 */
enum class Area {
    DIET,
    TRAINING,
    STUDY,
    RELATIONS,
}
