
# Historias de Usuario – Bus Popayán

**Proyecto:** Bus Popayán
**Asignatura:** Bases de Datos Avanzadas
**Fase:** 2 – Diseño
**Tecnologías:** Android Studio (Kotlin), Node.js, Express y MongoDB.

## US-01 — Consulta de tiempo de llegada

**Historia:** Como ciudadano que utiliza el transporte público a diario, quiero abrir la aplicación y consultar exactamente en cuántos minutos llegará el bus a mi paradero, para organizar mejor mi tiempo y evitar esperas innecesarias.

**Criterios de aceptación:**
1. Muestra la lista de próximos buses al seleccionar un paradero.
2. Indica el tiempo restante expresado claramente en minutos.
3. Se actualiza de forma automática cada 10 segundos.

## US-02 — Recepción de alertas de retraso

**Historia:** Como usuario frecuente de los buses de Popayán, quiero recibir alertas automáticas en mi celular si el bus se retrasa o cambia su recorrido, para tomar rutas alternativas a tiempo.

**Criterios de aceptación:**
1. Envía notificación push si la demora supera los 7 minutos.
2. Explica brevemente la razón del retraso (ej. tráfico en el centro).
3. Recomienda paraderos o rutas cercanas disponibles.

## US-03 — Guardado de favoritos

**Historia:** Como pasajero habitual, quiero guardar mis rutas y paraderos preferidos (casa, trabajo, universidad), para ingresar a sus horarios de forma directa sin buscar en el mapa cada vez.

**Criterios de aceptación:**
1. Incluye botón de favoritos (ícono de estrella) en cada ruta/paradero.
2. Despliega accesos directos guardados en el inicio de la app.
3. Mantiene la configuración guardada entre sesiones.

## US-04 — Transmisión de ubicación por el operador

**Historia:** Como conductor de bus de servicio público, quiero que mi dispositivo envíe la posición geográfica del vehículo automáticamente en segundo plano, para informar a los pasajeros sin distraer mi conducción.

**Criterios de aceptación:**
1. Transmite datos automáticamente al iniciar el recorrido del vehículo.
2. Funciona de fondo sin requerir acción manual del conductor.
3. Almacena en memoria local las coordenadas en caso de pérdida de red.

## US-05 — Consulta de historial de viajes

**Historia:** Como usuario de la plataforma, quiero revisar el historial de mis viajes pasados, para llevar un registro de mis tiempos de traslado y rutas más utilizadas.

**Criterios de aceptación:**
1. Menú accesible de 'Mis Viajes' dentro del perfil del usuario.
2. Muestra un listado detallado por fecha, horario y ruta tomada.
3. Permite gestionar o eliminar registros guardados.

## US-06 — Corrección y validación de ubicación del bus

**Historia:** Como operador de bus, quiero reportar inconsistencias en la ubicación GPS, para mantener confiable la posición mostrada a los pasajeros.

**Criterios de aceptación:**
1. Validar que las coordenadas sean válidas y correspondan razonablemente con la ruta asignada.
2. Si la ubicación aparece fuera de ruta, solicitar una nueva lectura antes de confirmarla.
3. Si se pierde el GPS, conservar la última posición válida e indicar que la ubicación es estimada.
4. Solo el operador autorizado puede corregir la ubicación de su vehículo; si no tiene permisos, se rechaza el cambio.
5. Registrar cada corrección con fecha, vehículo y ubicación anterior y nueva.

## US-07 — Recomendación de rutas alternativas

**Historia:** Como pasajero, quiero recibir alternativas cuando mi ruta tenga interrupciones o desvíos, para continuar mi viaje con información útil.

**Criterios de aceptación:**
1. Identificar rutas alternativas activas que permitan acercarse al destino.
2. Mostrar paraderos de conexión y ETA cuando existan datos suficientes.
3. Considerar tráfico, recorridos autorizados y posiciones recientes de los buses.
4. Si no hay alternativas válidas o los datos están desactualizados, informarlo sin presentar estimaciones como confirmadas.
5. Buscar una respuesta inferior a 2 segundos como objetivo, sujeto a pruebas de rendimiento.

## US-08 — Administración de rutas y vehículos

**Historia:** Como administrador, quiero gestionar rutas y asignar vehículos y operadores autorizados, para mantener actualizada la información operativa.

**Criterios de aceptación:**
1. Registrar rutas con identificador único, recorrido, paraderos y estado.
2. Validar que vehículo y operador existan y estén autorizados antes de asignarlos.
3. Rechazar identificadores duplicados, vehículos inexistentes o asignaciones no permitidas.
4. Al suspender una ruta o vehículo, impedir nuevas asignaciones que dependan de ellos.
5. Registrar quién realizó cada cambio y cuándo; detectar modificaciones simultáneas conflictivas.

## US-09 — Preferencias de accesibilidad

**Historia:** Como pasajero con necesidades de accesibilidad, quiero ajustar la presentación de la información, para consultar rutas y avisos de forma autónoma.

**Criterios de aceptación:**
1. Permitir configurar texto ampliado, alto contraste y avisos sonoros.
2. Conservar las preferencias entre sesiones para usuarios autenticados.
3. Mostrar información de accesibilidad de paraderos solo cuando esté registrada y, si no existe, indicarlo como desconocido.
4. No ocultar ETA, alertas ni estados de conexión al aplicar preferencias.
5. Permitir encontrar información principal en máximo tres toques, según el requisito de usabilidad.

## US-10 — Validación y sincronización de datos

**Historia:** Como administrador, quiero detectar datos GPS inconsistentes y controlar su sincronización, para evitar que información incorrecta afecte las consultas.

**Criterios de aceptación:**
1. Validar vehículo, fecha, coordenadas y asignación vigente en cada actualización GPS.
2. Rechazar posiciones más antiguas que la última posición aceptada.
3. Aislar datos inconsistentes para revisión sin reemplazar posiciones confirmadas.
4. Al recuperar conexión, procesar datos almacenados en orden cronológico y evitar duplicados.
5. Actualizar o invalidar la caché Redis cuando no coincida con MongoDB Compass.
6. Mantener como objetivo respuestas inferiores a 2 segundos, sujeto a pruebas.

## Nota técnica

Las historias anteriores conservan los criterios del documento original. En US-10, MongoDB Compass es la herramienta de administración y MongoDB es el motor de base de datos. La necesidad de Redis se evaluará durante la implementación.
  