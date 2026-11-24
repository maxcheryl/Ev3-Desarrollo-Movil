**Agencia Atenas - Gestión de oportunidades**

## Integrantes y equipo

- **Integrante 1:** Maxine Moreno
- **Integrante 2:** Dalí Morales
- **Equipo:** 9

## Descripción

Aplicación móvil desarrollada en **Kotlin y Jetpack Compose** para gestionar oportunidades de negocio corporativas.

El proyecto corresponde a la Evaluación 3 de la asignatura **DSY1105 - Desarrollo de Aplicaciones Móviles** (Duoc UC), y continúa el trabajo iniciado en las evaluaciones anteriores de la asignatura.

## Sobre el proyecto

Agencia Atenas necesita organizar sus oportunidades de negocio, como eventos corporativos, ferias y jornadas de beneficios. El propósito del proyecto es entregar una aplicación funcional que permita registrar y dar seguimiento a esas oportunidades: desde su identificación y las gestiones comerciales asociadas, hasta la activación con evidencias y resultados, manteniendo un historial de cada cambio.

## Funcionalidades

| Módulo | Descripción |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Inicio de sesión** | Accede con credenciales de demostración, valida los campos obligatorios y aplica permisos según el perfil (Ejecutivo comercial y Supervisor comercial). |
| **Navegación** | Permite desplazarse entre el menú principal, oportunidades, catálogos, gestiones, activaciones, escáner QR, ubicación y API externa. |
| **Oportunidades** | Muestra una lista con nombre, tipo, prioridad y estado; incluye datos de ejemplo y permite crear, editar y eliminar oportunidades. |
| **Detalle y edición** | Consulta la información completa de una oportunidad y modifica sus datos. |
| **Formulario** | Crea y edita oportunidades con validación de campos, opciones predefinidas y estado inicial «Evento identificado». |
| **Validaciones** | Muestra mensajes de error junto a los campos e impide guardar información que no cumple los requisitos. |
| **Catálogos** | Administra organizaciones, contactos y eventos con formularios validados, para relacionarlos con las oportunidades. |
| **Gestiones comerciales** | Registra contactos, propuestas y seguimientos; actualiza el estado de la oportunidad según la gestión. |
| **Historial de cambios** | Conserva un registro de creación, cambios de estado, prioridad, responsable y gestiones de cada oportunidad. |
| **Activaciones** | Coordina la activación (responsable, espacio, fecha, costo, requisitos), adjunta evidencias en foto o documento y registra el resultado final de la oportunidad. |
| **Escaneo QR** | Usa la cámara para leer códigos con prefijo `MAX:`; los válidos abren la oportunidad asociada y los inválidos se rechazan sin modificar registros. |
| **Ubicación** | Consulta la ubicación del dispositivo bajo demanda, sin seguimiento continuo ni almacenamiento del resultado. |
| **API externa** | Consulta publicaciones de prueba de JSONPlaceholder desde la pantalla «API de prueba» (Eventos → API de prueba), sin guardarlas en Room. |
| **Persistencia local** | Mantiene los registros en el dispositivo para que sigan disponibles al cerrar y volver a abrir la aplicación. |
| **Animaciones** | Incluye transiciones entre pantallas, animaciones en la lista y efectos visuales para cambios de estado y mensajes de error. |

## Endpoints utilizados

### API externa (JSONPlaceholder)

| Método | Endpoint | Descripción |
| ------ | -------- | ----------- |
| `GET` | `https://jsonplaceholder.typicode.com/posts` | Devuelve la lista de publicaciones de prueba, que se muestran en la pantalla «API de prueba». No se almacenan localmente. |

La pantalla indica explícitamente que son datos ficticios de la API y no se guardan en Room, por lo que no interfieren con los datos locales de la aplicación.

## Credenciales de demostración

| Perfil | Correo | Contraseña |
| ------ | ------ | ---------- |
| Ejecutivo comercial | `dali.morales@at.com` | `dali123` |
| Supervisor comercial | `maxine.moreno@at.com` | `maxine123` |


## Tecnologías utilizadas

- **Lenguaje:** Kotlin 2.2
- **Interfaz:** Jetpack Compose y Material Design 3
- **Arquitectura:** MVVM, ViewModel, StateFlow y Repository
- **Persistencia:** Room 2.8.4 con KSP
- **Navegación:** Navigation Compose
- **API externa:** Retrofit 2.12 con converter Gson
- **Cámara y códigos QR:** CameraX 1.4.2 y ML Kit Barcode Scanning 17.3.0
- **Ubicación:** LocationManager de Android
- **Pruebas:** JUnit 4, MockK y kotlinx-coroutines-test
- **Entorno:** Android Studio
- **SDK:** compileSdk y targetSdk 37; minSdk 24

## Estructura del proyecto

```text
app/src/main/java/cl/duocuc/evaluacion2/
├── data/
│   ├── historial/      # descripciones del historial de cambios
│   ├── local/          # entidades, DAOs y base de datos Room
│   ├── model/          # modelos y catálogos de la aplicación
│   ├── remote/         # interfaz y configuración de Retrofit
│   ├── repository/     # repositorios (única puerta a los datos)
│   └── session/        # sesión del usuario en memoria
├── location/           # consulta de ubicación del dispositivo
├── navigation/         # rutas y host de navegación
├── qr/                 # escaneo y validación de códigos QR
├── ui/
│   ├── components/     # componentes visuales reutilizables
│   ├── screens/        # pantallas Compose
│   ├── theme/          # tema y colores
│   └── viewmodels/     # ViewModels de cada módulo
└── validation/         # validadores de formularios

app/src/test/java/cl/duocuc/evaluacion2/
└── ...                 # pruebas unitarias (ViewModel y Repository)
```

- `data`: contiene los modelos, el acceso a los datos y su gestión.
- `navigation`: organiza las rutas y la navegación entre pantallas.
- `location` y `qr`: gestionan la ubicación y el escaneo de códigos.
- `ui`: reúne las pantallas, los componentes visuales, el tema y los modelos de vista.
- `validation`: validaciones de los formularios, separadas de la interfaz.
- `app/src/test`: pruebas unitarias de la lógica de ViewModels y repositorios.

## Requisitos para ejecutar la aplicación

- Android Studio (versión actual o compatible).
- JDK 17 o superior.
- SDK de Android con la plataforma 37 y las herramientas de compilación del proyecto.
- Un emulador o dispositivo Android compatible (minSdk 24).
- Conexión a Internet para consultar la API externa.

## Cómo ejecutar la aplicación

1. Clonar o descargar el repositorio.
2. Abrir el proyecto en Android Studio (**File → Open** y seleccionar la carpeta del proyecto).
3. Esperar la sincronización de Gradle (**Sync Now**) y la descarga de dependencias.
4. Seleccionar un emulador o dispositivo conectado (**Device Manager**).
5. Ejecutar el módulo **app** con el botón ▶ *Run*.
6. Iniciar sesión con una de las credenciales de demostración.
7. Para consultar la API externa: ingresar a **Catálogos → Eventos → API de prueba** (requiere Internet).

### Ejecutar las pruebas unitarias

- Desde Android Studio: clic derecho en la carpeta `app/src/test` → **Run Tests**, o usar *Run → Run with Coverage* para medir la cobertura.
- Desde la terminal: `.\gradlew.bat testDebugUnitTest` (Windows).

### Probar el escáner QR

La aplicación reconoce códigos cuyo contenido comienza con `MAX:`. Las oportunidades de ejemplo utilizan los siguientes códigos:

- `MAX:OP-001`
- `MAX:OP-002`
- `MAX:OP-003`

Pasos: generar un código QR con uno de los valores anteriores → abrir **Escanear código QR** desde el menú → conceder el permiso de cámara → escanear y comprobar que se abra la oportunidad correspondiente. Si el contenido no es válido, la aplicación muestra un mensaje de error sin modificar los registros.

Para probar la ubicación en el emulador se puede configurar una posición desde **Extended Controls → Location**.
