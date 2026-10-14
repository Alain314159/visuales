# Visuales

Cliente Android nativo para el catálogo de [visuales.uclv.cu](https://visuales.uclv.cu), la plataforma de contenidos audiovisuales de la Universidad Central "Marta Abreu" de Las Villas (Cuba).

> Reescritura nativa en Kotlin de la versión anterior (Vue 3 + Capacitor). Esta versión elimina el WebView y los plugins de terceros, sustituyéndolos por una arquitectura puramente nativa.

## ✨ Características

- **Catálogo offline-first** — Sincronización periódica con `listado.html` del servidor. Las 30.000 carpetas del catálogo se indexan localmente (Room + FTS) para navegación y búsqueda instantánea sin red.
- **Lazy scraping con caché** — El contenido de cada carpeta se descarga bajo demanda con caché TTL de 24 h. No se descarga nada que el usuario no haya visto.
- **Motor de descargas propio** — Multi-hilo (OkHttp Range + RandomAccessFile), pausa/reanuda, cola con prioridades, reanudación tras reboot.
- **Reproductor integrado** (v1.5) — Media3 ExoPlayer con subtítulos, gestos, PiP y resume.
- **Biblioteca local** — Escanea MediaStore + carpeta elegida. Watch progress, marca como visto, etc.
- **Material 3 dinámico** — Compose + Material You (Android 12+).
- **Cero telemetría** — Sin Firebase Analytics, sin Crashlytics, sin Ads.
- **Código abierto** — MIT.

## 🏗️ Arquitectura

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| BD | Room (SQLite) |
| Background | WorkManager |
| HTTP | OkHttp + Retrofit |
| HTML | Jsoup |
| Reproductor | Media3 ExoPlayer |
| Imágenes | Coil |

### Modularización

```
:app                    → entry point, nav, DI
:core:common            → constantes, formato, tipos
:core:designsystem      → Material 3 theme + componentes
:core:network           → OkHttp, Retrofit, ListadoHtmlParser, NginxyListingScraper
:core:data             → Room, DAOs, repositorios
:sync:listado           → SyncWorker para listado.html
:feature:catalog        → catálogo, búsqueda, detalle
:feature:downloads      → cola de descargas
:feature:library        → biblioteca local
:feature:player         → ExoPlayer (v1.5)
:feature:settings       → ajustes
```

## 🚀 Desarrollo

Requisitos:
- Android Studio Ladybug+ (o Android SDK 35 + JDK 17)
- Kotlin 2.0.21
- Gradle 8.9

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew test

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## 🔐 Release builds

El keystore de release NO se commitea. Configurar así:

1. Generar keystore:
   ```bash
   keytool -genkeypair -v -keystore visuales-release.jks \
     -keyalg RSA -keysize 4096 -validity 10000 -alias visuales
   ```

2. Crear `app/signing.properties` (NO commiteado, en `.gitignore`):
   ```properties
   storeFile=/path/to/visuales-release.jks
   storePassword=******
   keyAlias=visuales
   keyPassword=******
   ```

3. Build release:
   ```bash
   ./gradlew :app:assembleRelease
   ```

## 📲 Distribución

- **GitHub Releases** — Automático via CI en cada tag `v*`.
- **No Apklis** por decisión del autor.

## 🔍 Estrategia de datos

1. **Capa 1 — Sincronización del árbol de carpetas (listado.html):**
   - WorkManager diario (configurable).
   - HEAD + ETag/Last-Modified comparison → skip si no cambió.
   - Si cambió: descargar HTML (~1.5 MB), parsear con Jsoup, diff contra DB.
   - Resultado: 30.000 carpetas indexadas localmente, navegables sin red.

2. **Capa 2 — Contenido por carpeta (lazy + cacheado):**
   - GET a la URL de la carpeta, scrapeo con NginxyListingScraper.
   - Caché TTL 24h en Room.
   - Refresh en background al abrir.

3. **Scraper Nginxy** (no Apache autoindex):
   - El servidor usa nginx-fancyindex + theme Nginxy.
   - Clasificación por extensión del nombre (NO por icono del HTML, ya que el theme no incluye `.mkv`).
   - Iconos disponibles en `/.nginxy/images/icons/` del servidor.

## 📜 Licencia

MIT — ver [LICENSE](LICENSE).

## 👤 Autor

Alain Cruz — [GitHub](https://github.com/Alain314159)
