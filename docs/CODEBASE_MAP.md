# CODEBASE_MAP.md — ScanDocs

Mapa completo de cada archivo del proyecto: propósito, estado (activo/legacy/placeholder) y dependencias clave. Diseñado para que una IA pueda orientarse rápidamente antes de editar cualquier archivo.

---

## Leyenda de estado

| Etiqueta | Significado |
|---|---|
| ✅ ACTIVO | Usado en la app en producción |
| ⚠️ LEGACY | Existe en el código, no está conectado a la app activa |
| 🔲 PLACEHOLDER | Archivo vacío dejado por compatibilidad de build |
| 📐 TEMA | Relacionado con el sistema de diseño visual |

---

## Raíz del proyecto

| Archivo | Propósito |
|---|---|
| [`build.gradle.kts`](../build.gradle.kts) | Configuración del proyecto raíz (plugins BOM) |
| [`settings.gradle.kts`](../settings.gradle.kts) | Nombre del proyecto + inclusión del módulo `app` |
| [`gradle.properties`](../gradle.properties) | Propiedades de Gradle (memoria, AndroidX, Kotlin Compose) |
| [`README.md`](../README.md) | Documentación principal para humanos e IAs |
| [`ARCHITECTURE.md`](../ARCHITECTURE.md) | Arquitectura técnica detallada |

## Módulo `app/`

| Archivo | Propósito |
|---|---|
| [`app/build.gradle.kts`](../app/build.gradle.kts) | Dependencias, SDK targets, opciones de compilación |
| [`app/proguard-rules.pro`](../app/proguard-rules.pro) | Reglas ProGuard (minify desactivado en release actualmente) |

---

## `app/src/main/`

### AndroidManifest

| Archivo | Estado | Notas |
|---|---|---|
| [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | ✅ ACTIVO | Permisos CAMERA, STORAGE, READ_MEDIA_IMAGES; declara FileProvider (`com.abdapps.scandocs.fileprovider`) y MainActivity |

---

### Capa raíz — `java/com/abdapps/scandocs/`

| Archivo | Estado | Responsabilidad | Dependencias clave |
|---|---|---|---|
| [`MainActivity.kt`](../app/src/main/java/com/abdapps/scandocs/MainActivity.kt) | ✅ ACTIVO | Única Activity; cablea DI manual; renderiza `MainScreen` con NavHost y `ScanDocsBottomBar` | `AppDatabase`, `DocumentRepository`, `FileService`, `ScannerViewModel`, Navigation Compose |
| [`ScannerViewModel.kt`](../app/src/main/java/com/abdapps/scandocs/ScannerViewModel.kt) | ✅ ACTIVO | ViewModel central; gestiona toda la lógica de negocio y estado UI | `DocumentRepository`, `FileService`, `DocumentScanner` |
| [`DocumentScanner.kt`](../app/src/main/java/com/abdapps/scandocs/DocumentScanner.kt) | ✅ ACTIVO | Wrapper de `GmsDocumentScanner`; gestiona el launcher de Activity Result | ML Kit Document Scanner |
| [`PermissionHelper.kt`](../app/src/main/java/com/abdapps/scandocs/PermissionHelper.kt) | ⚠️ LEGACY | Clase de utilidad para permisos de runtime; no instanciada activamente | Android permissions API |

---

### Capa de datos — `data/entity/`

| Archivo | Estado | Descripción |
|---|---|---|
| [`DocumentEntity.kt`](../app/src/main/java/com/abdapps/scandocs/data/entity/DocumentEntity.kt) | ✅ ACTIVO | Entidad Room para `documents`. `jpgPath` conserva la primera página y `jpgPaths` guarda como JSON todas las páginas JPG; `allJpgPaths` da compatibilidad a registros antiguos |
| [`ScannedDocument.kt`](../app/src/main/java/com/abdapps/scandocs/data/entity/ScannedDocument.kt) | ⚠️ LEGACY | Entidad más rica (`pageCount`, `fileSize`, `isFavorite`, `tags`) para la tabla `scanned_documents`. No conectada a la app activa |

---

### Capa de datos — `data/dao/`

| Archivo | Estado | Operaciones |
|---|---|---|
| [`DocumentDao.kt`](../app/src/main/java/com/abdapps/scandocs/data/dao/DocumentDao.kt) | ✅ ACTIVO | `getAllDocuments(): Flow<List<DocumentEntity>>`, `getAllDocumentsSnapshot`, `getDocumentById`, `insertDocument`, `updateDocument`, `deleteDocument`, `deleteDocumentById` |
| [`ScannedDocumentDao.kt`](../app/src/main/java/com/abdapps/scandocs/data/dao/ScannedDocumentDao.kt) | ⚠️ LEGACY | DAO completo para `scanned_documents`: CRUD + búsqueda + favoritos + filtro por fecha + estadísticas |

---

### Capa de datos — `data/database/`

| Archivo | Estado | Descripción |
|---|---|---|
| [`AppDatabase.kt`](../app/src/main/java/com/abdapps/scandocs/data/database/AppDatabase.kt) | ✅ ACTIVO | `RoomDatabase` singleton; entidades: `[DocumentEntity]`; TypeConverters: `DateTimeConverters`; archivo BD: `scandocs_database` |
| [`ScanDocsDatabase.kt`](../app/src/main/java/com/abdapps/scandocs/data/database/ScanDocsDatabase.kt) | ⚠️ LEGACY | `RoomDatabase` sin instanciar; entidades: `[ScannedDocument]`; **mismo nombre de archivo BD** que `AppDatabase` — colisionaría si se instanciara |
| [`Converters.kt`](../app/src/main/java/com/abdapps/scandocs/data/database/Converters.kt) | ⚠️ LEGACY | Conversor `Date ↔ Long` en el paquete `database`; no vinculado a ninguna `@Database` |

---

### Capa de datos — `data/repository/`

| Archivo | Estado | Descripción |
|---|---|---|
| [`DocumentRepository.kt`](../app/src/main/java/com/abdapps/scandocs/data/repository/DocumentRepository.kt) | ✅ ACTIVO | Thin wrapper sobre `DocumentDao`; expone `allDocuments: Flow<List<DocumentEntity>>` y operaciones CRUD suspend |
| [`ScannedDocumentRepository.kt`](../app/src/main/java/com/abdapps/scandocs/data/repository/ScannedDocumentRepository.kt) | ⚠️ LEGACY | Repositorio completo con anotaciones `@Singleton @Inject` (Hilt) muertas — Hilt no está configurado en el proyecto |

---

### Capa de datos — `data/converter/`

| Archivo | Estado | Conversiones |
|---|---|---|
| [`DateTimeConverters.kt`](../app/src/main/java/com/abdapps/scandocs/data/converter/DateTimeConverters.kt) | ✅ ACTIVO | `java.util.Date ↔ Long` (epoch ms) **[usado por DocumentEntity]** + `LocalDateTime ↔ String` ISO-8601 [declarado, no usado por entidades activas] |
| [`Converters.kt`](../app/src/main/java/com/abdapps/scandocs/data/converter/Converters.kt) | ⚠️ LEGACY | `LocalDateTime ↔ String` + `List<String> ↔ String` (tags separadas por coma) — para `ScanDocsDatabase` |

---

### Capa de servicio — `service/`

| Archivo | Estado | Descripción |
|---|---|---|
| [`FileService.kt`](../app/src/main/java/com/abdapps/scandocs/service/FileService.kt) | ✅ ACTIVO | I/O de archivos en `Documents/ScanDocsApp/`; renombra, crea intents de vista y compartición vía FileProvider; `createThumbnail()` es un stub |

**Métodos de `FileService`:**

```
saveJpgFile(imageUri, fileName)   → String  (@RequiresApi(O))
savePdfFile(pdfUri, fileName)     → String  (@RequiresApi(O))
deleteFile(filePath)              → Boolean
renameFile(filePath, newBaseName) → String?
getUriForFile(file)               → Uri?
createViewIntent(ctx, path, mime) → Intent?
createShareIntent(ctx, path, mime)→ Intent?
determineMimeTypeSafely(file)     → String?
createThumbnail(jpgPath)          → String? [STUB]
```

---

### Capa UI — `ui/components/`

| Archivo | Estado | Pantalla / Rol | Composables públicos |
|---|---|---|---|
| [`DocumentScannerScreen.kt`](../app/src/main/java/com/abdapps/scandocs/ui/components/DocumentScannerScreen.kt) | ✅ ACTIVO | Pantalla Home (ruta `"home"`); acciones Escanear y Documentos | `DocumentScannerScreen`, `SaveDocumentDialog` |
| [`HomeComponents.kt`](../app/src/main/java/com/abdapps/scandocs/ui/components/HomeComponents.kt) | ✅ ACTIVO | Bloques del Home; no duplica la importación, disponible en ML Kit tras tocar Escanear | `HomeSearchBar`, `QuickActionsSection`, `RecentSectionHeader`, `CompactEmptyRecentState` |
| [`HistoryScreen.kt`](../app/src/main/java/com/abdapps/scandocs/ui/components/HistoryScreen.kt) | ✅ ACTIVO | Pantalla historial (ruta `"history"`) + diálogos compartidos, incluido renombrado | `HistoryScreen`, `ChooseFileDialog`, `ConfirmDeleteDialog`, `RenameDocumentDialog`, `HistoryEmptyState`, `SearchEmptyState` |
| [`DocumentItemCard.kt`](../app/src/main/java/com/abdapps/scandocs/ui/components/DocumentItemCard.kt) | ✅ ACTIVO | Tarjeta reutilizable de documento | `DocumentItemCard` |
| [`ScannerComponents.kt`](../app/src/main/java/com/abdapps/scandocs/ui/components/ScannerComponents.kt) | 🔲 PLACEHOLDER | Archivo vacío (reemplazado por `DocumentScannerScreen.kt`) | — |
| [`FileNameDialog.kt`](../app/src/main/java/com/abdapps/scandocs/ui/components/FileNameDialog.kt) | 🔲 PLACEHOLDER | Archivo vacío (diálogo consolidado en `DocumentScannerScreen.kt`) | — |

---

### Capa UI — `ui/theme/`

| Archivo | Estado | Contenido |
|---|---|---|
| [`Color.kt`](../app/src/main/java/com/abdapps/scandocs/ui/theme/Color.kt) | 📐 TEMA | Paleta completa Material 3: primario Teal/Esmeralda, superficies carbón (dark), badges PDF/JPG, colores de acciones rápidas |
| [`Theme.kt`](../app/src/main/java/com/abdapps/scandocs/ui/theme/Theme.kt) | 📐 TEMA | `ScanDocsDarkColorScheme`, `ScanDocsLightColorScheme`, `ScanDocsTheme { }` composable wrapper |
| [`Type.kt`](../app/src/main/java/com/abdapps/scandocs/ui/theme/Type.kt) | 📐 TEMA | Tipografía Material 3 de la app |

---

## Tokens de color importantes (referencia rápida)

```
Primary Teal Light:  #00796B     Primary Teal Dark:  #00BFA5
Background Dark:     #131417
SurfaceContainerLow Dark: #181A1E
SurfaceContainer Dark:    #1E2126

PDF badge (dark):    bg=#381A1A  fg=#FF8A80
JPG badge (dark):    bg=#142E1B  fg=#81C995

Quick Action Scan (dark):  bg=#14302A  icon=#00BFA5
Quick Action Gallery(dark):bg=#162538  icon=#64B5F6
Quick Action Docs (dark):  bg=#221C38  icon=#B39DDB
```

---

## Guía de edición rápida para IAs

### ¿Quiero agregar un campo a los documentos?
1. Modificar [`DocumentEntity.kt`](../app/src/main/java/com/abdapps/scandocs/data/entity/DocumentEntity.kt)
2. Incrementar la versión de BD en [`AppDatabase.kt`](../app/src/main/java/com/abdapps/scandocs/data/database/AppDatabase.kt) y agregar migración (o aceptar pérdida de datos con `fallbackToDestructiveMigration`)
3. Actualizar [`DocumentDao.kt`](../app/src/main/java/com/abdapps/scandocs/data/dao/DocumentDao.kt) si hay nuevas queries
4. Actualizar [`DocumentRepository.kt`](../app/src/main/java/com/abdapps/scandocs/data/repository/DocumentRepository.kt) si expone la nueva query
5. Actualizar [`ScannerViewModel.kt`](../app/src/main/java/com/abdapps/scandocs/ScannerViewModel.kt) para poblar/usar el nuevo campo
6. Actualizar [`DocumentItemCard.kt`](../app/src/main/java/com/abdapps/scandocs/ui/components/DocumentItemCard.kt) si debe mostrarse en la UI

### ¿Quiero agregar una nueva pantalla?
1. Crear el archivo Composable en `ui/components/`
2. Agregar la ruta en [`MainActivity.kt`](../app/src/main/java/com/abdapps/scandocs/MainActivity.kt) dentro del `NavHost`
3. Si aplica, agregar una pestaña en `ScanDocsBottomBar` (también en `MainActivity.kt`)

### ¿Quiero cambiar los colores del tema?
- Editar [`Color.kt`](../app/src/main/java/com/abdapps/scandocs/ui/theme/Color.kt) (tokens)
- Editar [`Theme.kt`](../app/src/main/java/com/abdapps/scandocs/ui/theme/Theme.kt) (asignación a roles Material 3)

### ¿Quiero implementar thumbnails reales?
- Modificar `FileService.createThumbnail(jpgPath)` en [`FileService.kt`](../app/src/main/java/com/abdapps/scandocs/service/FileService.kt) — actualmente es un stub que retorna el path original
- Actualizar la llamada en `ScannerViewModel.saveDocument()` para usar el path de thumbnail generado

### ¿Quiero agregar Hilt?
1. Agregar plugin y dependencias de Hilt en [`app/build.gradle.kts`](../app/build.gradle.kts)
2. Crear `@HiltAndroidApp` en la clase `Application`
3. Anotar `MainActivity` con `@AndroidEntryPoint`
4. Eliminar el cableado manual en `MainActivity.onCreate`
5. El código de `ScannedDocumentRepository` ya tiene las anotaciones `@Singleton @Inject` que funcionarían con Hilt

### ¿Quiero activar la pila de datos legacy (ScannedDocument)?
> **⚠️ No recomendado directamente** — ambas BDs usan el mismo nombre de archivo.
1. Renombrar `ScanDocsDatabase` a un archivo diferente: `"scandocs_v2_database"`
2. Instanciar `ScanDocsDatabase` en `MainActivity`
3. Conectar `ScannedDocumentRepository` e inyectarlo en el ViewModel
4. O mejor: migrar los campos útiles de `ScannedDocument` a `DocumentEntity` y descartar la pila legacy

---

## Dependencias del módulo `app` (resumen)

```kotlin
// Core
androidx.core.ktx
androidx.lifecycle.runtime.ktx
androidx.activity.compose

// Compose + Material 3
platform(androidx.compose.bom)
androidx.ui
androidx.ui.graphics
androidx.material3
androidx.material.icons.extended

// ML Kit
com.google.android.gms:play-services-mlkit-document-scanner

// Room
androidx.room.runtime
androidx.room.ktx
ksp(androidx.room.compiler)

// Navigation
androidx.navigation.compose

// Coil (carga de imágenes)
io.coil-kt:coil-compose

// Tests
junit (unit) · androidx.test.ext:junit · androidx.espresso.core · compose-ui-test-junit4
```
