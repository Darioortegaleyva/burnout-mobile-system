# Guía de despliegue

Esta guía cubre desde clonar el repositorio hasta tener la aplicación
funcionando en un teléfono. Está pensada para alguien que no ha trabajado
antes con este proyecto.

---

## 1. Requisitos

| Elemento | Versión / nota |
|---|---|
| Android Studio | Ladybug o posterior |
| JDK | 17 (incluido con Android Studio) |
| Dispositivo | Android 8.0 o superior. Recomendado Android 14+ |
| Espacio libre | ~1 GB durante la compilación |

No hace falta servidor, base de datos ni cuenta de ningún servicio: la
aplicación funciona por completo dentro del teléfono.

---

## 2. Compilar

```bash
git clone <url-del-repositorio>
cd burnout-app
```

Abre la carpeta desde Android Studio (*File → Open*) y espera a que termine la
sincronización de Gradle. La primera vez descarga las dependencias y puede
tardar varios minutos.

Después, `Run ▶` con un dispositivo seleccionado.

---

## 3. Añadir el modelo de lenguaje (opcional)

La aplicación **funciona sin modelo**: el asistente responde con el texto
documental íntegro, que es su comportamiento de respaldo. El modelo solo
aporta variedad en la redacción.

El fichero del modelo **no está en el repositorio** por dos motivos: su
licencia exige aceptación individual por parte de quien lo descarga, y su
tamaño (cientos de MB) desaconseja versionarlo.

Para incorporarlo:

1. Entra en la colección **litert-community** de Hugging Face.
2. Elige **Gemma 3 270M** (~237 MB, recomendado) o **Gemma 3 1B** (~529 MB,
   más capaz pero más lento en gama media). Acepta la licencia de Gemma.
3. Descarga el fichero `.task` cuantizado (int4 / q4). Evita las variantes
   marcadas como `web`: están empaquetadas para otro runtime.
4. Renómbralo a **`modelo_local.task`**.
5. Colócalo en **`app/src/main/assets/`**.
6. Recompila.

En el primer arranque la aplicación copia el modelo a su almacenamiento
privado y activa la reformulación por defecto. El usuario conserva un
interruptor para desactivarla en la pantalla de Dispositivos.

---

## 4. Health Connect y el dispositivo vestible

La aplicación **nunca se comunica con el reloj directamente**: solo lee de
Google Health Connect. Cualquier wearable sirve mientras su aplicación de
fabricante escriba ahí.

1. **Health Connect**: viene integrado en Android 14+. En versiones
   anteriores, instálalo desde Play Store.
2. **En la app del fabricante** (Zepp, Samsung Health, Fitbit…): busca la
   opción de compartir con Health Connect y concede permisos de **sueño** y
   **frecuencia cardíaca**.
3. **Quita la restricción de batería** a esa aplicación: si Android la
   suspende, dejará de volcar datos.
4. Duerme con el dispositivo puesto. Por la mañana, abre primero la app del
   fabricante para forzar la sincronización y después esta aplicación →
   *Dispositivos* → **Conectar y actualizar datos**.

> El índice no incorpora biometría hasta disponer de **tres días** de
> histórico: antes no puede establecerse una línea base individual fiable.

---

## 5. Probar sin dispositivo vestible

En la compilación de depuración, *Dispositivos* incluye una tarjeta
**Modo desarrollo** con tres acciones:

- **Generar datos de prueba** — inserta una noche simulada.
- **Generar semana de prueba** — inserta siete noches, seis con un patrón
  estable que forma la línea base y una última claramente peor. Es la opción
  para ver el índice multimodal completo en funcionamiento.
- **Restablecer la app** — devuelve todo al estado de primer arranque.

Los datos sintéticos recorren exactamente el mismo camino de código que los
reales, ya que entran por Health Connect.

---

## 6. Generar el instalable para otras personas

**Versión de depuración** (rápida, incluye las herramientas de desarrollo):

`Build → Build Bundle(s) / APK(s) → Build APK(s)` → el fichero queda en
`app/build/outputs/apk/debug/`.

**Versión de distribución** (sin herramientas de desarrollo ni permisos de
escritura, es la que conviene compartir):

`Build → Generate Signed Bundle / APK → APK → Create new…` para crear el
almacén de claves, y elige la variante **release**.

> El almacén de claves y sus contraseñas **no deben subirse al repositorio**;
> están excluidos en `.gitignore`.

Comparte el APK por el medio que prefieras. Quien lo instale tendrá que
autorizar la instalación de aplicaciones de origen desconocido, que es lo
habitual fuera de las tiendas oficiales.

---

## 7. Aviso a quien vaya a probarla

Conviene decirlo antes de que la usen:

- Es un **prototipo académico**, no un producto sanitario.
- **No emite diagnósticos**: ofrece una orientación y, si detecta una
  situación seria, deriva a ayuda profesional.
- Los enunciados del cuestionario son **provisionales**, de redacción propia
  y no los oficiales del CESQT, que están protegidos. La propia aplicación lo
  declara antes de empezar.
- **Los datos no salen del teléfono** y la base de datos está cifrada.

---

## 8. Problemas frecuentes

| Síntoma | Causa habitual |
|---|---|
| Falla al instalar sobre una versión anterior | Cambió el esquema de la base de datos. Desinstala primero. |
| «Sin datos» en todas las métricas | La app del fabricante aún no ha escrito nada. Comprueba en Health Connect → Datos y acceso. |
| El índice ignora la biometría | Faltan días de histórico (mínimo tres). |
| Aviso «16 KB page size» en emulador | Aviso de compatibilidad de librerías nativas en Android 15+. No afecta al funcionamiento. |
| El asistente responde siempre con el texto literal | No hay modelo importado, o está desactivado en Dispositivos. |
