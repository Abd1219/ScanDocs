# ARCHITECTURE.md — ScanDocs

Documento de referencia técnica para IAs y desarrolladores. Describe la arquitectura completa, flujo de datos, esquema de BD y deuda técnica conocida.

---

## Tabla de contenidos

1. [Visión general](#1-visión-general)
2. [Capas de la arquitectura](#2-capas-de-la-arquitectura)
3. [Flujo de datos completo](#3-flujo-de-datos-completo)
4. [Estructura de navegación](#4-estructura-de-navegación)
5. [Gestión de estado](#5-gestión-de-estado)
6. [Almacenamiento de archivos](#6-almacenamiento-de-archivos)
7. [Tema visual](#7-tema-visual)
8. [Código legacy y deuda técnica](#8-código-legacy-y-deuda-técnica)
9. [Decisiones de diseño notables](#9-decisiones-de-diseño-notables)

---

## 1. Visión general

ScanDocs es una app Android **single-Activity, single-module** que sigue el patrón **MVVM** con un ViewModel central (`ScannerViewModel`) y repositorio de acceso a datos (`DocumentRepository`). No usa Hilt ni ningún framework de inyección de dependencias — todo se cablea manualmente en `MainActivity.onCreate`.

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│  DocumentScannerScreen  HistoryScreen  DocumentItemCard  │
│  HomeComponents         (diálogos compartidos)           │
└─────────────────────────┬───────────────────────────────┘
                          │ collectAsState / eventos
┌─────────────────────────▼───────────────────────────────┐
│                   ViewModel Layer                        │
│              ScannerViewModel (StateFlow)                │
│   uiState · scannerState · showSaveDialog · flows BD    │
└──────┬──────────────────┬──────────────────────────────┘
       │                  │
┌──────▼──────┐   ┌───────▼───────────────────────────────┐
│ DocumentSc- │   │          Repository Layer              │
│ anner       │   │        DocumentRepository              │
│ (ML Kit     │   └───────────────────┬────────────────────┘
│  wrapper)   │                       │
└─────────────┘   ┌────────────────── ▼────────────────────┐
                  │              Data Layer                  │
                  │  AppDatabase · DocumentDao · DocumentEntity│
                  └────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────┐
│                   Service Layer                           │
│          FileService (I/O archivos + FileProvider)        │
└──────────────────────────────────────────────────────────┘
```

---

## 2. Capas de la arquitectura

### 2.1 UI Layer — Jetpack Compose

| Archivo | Ruta (relativa a `ui/`) | Responsabilidad |
|---|---|---|
| `DocumentScannerScreen.kt` | `components/` | Pantalla Home (FAB, recientes, diálogo guardar) |
| `HomeComponents.kt` | `components/` | `HomeSearchBar`, `QuickActionsSection`, `CompactEmptyRecentState`, `RecentSectionHeader` |
| `HistoryScreen.kt` | `components/` | Pantalla historial + `ChooseFileDialog` + `ConfirmDeleteDialog` (compartidos con Home) |
| `DocumentItemCard.kt` | `components/` | Tarjeta compacta reutilizable con DropdownMenu (Ver / Compartir / Eliminar) |
| `ScannerComponents.kt` | `components/` | **Placeholder vacío** — dejado por compatibilidad de build |
| `FileNameDialog.kt` | `components/` | **Placeholder vacío** — diálogo consolidado en `DocumentScannerScreen` |
| `Color.kt` | `theme/` | Paleta completa para modo claro y oscuro |
| `Theme.kt` | `theme/` | `ScanDocsDarkColorScheme` + `ScanDocsLightColorScheme` + `ScanDocsTheme` |
| `Type.kt` | `theme/` | Tipografía Material 3 |

**Patrón de recolección de estado:**

```kotlin
// En cada Screen:
val uiState by viewModel.uiState.collectAsState()
val documents by viewModel.allDocuments.collectAsState(initial = emptyList())
```

Los Composables son **stateless** excepto por estado local de UI (visibilidad de diálogos). Todo el estado de negocio vive en el ViewModel.

---

### 2.2 ViewModel Layer

**`ScannerViewModel`** — único ViewModel de la app.

**StateFlows expuestos:**

```kotlin
val uiState: StateFlow<ScannerUiState>
val scannerState: StateFlow<ScannerState>
val showSaveDialog: StateFlow<Boolean>
val documentName: StateFlow<String>
val allDocuments: Flow<List<DocumentEntity>>   // desde Room
val recentDocuments: Flow<List<DocumentEntity>> // allDocuments.map { it.take(3) }
```

**`ScannerUiState`:**

```kotlin
data class ScannerUiState(
    val isScannerReady: Boolean = false,
    val isScanning: Boolean = false,
    val scannedPages: List<DocumentPage> = emptyList(),
    val generatedPdf: DocumentPdf? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
```

**`ScannerState` enum:**

```
IDLE → READY → SCANNING → PROCESSING (declarado, nunca asignado) → SUCCESS / ERROR
```

**Funciones públicas del ViewModel:**

| Función | Descripción |
|---|---|
| `initializeScanner(context, activity)` | Crea y registra el `DocumentScanner` |
| `startScanning()` | Guard-checked (READY + !isScanning), delega a `DocumentScanner` |
| `handleScanResult(result)` | Guarda páginas/PDF en `uiState`, muestra diálogo guardar |
| `handleScanCancel()` | Resetea estado a READY |
| `handleScanError(error)` | Resetea estado a READY, setea `errorMessage` |
| `saveDocument()` | Llama a FileService, crea `DocumentEntity`, persiste en Room |
| `deleteDocument(document)` | Borra archivos físicos + registro BD |
| `viewFile(context, filePath, mimeType)` | Crea y lanza `ACTION_VIEW` Intent |
| `shareFile(context, filePath, mimeType)` | Crea chooser Intent con `FLAG_GRANT_READ_URI_PERMISSION` |
| `updateDocumentName(name)` | Actualiza `documentName` StateFlow |
| `dismissSaveDialog()` | Oculta el diálogo de guardar |
| `clearError()` / `clearSuccess()` | Limpia mensajes transitorios de Snackbar |

**Patrón de actualización de estado:**

```kotlin
private fun updateUiState(update: ScannerUiState.() -> ScannerUiState) {
    _uiState.update { it.update() }
}
// Uso:
updateUiState { copy(isScanning = true) }
```

---

### 2.3 DocumentScanner (ML Kit Wrapper)

**Archivo:** `DocumentScanner.kt`

Encapsula `GmsDocumentScanner` y el mecanismo `ActivityResultLauncher`.

**Opciones configuradas:**
- `setGalleryImportAllowed(true)` — galería disponible en la UI del escáner
- `setPageLimit(10)` — máximo 10 páginas por sesión
- `RESULT_FORMAT_JPEG + RESULT_FORMAT_PDF` — ambos formatos
- `SCANNER_MODE_FULL` — UI completa de ML Kit

**Data classes propias:**

```kotlin
data class DocumentPage(val imageUri: Uri, val pageNumber: Int)
data class DocumentPdf(val uri: Uri, val pageCount: Int)
data class DocumentScanResult(val pages: List<DocumentPage>, val pdf: DocumentPdf?)
```

**Ciclo de vida:**

```
initializeScanner()
  └── registerForActivityResult(StartIntentSenderForResult) → guarda launcher

startScan(onResult, onCancel, onError)
  └── scanner.getStartScanIntent(activity) [GMS Task async]
        └── success → launch(IntentSenderRequest)
              └── RESULT_OK → handleScanResult(data) → onResult(DocumentScanResult)
              └── RESULT_CANCELED → onCancel()
              └── otro código → onError(Exception)
```

> **Nota:** `MainActivity` extiende `FragmentActivity` (no `AppCompatActivity`) porque ML Kit requiere `registerForActivityResult` en una `FragmentActivity`.

---

### 2.4 Repository Layer

**`DocumentRepository`** — repositorio activo, thin wrapper sobre `DocumentDao`.

```kotlin
val allDocuments: Flow<List<DocumentEntity>> // ORDER BY createdAt DESC
suspend fun getDocumentById(id: Long): DocumentEntity?
suspend fun insertDocument(document: DocumentEntity): Long
suspend fun updateDocument(document: DocumentEntity)
suspend fun deleteDocument(document: DocumentEntity)
suspend fun deleteDocumentById(id: Long)
```

---

### 2.5 Data Layer — Room

**Base de datos activa:** `AppDatabase` → archivo `scandocs_database`

```kotlin
@Database(entities = [DocumentEntity::class], version = 2, exportSchema = false)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase()
```

La migración `MIGRATION_1_2` agrega `jpgPaths` y conserva los registros existentes. Los documentos previos mantienen la primera imagen en `jpgPath`; como Room no guardaba las demás rutas, el historial no puede compartir retroactivamente todas sus páginas.

**Entidad activa:**

```kotlin
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val jpgPath: String,
    val pdfPath: String,
    val createdAt: java.util.Date,
    val thumbnailPath: String? = null,
    val jpgPaths: String = "" // Array JSON; vacío en registros antiguos
)
```

`allJpgPaths` decodifica `jpgPaths` y usa `jpgPath` como respaldo para registros antiguos.

**TypeConverters activos (`DateTimeConverters`):**
- `java.util.Date ↔ Long` (epoch ms) — **usado por `DocumentEntity.createdAt`**
- `LocalDateTime ↔ String` (ISO-8601) — declarado pero no usado por entidades activas

---

### 2.6 Service Layer

**`FileService`** — manejo de archivos y compartición.

**Directorio de almacenamiento:**

```
Environment.DIRECTORY_DOCUMENTS/ScanDocsApp/
→ Típicamente: /sdcard/Documents/ScanDocsApp/
```

**Nomenclatura de archivos:**
- JPG: `{nombre}_page{N}_{yyyyMMdd_HHmmss}.jpg`
- PDF: `{nombre}_{yyyyMMdd_HHmmss}.pdf`

**Métodos clave:**

| Método | Descripción |
|---|---|
| `saveJpgFile(uri, fileName)` | Lee URI de ML Kit via `ContentResolver`, escribe JPEG |
| `savePdfFile(uri, fileName)` | Lee URI de ML Kit via `ContentResolver`, escribe PDF |
| `deleteFile(filePath)` | Elimina archivo físico, maneja `SecurityException` |
| `getUriForFile(file)` | `FileProvider.getUriForFile(...)` → `content://` URI |
| `createViewIntent(context, filePath, mimeType)` | `ACTION_VIEW` + permisos URI |
| `createShareIntent(context, filePath, mimeType)` | `ACTION_SEND` + chooser |
| `createShareIntent(context, filePaths)` | `ACTION_SEND_MULTIPLE` + `EXTRA_STREAM` y permisos para todos los URI JPG |
| `createThumbnail(jpgPath)` | **Stub** — devuelve `jpgPath` sin procesar |

> `saveJpgFile` y `savePdfFile` están anotados con `@RequiresApi(Build.VERSION_CODES.O)`.  
> El `minSdk` del proyecto es 24 — esto requiere verificación de API en runtime o elevar `minSdk` a 26.

**FileProvider authority:** `${applicationId}.fileprovider` → `com.abdapps.scandocs.fileprovider`

---

## 3. Flujo de datos completo

### 3.1 Escanear y guardar un documento

```
Usuario toca FAB / "Escanear"
         │
         ▼
ScannerViewModel.startScanning()
  Guard: isScannerReady == true && !isScanning
         │
         ▼
DocumentScanner.startScan(onResult, onCancel, onError)
  scanner.getStartScanIntent(activity) → GMS Task
         │ success
         ▼
  IntentSenderRequest → scannerLauncher.launch(...)
         │
         │  ← ML Kit abre su propia UI (cámara, detección bordes, recorte)
         │
         ▼ RESULT_OK
  GmsDocumentScanningResult → DocumentScanResult { pages, pdf }
         │ onResult callback
         ▼
ScannerViewModel.handleScanResult(result)
  uiState ← copy(scannedPages=pages, generatedPdf=pdf)
  showSaveDialog ← true
  documentName ← "Scan_yyyy-MM-dd_HH-mm-ss"
         │
         ▼
UI muestra SaveDocumentDialog (usuario puede editar el nombre)
         │ toca "Guardar"
         ▼
ScannerViewModel.saveDocument()
  ├── FileService.saveJpgFile(page.imageUri, name_pageN) por cada página
  │     └── retorna ruta absoluta al JPG
  ├── FileService.savePdfFile(pdf.uri, name)
  │     └── retorna ruta absoluta al PDF
  ├── Construye DocumentEntity con jpgPath (primera página), jpgPaths (todas) y pdfPath
  └── DocumentRepository.insertDocument(entity)
         │  → DocumentDao → Room → SQLite INSERT
         ▼
uiState.successMessage ← "Documento '$name' guardado."
scannerState ← READY
showSaveDialog ← false
         │
         ▼
AppDatabase Flow emite nueva lista
recentDocuments / allDocuments actualizados
UI re-compone → nueva tarjeta visible en Home y en History
```

### 3.2 Ver / Compartir un documento

```
Usuario abre DropdownMenu en DocumentItemCard
  → toca "Ver documento" o "Compartir"
         │
         ▼
UI muestra ChooseFileDialog (PDF, primera imagen JPG o todas las páginas JPG al compartir)
  Usuario elige formato
         │
         ▼
ScannerViewModel.viewFile(context, filePath, mimeType)
  FileService.createViewIntent(context, filePath, mimeType)
    → FileProvider.getUriForFile(file)
    → Intent(ACTION_VIEW, contentUri)
       + FLAG_GRANT_READ_URI_PERMISSION
       + FLAG_ACTIVITY_NEW_TASK
  context.startActivity(intent)

— o bien —

ScannerViewModel.shareFile(context, filePath, mimeType)
  FileService.createShareIntent(...)
    → Intent(ACTION_SEND) + EXTRA_STREAM + permisos URI
  context.startActivity(Intent.createChooser(...))

Para compartir todas las páginas JPG, la UI llama `ScannerViewModel.shareJpgFiles(context, paths, documentName)`. El servicio crea un `ACTION_SEND_MULTIPLE` con todos los URI `content://` y permisos de lectura.
```

### 3.3 Eliminar un documento

```
Usuario toca "Eliminar" → ConfirmDeleteDialog
  → confirma
         │
         ▼
ScannerViewModel.deleteDocument(document)
  FileService.deleteFile(document.pdfPath)
  FileService.deleteFile() para cada ruta de `document.allJpgPaths`
  FileService.deleteFile(document.thumbnailPath)
  DocumentRepository.deleteDocument(document)
    → DocumentDao.deleteDocument() → SQLite DELETE
         │
         ▼
Room Flow emite lista actualizada → UI re-compone
```

---

## 4. Estructura de navegación

```
MainActivity
└── MainScreen (Scaffold + ScanDocsBottomBar)
    └── NavHost(startDestination = "home")
        ├── "home" → DocumentScannerScreen
        │   ├── FAB → viewModel.startScanning()
        │   ├── HomeSearchBar.onClick → navigate("history")
        │   ├── QuickActionsSection
        │   │   ├── onScanClick → viewModel.startScanning()
        │   │   ├── onImportClick → viewModel.startScanning() (ML Kit abre galería)
        │   │   └── onViewAllClick → navigate("history")
        │   └── RecentSectionHeader.onViewAllClick → navigate("history")
        │
        └── "history" → HistoryScreen
            └── Back arrow → navController.popBackStack()

ScanDocsBottomBar (persistente, fuera del NavHost)
├── Tab "Inicio" → navigate("home")
└── Tab "Documentos" → navigate("history")
```

**Opciones de navegación usadas en todos los `navigate()`:**

```kotlin
navOptions {
    popUpTo(startDestination) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

---

## 5. Gestión de estado

### Snackbar de feedback

Los mensajes de éxito/error en `DocumentScannerScreen` e `HistoryScreen` se muestran con:

```kotlin
LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let {
        snackbarHostState.showSnackbar(it)
        viewModel.clearSuccess()
    }
}
```

### Estado de los diálogos de acción (View/Share/Delete)

Cada pantalla mantiene **estado local** para saber qué documento está seleccionado:

```kotlin
var showViewDialog: DocumentEntity? by remember { mutableStateOf(null) }
var showShareDialog: DocumentEntity? by remember { mutableStateOf(null) }
var showDeleteDialog: DocumentEntity? by remember { mutableStateOf(null) }
```

### Búsqueda en History

Filtrado **en memoria** (no via Room query):

```kotlin
val filteredDocuments = remember(allDocuments, searchQuery) {
    if (searchQuery.isBlank()) allDocuments
    else allDocuments.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
```

---

## 6. Almacenamiento de archivos

### Ubicación

Los archivos se guardan en almacenamiento externo público:

```
/sdcard/Documents/ScanDocsApp/
├── MiDocumento_page1_20260919_221000.jpg
├── MiDocumento_page2_20260919_221000.jpg
└── MiDocumento_20260919_221000.pdf
```

### Permisos declarados en Manifest

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

### ⚠️ Problema de compatibilidad de almacenamiento

`FileService` usa `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)` que en API 29+ (Android 10+) requiere `MANAGE_EXTERNAL_STORAGE` o una migración a `MediaStore`. La declaración `WRITE_EXTERNAL_STORAGE` con `maxSdkVersion=28` es correcta, pero la lectura/escritura en este directorio puede fallar en dispositivos Android 10+ dependiendo del fabricante. Se recomienda migrar a `MediaStore` o al directorio privado de la app (`context.getExternalFilesDir`).

---

## 7. Tema visual

### Paleta de colores

| Token | Claro | Oscuro |
|---|---|---|
| Primary (Teal) | `#00796B` | `#00BFA5` |
| Background | `#F7F9FA` | `#131417` |
| Surface | `#FFFFFF` | `#131417` |
| SurfaceContainerLow | `#F2F4F7` | `#181A1E` |
| SurfaceContainer | `#EAEFF2` | `#1E2126` |
| PDF Badge bg | `#FDE8E7` | `#381A1A` |
| JPG Badge bg | `#E6F4EA` | `#142E1B` |

### Colores de acciones rápidas (QuickActionsSection)

| Acción | Fondo Claro | Fondo Oscuro |
|---|---|---|
| Escanear | `#D1F2EB` | `#14302A` |
| Galería | `#E3F2FD` | `#162538` |
| Documentos | `#EDE7F6` | `#221C38` |

---

## 8. Código legacy y deuda técnica

### Segunda pila de datos (inactiva)

Existe una segunda implementación completa que **no está conectada** a la app activa:

| Componente legacy | Componente activo equivalente |
|---|---|
| `ScanDocsDatabase` | `AppDatabase` |
| `ScannedDocument` | `DocumentEntity` |
| `ScannedDocumentDao` | `DocumentDao` |
| `ScannedDocumentRepository` | `DocumentRepository` |
| `data/converter/Converters.kt` | `data/converter/DateTimeConverters.kt` |

> **⚠️ Conflicto potencial:** Ambas bases de datos usan el mismo nombre de archivo `"scandocs_database"`. Si alguna vez se instanciaran simultáneamente, colisionarían.

`ScannedDocumentRepository` tiene anotaciones `@Singleton @Inject` (Hilt/Dagger) pero **Hilt no está configurado** en el proyecto — son decorativas/muertas.

### Deuda técnica conocida

| Área | Problema | Impacto |
|---|---|---|
| `FileService.saveJpgFile/savePdfFile` | `@RequiresApi(O)` pero `minSdk=24` | Crash en API 24-25 sin guard en runtime |
| `FileService.createThumbnail()` | Stub — retorna el path original sin procesar | Sin thumbnails reales |
| Strings de UI | Hardcodeadas en Composables (no en `strings.xml`) | Sin soporte de localización |
| `PermissionHelper` | Clase definida pero no instanciada | ML Kit gestiona sus propios permisos |
| `ScannerState.PROCESSING` | Declarado pero nunca asignado | Estado del enum huérfano |
| `DocumentScanner.updateOptions()` | Crea nuevo cliente pero no reassigna el campo | Funcionalidad incompleta |
| `data/database/Converters.kt` | Duplica parte de `DateTimeConverters` y no está vinculado a ninguna `@Database` | Código muerto |
| Almacenamiento externo público | Problema de compatibilidad en API 29+ | Posibles fallos en Android 10+ |
| `fallbackToDestructiveMigration()` | Pérdida de datos en cambios de esquema | Riesgo en producción |
| Tests | Sin tests unitarios ni de integración | Sin cobertura de código |
| DI manual | Sin framework DI | Escalabilidad limitada |

---

## 9. Decisiones de diseño notables

1. **`FragmentActivity` en lugar de `ComponentActivity`:** Requerido por ML Kit para `registerForActivityResult`. No es un error — es una restricción de la librería.

2. **`recentDocuments` derivado, no consultado:** `recentDocuments = allDocuments.map { it.take(3) }` — evita una query adicional a Room al costo de cargar siempre la lista completa en memoria.

3. **Búsqueda client-side en History:** Aceptable para el volumen esperado de documentos. Si el número crece (cientos/miles), se debería migrar a una query de Room con `LIKE`.

4. **`SaveDocumentDialog` consolidado en `DocumentScannerScreen`:** El archivo `FileNameDialog.kt` es un placeholder vacío mantenido por compatibilidad de build (está importado o referenciado indirectamente). Lo mismo aplica a `ScannerComponents.kt`.

5. **`ChooseFileDialog` y `ConfirmDeleteDialog` en `HistoryScreen.kt`:** Aunque son usados también desde `DocumentScannerScreen`, están definidos en `HistoryScreen.kt`. Son Composables públicos (sin modificador privado) y se importan directamente.

6. **`updateUiState { copy(...) }`:** Patrón de actualización inmutable del estado. Garantiza thread-safety y predecibilidad.

7. **UI en español:** Todas las cadenas de texto de usuario están en español directamente en los Composables. No hay archivo `strings.xml` con traducciones.
