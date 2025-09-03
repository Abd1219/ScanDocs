# ScanDocs - Escáner de Documentos con ML Kit

## 📱 Descripción

ScanDocs es una aplicación Android moderna que permite escanear documentos usando la cámara del dispositivo. La aplicación utiliza **ML Kit Document Scanner** de Google para proporcionar una experiencia de escaneo inteligente y profesional.

## ✨ Características Principales

- **Escaneo Inteligente**: Detección automática de bordes de documentos
- **Múltiples Formatos**: Generación de imágenes JPEG y archivos PDF
- **Interfaz Moderna**: UI construida con Jetpack Compose y Material Design 3
- **Manejo de Permisos**: Gestión automática de permisos de cámara y almacenamiento
- **Arquitectura Limpia**: Implementación siguiendo patrones MVVM y Clean Architecture

## 🏗️ Arquitectura del Proyecto

```
app/src/main/java/com/abdapps/scandocs/
├── MainActivity.kt              # Actividad principal
├── PermissionHelper.kt          # Manejo de permisos
├── DocumentScanner.kt           # Lógica del escáner ML Kit
├── ScannerViewModel.kt          # ViewModel para la lógica de negocio
└── ui/
    └── components/
        └── ScannerComponents.kt # Componentes de UI en Compose
```

## 🚀 Tecnologías Utilizadas

- **Android SDK**: API 24+ (Android 7.0+)
- **ML Kit**: Document Scanner API
- **Jetpack Compose**: UI declarativa moderna
- **ViewModel**: Gestión de estado y ciclo de vida
- **Kotlin Coroutines**: Programación asíncrona
- **Material Design 3**: Sistema de diseño moderno

## 📋 Requisitos del Sistema

### Requisitos Mínimos
- **Android**: API 21+ (Android 5.0+)
- **RAM**: Mínimo 1.7GB de RAM del dispositivo
- **Google Play Services**: Debe estar instalado y actualizado

### Permisos Requeridos
- **Cámara**: Para escanear documentos
- **Almacenamiento**: Para guardar documentos escaneados
  - Android 12 y anteriores: `READ_EXTERNAL_STORAGE`
  - Android 13+: `READ_MEDIA_IMAGES`

## 🛠️ Instalación y Configuración

### 1. Clonar el Repositorio
```bash
git clone <url-del-repositorio>
cd ScanDocs
```

### 2. Configurar el Proyecto
- Abrir el proyecto en Android Studio
- Sincronizar las dependencias de Gradle
- Asegurarse de que el SDK de Android esté configurado correctamente

### 3. Configurar el Dispositivo
- Habilitar la depuración USB
- Conectar un dispositivo Android compatible
- Instalar la aplicación

### 4. Ejecutar la Aplicación
- Presionar el botón "Run" en Android Studio
- Seleccionar el dispositivo de destino
- La aplicación se instalará y ejecutará automáticamente

## 📱 Uso de la Aplicación

### Primer Uso
1. **Permisos**: La aplicación solicitará permisos de cámara y almacenamiento
2. **Inicialización**: El escáner se inicializará automáticamente
3. **Estado**: Verificar que el estado muestre "Listo para escanear"

### Escaneo de Documentos
1. **Preparar**: Colocar el documento en una superficie plana y bien iluminada
2. **Escanear**: Presionar "Escanear Documento"
3. **Posicionar**: Usar la guía visual para alinear el documento
4. **Capturar**: La aplicación detectará automáticamente los bordes
5. **Revisar**: Ver la vista previa y ajustar si es necesario
6. **Confirmar**: Aceptar el escaneo para procesar

### Resultados
- **Imágenes**: Cada página se guarda como archivo JPEG
- **PDF**: Se genera automáticamente un archivo PDF con todas las páginas
- **Acceso**: Los archivos se pueden acceder desde la aplicación

## 🔧 Configuración Avanzada

### Personalizar Opciones del Escáner
```kotlin
val customOptions = GmsDocumentScannerOptions.Builder()
    .setGalleryImportAllowed(false)        // Deshabilitar importación desde galería
    .setPageLimit(5)                       // Limitar a 5 páginas
    .setResultFormats(RESULT_FORMAT_JPEG)  // Solo formato JPEG
    .setScannerMode(SCANNER_MODE_BASE)     // Modo básico
    .build()
```

### Modos de Escáner Disponibles
- **SCANNER_MODE_BASE**: Funcionalidad básica
- **SCANNER_MODE_FULL**: Funcionalidad completa (recomendado)
- **SCANNER_MODE_GALLERY**: Solo importación desde galería

## 🐛 Solución de Problemas

### Error: "El escáner no está listo"
- Verificar que Google Play Services esté actualizado
- Reiniciar la aplicación
- Verificar permisos de cámara y almacenamiento

### Error: "API no soportada"
- Verificar que el dispositivo tenga al menos 1.7GB de RAM
- Asegurarse de que la API sea 21 o superior
- Verificar que Google Play Services esté disponible

### Problemas de Rendimiento
- Cerrar otras aplicaciones que usen la cámara
- Asegurar buena iluminación para el escaneo
- Verificar que el dispositivo tenga suficiente espacio de almacenamiento

## 📚 API Reference

### Clases Principales

#### DocumentScanner
```kotlin
class DocumentScanner(
    context: Context,
    activity: FragmentActivity
)
```
- **initializeScanner()**: Inicializa el launcher del escáner
- **startScan()**: Inicia el proceso de escaneo
- **cleanup()**: Libera recursos del escáner

#### ScannerViewModel
```kotlin
class ScannerViewModel : ViewModel()
```
- **initializeScanner()**: Configura el escáner
- **startScanning()**: Inicia el escaneo
- **clearResults()**: Limpia los resultados

### Estados del Escáner
- **IDLE**: Estado inicial
- **READY**: Escáner listo para usar
- **SCANNING**: Escaneando documento
- **COMPLETED**: Escaneo completado
- **ERROR**: Error en el escaneo

## 🤝 Contribución

### Cómo Contribuir
1. Fork del repositorio
2. Crear una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit de tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear un Pull Request

### Estándares de Código
- Usar Kotlin para todo el código nuevo
- Seguir las convenciones de nomenclatura de Android
- Documentar todas las funciones públicas
- Incluir tests unitarios para nueva funcionalidad

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 🙏 Agradecimientos

- **Google ML Kit**: Por proporcionar la API de escaneo de documentos
- **Jetpack Compose**: Por la excelente experiencia de desarrollo de UI
- **Comunidad Android**: Por el continuo soporte y contribuciones

## 📞 Soporte

Si tienes preguntas o problemas:

1. **Issues**: Crear un issue en GitHub
2. **Documentación**: Revisar la documentación oficial de ML Kit
3. **Comunidad**: Buscar en Stack Overflow o grupos de desarrolladores Android

---

**Desarrollado con ❤️ usando tecnologías modernas de Android**
