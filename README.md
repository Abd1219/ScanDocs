# ScanDocs — Escáner Profesional de Documentos para Android

**ScanDocs** es una aplicación Android nativa para escanear documentos con la cámara del dispositivo o importarlos desde la galería. Exporta en **PDF** y **JPEG**, almacena el historial localmente con **Room** y expone una UI moderna en **Jetpack Compose + Material Design 3**.

> Para documentación técnica profunda, consulta [`ARCHITECTURE.md`](ARCHITECTURE.md) y [`docs/CODEBASE_MAP.md`](docs/CODEBASE_MAP.md).

---

## Índice

- [Características](#-características)
- [Stack tecnológico](#-stack-tecnológico)
- [Arquitectura](#-arquitectura)
- [Estructura de archivos](#-estructura-de-archivos)
- [Base de datos](#-base-de-datos)
- [Requisitos del sistema](#-requisitos-del-sistema)
- [Compilación y ejecución](#-compilación-y-ejecución)
- [Estado del proyecto](#-estado-del-proyecto)

---

## ✨ Características

| Función | Descripción |
|---|---|
| **Escáner ML Kit** | Detección automática de bordes, corrección de perspectiva, filtros — hasta 10 páginas por sesión |
| **Importar desde galería** | El propio escáner de ML Kit permite elegir imágenes de la galería |
| **Exportación dual** | Genera simultáneamente un PDF multipágina y un JPEG por página |
| **Historial completo** | Pantalla dedicada con búsqueda en tiempo real (filtrado en memoria) |
| **Compartición segura** | `FileProvider` + `content://` — compatible con cualquier app externa |
| **Persistencia local** | Room + Kotlin Flows reactivos |
| **Tema oscuro / claro** | Material 3 con paleta carbón + esmeralda/teal |

---

## 🛠 Stack tecnológico

| Categoría | Librería / Tecnología | Versión mínima |
|---|---|---|
| UI | Jetpack Compose + Material 3 | BOM actual |
| Escáner | Google ML Kit Document Scanner (`play-services-mlkit-document-scanner`) | — |
| Base de datos | Room (SQLite reactivo) | — |
| Carga de imágenes | Coil Compose | — |
| Navegación | AndroidX Navigation Compose | — |
| DI | Manual (sin Hilt) | — |
| Lenguaje | Kotlin + Coroutines + StateFlow/Flow | — |
| Build | Gradle KTS + KSP | — |
| SDK mínimo | **API 24** (Android 7.0) | — |
| SDK objetivo | API 36 | — |
| JVM target | 11 | — |

---

## 🏗 Arquitectura

El proyecto sigue **MVVM** con separación estricta de capas:

```
UI Layer          → Composables (Screens + Components)
ViewModel Layer   → ScannerViewModel (StateFlow, lógica de negocio)
Repository Layer  → DocumentRepository (abstracción de la BD)
Data Layer        → Room (AppDatabase, DocumentDao, DocumentEntity)
Service Layer     → FileService (I/O de archivos), DocumentScanner (ML Kit wrapper)
```

Flujo de datos principal:

```
Usuario toca FAB
  → ScannerViewModel.startScanning()
    → DocumentScanner.startScan() [lanza ML Kit UI]
      → handleScanResult() guarda en uiState
        → SaveDocumentDialog (usuario edita nombre)
          → saveDocument() → FileService escribe JPG/PDF
            → DocumentRepository.insertDocument()
              → Room emite Flow → UI re-compone
```

Ver diagrama detallado en [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## 📁 Estructura de archivos

```
app/src/main/java/com/abdapps/scandocs/
├── MainActivity.kt              # Actividad principal, NavHost, NavigationBar
├── ScannerViewModel.kt          # ViewModel central: StateFlow, lógica de escaneo/guardado/borrado
├── DocumentScanner.kt           # Wrapper de GmsDocumentScanner (ML Kit)
├── PermissionHelper.kt          # Utilidades de permisos (actualmente no instanciado activamente)
│
├── data/
│   ├── entity/
│   │   ├── DocumentEntity.kt        # Entidad ACTIVA — tabla `documents`
│   │   └── ScannedDocument.kt       # Entidad LEGACY — sin usar en la app actual
│   ├── dao/
│   │   ├── DocumentDao.kt           # DAO ACTIVO — consultas con Flow
│   │   └── ScannedDocumentDao.kt    # DAO LEGACY — sin usar
│   ├── database/
│   │   ├── AppDatabase.kt           # BD ACTIVA — Room singleton
│   │   ├── ScanDocsDatabase.kt      # BD LEGACY — sin usar
│   │   └── Converters.kt            # Conversor Date↔Long (no vinculado a ninguna @Database)
│   ├── repository/
│   │   ├── DocumentRepository.kt        # Repositorio ACTIVO
│   │   └── ScannedDocumentRepository.kt # Repositorio LEGACY con anotaciones Hilt muertas
│   └── converter/
│       ├── DateTimeConverters.kt    # Conversor ACTIVO: Date↔Long, LocalDateTime↔String
│       └── Converters.kt            # Conversor LEGACY para ScanDocsDatabase
│
├── service/
│   └── FileService.kt           # I/O de archivos, FileProvider, intents de vista/compartición
│
└── ui/
    ├── components/
    │   ├── DocumentScannerScreen.kt # Pantalla Home (ruta "home")
    │   ├── HomeComponents.kt        # Bloques reutilizables del Home
    │   ├── HistoryScreen.kt         # Pantalla historial (ruta "history") + diálogos compartidos
    │   ├── DocumentItemCard.kt      # Tarjeta de documento reutilizable
    │   ├── ScannerComponents.kt     # Placeholder vacío (compatibilidad de build)
    │   └── FileNameDialog.kt        # Placeholder vacío (compatibilidad de build)
    └── theme/
        ├── Color.kt                 # Paleta completa Material 3 (claro + oscuro)
        ├── Theme.kt                 # ColorSchemes: ScanDocsDarkColorScheme / ScanDocsLightColorScheme
        └── Type.kt                  # Tipografía de la app
```

---

## 🗄 Base de datos

**Base de datos activa:** `scandocs_database` (Room, `AppDatabase`)

Tabla: **`documents`**

| Columna | Tipo SQLite | Notas |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | |
| `name` | TEXT | Nombre dado por el usuario |
| `jpgPath` | TEXT | Ruta absoluta al primer JPEG |
| `pdfPath` | TEXT | Ruta absoluta al PDF |
| `createdAt` | INTEGER | Epoch ms vía `DateTimeConverters` |
| `thumbnailPath` | TEXT (nullable) | Actualmente igual a `jpgPath` (stub) |

> Existe una segunda pila de datos legacy (`ScanDocsDatabase` / `ScannedDocument`) que **no está conectada** a la app activa. Ver [`ARCHITECTURE.md`](ARCHITECTURE.md#código-legacy-y-deuda-técnica).

---

## 📋 Requisitos del sistema

- **Android 7.0+** (API 24)
- **Google Play Services** instalado y actualizado
- **Permisos en runtime:**
  - `CAMERA`
  - `READ_EXTERNAL_STORAGE` (API ≤ 32) / `READ_MEDIA_IMAGES` (API 33+)
- `WRITE_EXTERNAL_STORAGE` declarado en Manifest con `maxSdkVersion=28` (API 29+ usa almacenamiento delimitado)

---

## 🚀 Compilación y ejecución

```bash
# Debug
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug

# Release (minify desactivado actualmente)
./gradlew assembleRelease
```

**Namespace:** `com.abdapps.scandocs`  
**Application ID:** `com.abdapps.scandocs`  
**FileProvider authority:** `com.abdapps.scandocs.fileprovider`

---

## 📊 Estado del proyecto

| Área | Estado |
|---|---|
| Escáner ML Kit | ✅ Funcional |
| Guardado PDF + JPEG | ✅ Funcional |
| Historial con búsqueda | ✅ Funcional |
| Compartición / Vista de archivos | ✅ Funcional |
| Eliminación de documentos | ✅ Funcional |
| Thumbnails reales | ⚠️ Stub — devuelve el path del JPG original |
| Código legacy (segunda pila BD) | ⚠️ Presente pero inactivo |
| Inyección de dependencias | ⚠️ Manual (sin Hilt) |
| Strings en recursos XML | ❌ Hardcodeadas en Composables |
| Tests unitarios | ❌ Sin implementar |

---

## 📄 Licencia

MIT — consulta el archivo `LICENSE` para más información.
