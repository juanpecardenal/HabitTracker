[README.md](https://github.com/user-attachments/files/35015048/README.md)
# Habit Tracker

Aplicación Android para crear hábitos y mantenerlos en el tiempo, con un sistema de **rachas, experiencia, niveles y logros** que convierte la constancia en un juego. Desarrollada en **Kotlin** con **Jetpack Compose** y **Material 3**, con todos los datos guardados en local mediante **Room**.

> Proyecto personal realizado durante el 2.º curso de Desarrollo de Aplicaciones Multiplataforma (DAM).

---

## ✨ Funcionalidades

### Hábitos
- Crear, editar, **archivar** (oculta el hábito pero conserva su historial) y borrar hábitos.
- Cuatro **áreas**: Dieta, Entrenamiento, Estudio y Relaciones.
- **Frecuencias** flexibles: cada día, días fijos de la semana (p. ej. de lunes a viernes o fines de semana) o *X veces por semana*.
- **Cantidad objetivo** opcional (minutos, repeticiones, vasos, páginas, veces): cada toque suma, y una pulsación larga permite anotar una cantidad parcial.
- Hábitos de ejemplo al abrir la app por primera vez.

### Pantalla «Hoy»
- Anillo de progreso con los hábitos del día y mensajes que cambian según cómo vayas.
- Los hábitos semanales solo pasan a ser obligatorios cuando ya no queda margen en la semana.
- Mensajes de ánimo si ayer no se completó el día o si un comodín salvó la racha.

### Gamificación
- **Experiencia (XP):** 10 XP por hábito cumplido, hasta +5 extra si está en racha.
- **Bonus de día completo:** 20 XP, que crece con la racha hasta 50.
- **Niveles** generales y por área.
- **Racha global** de días completos seguidos; los días sin nada programado no la rompen. La llama crece a los 3, 7, 14 y 30 días.
- **Comodines:** empiezas con uno y ganas otro cada 7 días completos seguidos (máximo 2). Si fallas un día, se gasta solo y la racha sigue viva.
- **26 logros** (primer paso, rachas de 7/14/30/100 días, 21 días con un mismo hábito, totales de 50/250/1000, logros por área y por nivel, «Equilibrio»…).
- **Celebraciones** animadas con confeti al completar el día, subir de nivel o desbloquear un logro.

### Progreso y estadísticas
- XP total y lo que falta para el siguiente nivel.
- Niveles por área, destacando dónde hay más margen para crecer.
- **Resumen semanal** comparado con la semana anterior.
- Detalle de cada hábito: racha actual y mejor racha, veces cumplido, porcentaje de cumplimiento, **calendario tipo heatmap** y gráfico de cumplimiento semanal.

### Notificaciones
- **Recordatorio por hábito** a la hora elegida, solo si aún no se ha hecho.
- **Aviso de racha en riesgo** por la tarde si quedan hábitos pendientes (hora configurable en Ajustes).
- Alarmas exactas y reprogramación automática al reiniciar el móvil, actualizar la app o cambiar la hora o la zona horaria.

---

## 🛠️ Tecnologías

| Ámbito | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.4 |
| Interfaz | Jetpack Compose · Material 3 · Material Icons Extended |
| Navegación | Navigation Compose |
| Estado | ViewModel + StateFlow (Lifecycle) |
| Persistencia | Room (con KSP para generar el código) |
| Notificaciones | AlarmManager · BroadcastReceiver · NotificationManager |
| Gráficos y animaciones | Canvas propio de Compose (sin librerías externas) |
| Tests | JUnit 4 · Robolectric |
| Build | Gradle 9.3 (Kotlin DSL) · Android Gradle Plugin 9.1 |

- **minSdk:** 26 (Android 8.0)
- **targetSdk:** 36
- **compileSdk:** 37

---

## 🏗️ Arquitectura

El proyecto sigue una arquitectura por capas inspirada en las recomendaciones oficiales de Android (**UI → dominio → datos**), con inyección de dependencias manual.

```
app/src/main/java/dev/juanpe/habittracker/
├── data/
│   ├── local/          # Room: base de datos, entidades, DAOs y mappers
│   ├── repository/     # Repositorios de hábitos y progreso
│   └── SeedData.kt     # Hábitos de ejemplo iniciales
├── di/
│   └── AppContainer.kt # Contenedor de dependencias (DI manual)
├── domain/
│   ├── model/          # Modelos: Habit, Area, Achievement, Progress
│   └── logic/          # Reglas de negocio puras: rachas, XP, calendario,
│                       # evaluación del día, resumen semanal, logros…
├── notifications/      # Programación y recepción de recordatorios
└── ui/
    ├── today/          # Pantalla «Hoy»
    ├── habits/         # Lista y edición de hábitos
    ├── detail/         # Detalle y gráficos de un hábito
    ├── progress/       # Pantalla de progreso
    ├── achievements/   # Pantalla de logros
    ├── celebration/    # Overlays de celebración
    ├── settings/       # Ajustes
    ├── components/     # Componentes reutilizables (anillo, llama, confeti…)
    ├── navigation/     # Grafo de navegación
    └── theme/          # Colores, tipografía (Nunito) y tema claro/oscuro
```

La lógica de negocio (`domain/logic`) es Kotlin puro y no depende de Android, lo que permite probarla con tests unitarios rápidos.

---

## 🚀 Cómo ejecutarlo

### Requisitos
- **Android Studio** en una versión reciente compatible con AGP 9.1.
- **JDK 17** o superior (el que trae Android Studio sirve).
- Un emulador o un móvil con **Android 8.0** o superior.

### Pasos
1. Clona el repositorio:
   ```bash
   git clone https://github.com/TU_USUARIO/HabitTracker.git
   ```
2. Ábrelo en Android Studio (**File → Open**) y espera a que termine la sincronización de Gradle. El archivo `local.properties` se genera solo.
3. Pulsa **Run ▶** con un emulador o un dispositivo conectado.

También se puede compilar desde la terminal:
```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
El APK queda en `app/build/outputs/apk/debug/`.

---

## 🧪 Tests

Los tests unitarios se ejecutan en la JVM, sin emulador:

```bash
./gradlew test        # o gradlew.bat test en Windows
```

Cubren la lógica principal de la app:
- `HabitStreakCalculatorTest`: rachas de cada hábito.
- `GlobalStreakEngineTest`: racha global y comodines.
- `ScheduleTest`: qué hábitos tocan cada día según su frecuencia.
- `WeeklySummaryCalculatorTest`: resumen y comparación semanal.
- `XpRulesTest`: cálculo de experiencia y niveles.
- `ProgressRepositoryTest`: repositorio de progreso contra una base de datos Room real (con Robolectric).

---

## 🔔 Permisos

| Permiso | Para qué se usa |
|---|---|
| `POST_NOTIFICATIONS` | Mostrar recordatorios (se pide en tiempo de ejecución en Android 13+). |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Que los recordatorios lleguen a la hora exacta elegida. |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar los recordatorios tras reiniciar el móvil. |

> Nota: publicar en Google Play con `USE_EXACT_ALARM` requiere justificarlo. Sin ese permiso la app sigue funcionando con alarmas inexactas.

---

## 👤 Autor

**Juan Pedro Cardenal Aranda** · Estudiante de 2.º de DAM
