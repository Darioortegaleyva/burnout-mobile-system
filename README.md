
## Cambios de la versión 7.0 — Cierre de las tareas

Auditoría estricta buscando lo que NO estaba. Tres huecos encontrados y resueltos:

1. **[CRÍTICO] El permiso de notificaciones no se pedía en tiempo de ejecución.** Estaba declarado en el manifest, pero en Android 13+ eso no basta: sin `requestPermission()` el recordatorio **no habría llegado nunca**, dejando las Tareas 2 y 3 inoperativas en la práctica. Ahora se solicita una vez al arrancar; denegarlo no bloquea nada.
2. **El perfil físico se guardaba pero no influía en nada.** Ahora la edad personaliza el umbral de elevación de la FC a través de la **reserva cardíaca** ((220 − edad) − FC en reposo): cuanta menos reserva, más significativo es un mismo repunte. Ejemplos: 22 años y 51 lpm → 11,3 lpm; 58 años y 75 lpm → 6,7 lpm; sin edad declarada → 10 lpm de referencia. Acotado a [6, 14] para que afine sin desvirtuar.
3. **El umbral de FC se apoyaba en un solo estudio.** Documentado ahora con tres líneas de evidencia convergentes (FC basal elevada en burnout, magnitud del cambio por unidad de estrés, y la convención de 5/10 lpm en monitorización con wearables).

### Conclusión del estudio de viabilidad del perfil físico
La **edad** aporta un ajuste defendible vía reserva cardíaca. El **peso, la altura y el sexo** influyen sobre todo en el *nivel* basal de la frecuencia, no en su *reactividad*, y ese nivel ya queda absorbido por la línea base individual. Se recogen igualmente (petición expresa) y quedan disponibles para una calibración futura, pero no se les atribuye un efecto que la evidencia no respalda.

### Referencias añadidas
- Föhr, T. et al. (2022). Occupational Burnout Is Linked with Inefficient Executive Functioning, Elevated Average Heart Rate, and Decreased Physical Activity. *Brain Sciences*, 12(12), 1723.
- De Vente, W. et al. (2015). Burnout Is Associated with Reduced Parasympathetic Activity and Reduced HPA Axis Responsiveness. *BioMed Research International*.


## Cambios de la versión 6.0 — Auditoría completa de las tareas

Verificación punto por punto de las tareas encargadas. Lo que faltaba y se ha añadido:

- **RAG sobre las actividades** (Tarea 4, tercer punto): el corpus ya no son solo los 19 fragmentos teóricos; ahora **indexa automáticamente las 24 pautas del catálogo**, de modo que el asistente responde tanto «¿qué es el burnout?» como «¿en qué consiste el apagón de pantallas?». Al añadir una pauta nueva al catálogo, el asistente la conoce sin tocar nada más.
- **Pautas de las fuentes sugeridas por la tutora** (mind.org.uk y UCDenver): 8 actividades nuevas de redacción propia inspiradas en esas guías — nombrar el problema, conversación con quien decide, las tres columnas, recuperar el porqué, microdescansos reales, algo que no rinda, descarga mental antes de dormir y luz por la mañana. El catálogo pasa de 16 a 24.
- **Personalización opcional del pulso** (Tarea 6, último punto): tarjeta en Dispositivos que **pregunta al usuario si quiere afinar** la lectura aportando edad, altura, peso y sexo biológico. Es opcional y borrable en cualquier momento; el sistema funciona igual sin ella porque compara a cada persona con su propia línea base.

### Correcciones de esta versión
- **Sesiones de sueño solapadas**: se fusionan los intervalos en lugar de sumarlos a ciegas (antes podían salir totales imposibles, de más de 24 h, si dos apps escribían la misma noche). Con límite de cordura de 16 h.
- **El generador de semana limpia sus datos previos** antes de insertar (solo los escritos por esta app; los del wearable real quedan intactos).
- **Las sugerencias tienen en cuenta el componente dominante**: si el sueño es lo que más tira del riesgo, higiene del sueño aparece primero aunque el escenario sea severo. Antes se elegía solo por escenario, lo que resultaba incoherente para quien llevaba una semana durmiendo mal.
- **Umbral de culpa**: la constante documental decía 0,60 «normalizado» cuando el código usa 2,5 en escala 0–4. Unificado, y el gestor usa ya la constante.

### Estado de las tareas
| Tarea | Estado |
|---|---|
| T0 · Enrutamiento BOT A / BOT B | ✅ |
| T1 · BOT A (preguntas, score, timestamp, pautas) | ✅ |
| T2 · Notificación de cuestionario | ✅ |
| T3 · Clic → chat con frase aleatoria (11 variantes) | ✅ |
| T4 · RAG simple (burnout + actividades + «qué tal fue») | ✅ |
| T5 · Panel de dispositivos | ✅ |
| T6 · FC en la fórmula + personalización opcional | ✅ |
| Score general (umbrales, rangos, integración) | ✅ |
| Pautas sin repetir + historial | ✅ |
| User stories en la memoria | ⏳ pendiente (documentación) |


## Cambios de la versión 5.0 — Chat con dos bots, RAG y score documentado

### Chat: enrutamiento BOT A / BOT B (Tareas 0–4)
- **Enrutamiento** (`ChatbotViewModel.init`): al abrir el chat, venga de la notificación o por iniciativa del usuario, se comprueba si toca cuestionario. Si toca, se **ofrece** (nunca se impone): aceptar lleva al BOT A, rechazar lleva al BOT B. Si no toca, BOT B directo.
- **BOT A** (estático, sin modelo): administra los 20 ítems con chips, acuses sensibles a la valencia de cada respuesta, guarda el score con marca temporal y **termina dando pautas** y negociando retos.
- **BOT B** (`data/rag/`): asistente documental con **RAG léxico** sobre 18 fragmentos curados de fuentes oficiales (INSST, OMS, Gil-Monte…). Acepta texto libre. Si ninguna fuente cubre la pregunta, **lo admite en lugar de inventar**. Muestra siempre la fuente usada.
- **Filtro de crisis** (`domain/chat/FiltroCrisis.kt`): barrera OBLIGATORIA antes del RAG. Detecta crisis y malestar intenso, corta la generación y deriva con texto fijo + Línea 024.
- **Notificación de reevaluación** (`work/NotificadorCuestionario.kt` + `RecordatorioCuestionarioWorker`): comprobación diaria, un aviso por ciclo y no más de uno por semana. Al pulsarla se aterriza en el chat con una **frase de apertura elegida al azar** entre 8 variantes (revisables en `FrasesEntrada.kt`).
- **La EMA diaria vive ahora dentro del BOT B**: una vez al día como máximo, sin flujo aparte. Mantiene la capa táctica del §5.4.

### Score general (Tarea 6)
- Nuevo `UmbralesRiesgo.kt` con **todos los cortes documentados**: bandas del índice (bueno < 0,35 ≤ intermedio < 0,60 ≤ cuidarse), cortes por rama y normalizadores de cada componente biométrico.
- **Frecuencia cardíaca en reposo incorporada a la fórmula** con peso ω₄ = 0,10 (pesos: CESQT 0,40 · RMSSD 0,20 · TST 0,30 · RHR 0,10). Se computa como *elevación en lpm sobre la línea base individual*, normalizada por 10 lpm.
- **Renormalización ante ramas ausentes**: si falta un dato (p. ej. Zepp no exporta RMSSD), su peso se reparte entre las disponibles en lugar de asumir un valor neutro. El índice sigue en [0,1] y es comparable entre usuarios con distinto hardware.
- **Perfil físico opcional** (edad, sexo, altura, peso) en `UsuarioEntity`, solo si el usuario lo autoriza: el sistema funciona sin él porque razona sobre la línea base individual.

### Pautas sin repetición
- Nueva entidad `RecomendacionEntity` + `RecomendacionDao`: se registra qué pauta se propuso y cuándo.
- `CatalogoPautas.elegirSinRepetir()` prioriza las no propuestas en los últimos **21 días**.
- El BOT B abre preguntando **qué tal fue la última pauta** y guarda la valoración.

### Dispositivos (Tarea 5)
- Panel de configuración: **interruptores por métrica** (sueño / FC / variabilidad) y **selección de fuente prioritaria** entre las apps detectadas escribiendo en Health Connect.
- `HealthConnectManager.fuentesDetectadas()` lista los paquetes de origen reales; `PreferenciasDispositivos.nombreLegible()` los traduce (Zepp, Samsung Health, Fitbit…).

### Base de datos
- Versión **3**: nuevos campos (`rhrMedioBase`, `ultimaEmaEpochDay`, perfil físico) y tabla `recomendacion`. Migración destructiva (prototipo).

### Pendiente de decisión de la tutora
- Si el RAG se queda corto con Gemma 3 270M, **subir a Gemma 3 1B** (529 MB) es cambiar el `.task` de assets: no requiere tocar código.
- Ampliar el corpus documental: añadir entradas a `BaseConocimiento.fragmentos` (cada una con su fuente).

# BurnoutApp — Sistema móvil de evaluación y mitigación del Burnout

Código fuente del TFG. Aplicación **Android nativa, offline-first, sin servidor**, que triangula biometría (Health Connect) y psicometría (CESQT) para calcular un Índice de Riesgo Multimodal y proponer pautas de mitigación, con derivación automática al Colegio Oficial de Psicología (COP) ante el Perfil 2.

> **Aviso:** este proyecto es el esqueleto funcional y bien estructurado del sistema descrito en la memoria. Está pensado para abrirse en Android Studio, sincronizar Gradle y compilar. **No es un binario terminado**: hay puntos marcados como `TODO` que debes completar (ver más abajo).

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (3 capas) |
| Estado reactivo | ViewModel + Kotlin Flows / StateFlow |
| Persistencia local | Room (sobre SQLite) |
| Tareas en segundo plano | WorkManager |
| Biometría | API Google Health Connect |

## Cómo abrir y compilar

1. Instala **Android Studio** (versión Koala o posterior).
2. Abre la carpeta del proyecto (`File → Open`).
3. Android Studio descargará Gradle 8.9 y todas las dependencias automáticamente (necesita conexión la primera vez).
4. Crea un emulador con **API 34+** (o conecta un móvil con Android 9+).
5. Instala **Health Connect** en el dispositivo desde Google Play (en Android 14+ ya viene integrado).
6. Ejecuta (`Run ▶`).

> El Gradle Wrapper (`gradlew`) se genera automáticamente la primera vez que Android Studio abre el proyecto, o ejecutando `gradle wrapper` si tienes Gradle instalado. Por eso no se incluye el binario `gradle-wrapper.jar` en este paquete.

## Mapa del código (qué archivo implementa qué parte de la memoria)

```
domain/                        ← Capa de Lógica de Negocio (§4.3)
  cesqt/Cesqt.kt               ← estructura del CESQT (4 dimensiones, escala 0–4)
  cesqt/CalculadoraCesqt.kt    ← puntuación por dimensión + score global + subscore Culpa
  engine/MotorRiesgo.kt        ← ECUACIÓN R = ω1·Score + ω2·ΔRMSSD + ω3·ΔTST (§2.2.5)
  engine/GestorCoping.kt       ← Matriz de escenarios A–D (§2.3.4)
  engine/ModuloEticoRuteo.kt   ← derivación COP por provincia, sin GPS (§2.2.7)
  model/                       ← modelos de dominio

data/                          ← Capa de Datos e Integración (§4.4)
  local/entity/Entidades.kt    ← Room: Usuario, CESQT, Biometria, SedeCop, Meta
  local/dao/Daos.kt            ← DAOs
  local/AppDatabase.kt         ← base de datos + asset seeding
  local/seed/SedesCopSeed.kt   ← directorio de sedes del COP (PARCIAL — completar)
  healthconnect/HealthConnectManager.kt ← lectura de sueño, HRV y RHR
  repository/BurnoutRepository.kt ← orquesta datos + dominio

work/                          ← Cron Scheduler (§2.2.6)
  LecturaNocturnaWorker.kt     ← lectura nocturna con WorkManager
  SchedulerConfig.kt           ← programación periódica (03:00)

ui/                            ← Capa de Presentación (§4.2, §6)
  theme/                       ← paleta SIN rojo (principio P4)
  navigation/AppNavigation.kt  ← navegación + barra inferior
  dashboard/                   ← Pantalla 1: Batería de Energía (§6.2)
  chatbot/                     ← Pantalla 2: conversacional + derivación (§6.3)
  devices/                     ← Pantalla 3: semáforo de sincronización (§6.4)

BurnoutApp.kt                  ← inyección manual de dependencias
MainActivity.kt                ← single-activity Compose
```

## Estado de los puntos pendientes

Estos son los cinco puntos que originalmente quedaban abiertos. Cuatro ya están resueltos en este paquete; el primero requiere una licencia que solo tú puedes obtener.

1. **Ítems del CESQT** (`domain/cesqt/Cesqt.kt`) — *PENDIENTE POR LICENCIA (no se puede automatizar)*.
   El texto literal de los 20 ítems es material protegido y no puede incluirse sin la licencia de uso (TEA Ediciones / UNIPSICO). Se ha dejado todo preparado: los textos están centralizados en el objeto `TextosCesqt`, de modo que, cuando obtengas la autorización, solo tendrás que pegar 20 frases en un único sitio, sin tocar la lógica. Mientras tanto el sistema compila y funciona con marcadores.

2. **Directorio del COP** (`data/local/seed/SedesCopSeed.kt`) — *HECHO*.
   Las 52 demarcaciones (50 provincias + Ceuta y Melilla) están mapeadas a su colegio territorial real, con nombre, web y teléfono tomados del documento oficial "Colegios Territoriales" del Consejo General de la Psicología (cop.es). Verifica los teléfonos/URLs antes de la entrega por si han cambiado; algunas webs oficiales aún usan `http://`.

3. **Flujo completo del CESQT en el chatbot** (`ui/chatbot/ChatbotViewModel.kt`) — *HECHO*.
   El ViewModel recorre los 20 ítems, presenta la escala 0–4, introduce puentes de transición entre dimensiones (§2.3.1), acumula respuestas, llama a `repository.registrarCesqt(...)`, recalcula el índice y, si procede, lanza la derivación. La pantalla muestra una barra de progreso.

4. **Solicitud de permisos de Health Connect** (`ui/devices/DevicesScreen.kt`) — *HECHO*.
   Se usa `rememberLauncherForActivityResult` con `PermissionController.createRequestPermissionResultContract()`. Hay un botón "Conceder permisos de salud" y un aviso si Health Connect no está disponible en el dispositivo.

5. **Iconos de la app** (`res/mipmap-*`, `res/drawable`) — *HECHO*.
   Se incluye un icono adaptativo (fondo verde + brote blanco, sin rojo) en `mipmap-anydpi-v26`, más PNG de respaldo en todas las densidades. Puedes regenerarlo con el asistente Image Asset de Android Studio si quieres un diseño más pulido.




## Cambios de la versión 4.0 (definitiva)

- **IA local opcional integrada** (`data/ia/`): la app puede cargar un modelo pequeño (recomendado: Gemma 3 1B int4, fichero `.task` de ~529 MB) mediante la API LLM Inference de Google AI Edge (`com.google.mediapipe:tasks-genai:0.10.27`). Bajo el principio «el motor decide, el modelo redacta», el modelo SOLO reformula tres tipos de mensajes ya validados (desculpabilización, pauta de afrontamiento y cierre de la EMA); nunca los ítems del cuestionario, los acuses ni el flujo de derivación. Toda salida pasa por `ValidadorSalida` y, si no es válida, se muestra la plantilla original. Sin modelo importado o con el interruptor apagado, la app funciona exactamente igual que antes.
- **Modelo integrado de serie (v4.1)**: la app aprovisiona el modelo automáticamente en el primer arranque si la compilación lo incluye en `app/src/main/assets/modelo_local.task` (instrucciones en `assets/LEEME_MODELO.txt`). El usuario NO hace nada: la IA queda activa por defecto (con interruptor para apagarla). Este es un paso único del DESARROLLADOR, no del usuario: el binario no puede distribuirse en el repositorio porque su licencia exige aceptación individual y por su tamaño. Recomendado para que "no ocupe mucho": **Gemma 3 270M int4 (~300 MB)**, más rápido en gama media; alternativa más capaz: Gemma 3 1B int4 (~529 MB). La importación manual por SAF queda como vía alternativa.
- **Diagnóstico de la cadena Zepp → Health Connect**: la pantalla Dispositivos muestra ahora qué llegó en la última lectura (Sueño / FC reposo / RMSSD). Nota: verificado en dispositivo real (Amazfit + Zepp, julio 2026): Zepp exporta sueño, frecuencia cardíaca en reposo Y variabilidad (RMSSD). No obstante, el motor está preparado para que falte cualquiera de las ramas, ya que otras aplicaciones de fabricante sí omiten algunas métricas.
- **Endurecimiento**: `android:allowBackup="false"` (nadie puede extraer la BD por copia de seguridad) y `networkSecurityConfig` que bloquea todo tráfico en claro (la app, además, no hace ninguna llamada de red). En producción cabría añadir `FLAG_SECURE` para bloquear capturas de pantalla; no se activa en el prototipo para poder grabar la demo.

## Cambios de la versión 3.3

- **Cuestionario funcional de extremo a extremo**: los 20 marcadores se han sustituido por **ítems provisionales originales** (mismas 4 dimensiones, misma escala 0–4, mismo orden). NO son los enunciados oficiales del CESQT (protegidos; pendiente licencia TEA/UNIPSICO): el flag `TextosCesqt.ITEMS_PROVISIONALES` lo indica y el chatbot lo declara al iniciar el test. Con la licencia, basta sustituir 20 cadenas.
- **Acuses de recibo con valencia**: tras cada respuesta, el asistente entiende si es buena o preocupante según la dimensión (Ilusión alta = positivo; Desgaste/Indolencia/Culpa altas = preocupante) y responde en consecuencia, con variación (`GeneradorMensajes.acuseCesqt`). La EMA también: si el día fue bien, el asistente se alegra; nunca asume malestar.
- **Cifrado SQLCipher + Keystore** (ver salvaguardas).

## Cambios de la versión 3.1 (revisión ética de la interfaz)

- **Inicio sin números**: la pantalla de inicio ya no muestra la puntuación 0–100. Mostrar una cifra baja a quien ya se siente mal refuerza el malestar; ahora comunica una banda cualitativa con mensaje de apoyo ("Buen momento" / "Vas haciendo camino" / "Hoy toca cuidarse"), siempre orientada a la acción. El valor numérico sigue existiendo internamente para el motor.
- **Nueva pestaña Actividades**: biblioteca de pautas (sueño, mindfulness/ACT, reestructuración, apoyo social) consultable bajo demanda, con las categorías sugeridas por el motor destacadas primero. Materializa el "modelo de interacción bajo demanda" (§2.3.2) y la agencia del usuario (P3).
- **Dispositivos simplificado**: un único botón "Conectar y actualizar datos" que pide los permisos de Health Connect si faltan y sincroniza si ya están concedidos.
- **Transparencia del chatbot**: el asistente declara en su primer mensaje que no es una IA generativa, sino un flujo guiado con mensajes predefinidos y cuestionarios validados (nunca improvisa consejos clínicos). El flujo CESQT está completo; en pantalla, cada pregunta indica claramente que el enunciado oficial se incorporará con la licencia del instrumento.

## Tests

`app/src/test/` contiene tests unitarios de la lógica de dominio: `MotorRiesgoTest` (ecuación de riesgo y degradación elegante), `GestorCopingTest` (los cuatro escenarios A–D y la cláusula de derivación, incluido el disparador de Culpa del Perfil 2) y `CalculadoraCesqtTest` (inversión de la escala de Ilusión, exclusión de Culpa del global, normalización). Ejecútalos con `./gradlew test`.

## Salvaguardas éticas y RGPD implementadas

- **Consentimiento informado** (`ui/onboarding/ConsentScreen.kt`): pantalla previa a cualquier recogida de datos que explica qué se mide, para qué, y que los datos no salen del dispositivo. La app queda bloqueada hasta su aceptación (RGPD art. 7).
- **Línea de crisis 024**: la derivación del Perfil 2 ofrece siempre, además del COP, la línea 024 de atención a la conducta suicida (24/7), porque el colegio profesional tiene horario de oficina y la app no es un servicio de emergencias.
- **Portabilidad de datos** (RGPD art. 20): botón "Exportar mis datos" en la pantalla de dispositivos, que genera un JSON con el histórico y abre el selector de compartir del sistema.
- **Cifrado de la base de datos (IMPLEMENTADO)**: la BD Room se cifra con SQLCipher (AES-256) y la passphrase se genera aleatoriamente y se custodia en `EncryptedSharedPreferences` respaldadas por el Android Keystore (`data/local/security/GestorClaveBd.kt`). Aunque se extraiga el fichero de la BD, los datos de salud son ilegibles sin la clave.
- **Uso personal, nunca corporativo**: el consentimiento declara explícitamente que la herramienta no está diseñada para que un empleador supervise a su plantilla (encaje con el Reglamento de IA, art. 5.1.f).

## Decisiones de diseño reflejadas en el código

- **Offline-first**: no hay capa de red; todo el dominio se ejecuta localmente y los datos se persisten con Room.
- **Sin GPS** (principio P6): el `AndroidManifest` no declara permisos de localización; la derivación pregunta la provincia por chat.
- **Paleta sin rojo** (principio P4): definida en `ui/theme/Color.kt`; el semáforo usa verde/ámbar/gris.
- **Degradación elegante** (§4.4): el `MotorRiesgo` opera solo con la rama psicométrica si no hay biometría fresca.

## Verificar la cadena Amazfit → Zepp → Health Connect

1. En Zepp: Perfil → Añadir cuentas (o «Acceso a datos de terceros») → **Health Connect** → activa permisos de **Sueño** y **Frecuencia cardíaca**.
2. En Ajustes de Android: batería de Zepp **sin restricciones** (si Android duerme Zepp, no vuelca datos).
3. Duerme una noche con el reloj puesto y abre Zepp por la mañana (fuerza la sincronización reloj→Zepp→HC).
4. Comprueba en la app **Health Connect** → Explorar datos: deben aparecer sesiones de sueño y FC con origen «Zepp».
5. En nuestra app: Dispositivos → «Conectar y actualizar datos». La tarjeta «Última lectura» mostrará ✓ en Sueño y FC; **si alguna métrica saliera «sin datos»**, el índice se calcula igualmente con las disponibles (renormalización documentada en la memoria).

## Probar SIN el reloj (móvil real o emulador)

La app es agnóstica del hardware: solo lee de Health Connect, así que puedes validar todo el flujo con datos sintéticos. Dos vías:

1. **Botón integrado (recomendado)**: compila en variante *debug*, ve a Dispositivos → tarjeta «Modo desarrollo» → **Generar datos de prueba**. Concede los permisos de escritura que pedirá Health Connect y la app insertará una noche simulada (sueño 23:40–07:15, FC reposo 58, RMSSD 42) y la leerá al momento: semáforo en verde, «Última lectura» con ✓✓✓ y el índice calculándose con biometría real. En la variante *release* esta tarjeta y sus permisos no existen.
2. **Health Connect Toolbox** (herramienta oficial de Google para desarrolladores): permite insertar manualmente registros de sueño/FC/VFC en Health Connect; útil para probar casos concretos (noches cortas, sin RMSSD, etc.).

En **emulador**: usa una imagen con Play Store, API 34 o superior (Health Connect viene integrado en el sistema); en imágenes anteriores instala Health Connect desde Play. En tu Oppo solo necesitas la app Health Connect instalada.

Nota: esto valida el tramo del que es responsable la app (Health Connect → motor → índice → interfaz). El tramo reloj→Zepp→Health Connect se comprueba con la lista de verificación de arriba el día que tengas el reloj; los datos que escribe Zepp entran por el mismo camino que los sintéticos.

## Aviso «16 KB page size» en emuladores Android 15+

Al arrancar en un emulador con páginas de memoria de 16 KB (p. ej. Pixel API 35+) puede aparecer el diálogo «This app isn't 16 KB compatible»: algunas librerías nativas de terceros (SQLCipher, motor de inferencia de MediaPipe) aún no vienen alineadas a 16 KB. **Es solo un aviso**: el sistema ejecuta la app en modo compatible y todo funciona. En el dispositivo objetivo del proyecto (Oppo Reno 4 5G, páginas de 4 KB) el aviso no existe. Para producción bastaría con actualizar esas dependencias cuando publiquen builds alineados (p. ej. `net.zetetic:sqlcipher-android` moderno). Documentado como nota técnica, no como defecto.
