
# Diseño de Endpoints — Bus Popayán

## Información general

Proyecto: Bus Popayán
Asignatura: Bases de Datos Avanzadas
Backend: Node.js + Express
Base de datos: MongoDB
Cliente: Android Studio (Kotlin + Retrofit)

URL base: http://localhost:3000/api

Nota: En el emulador Android se utilizará
http://10.0.2.2:3000/api para acceder al backend local.

Los endpoints siguientes son contratos propuestos para
la fase 3. Todavía no están todos implementados.

## US-01 — Consulta de tiempo de llegada

GET /eta/:paradero_id

Descripción: Consultar próximos buses y tiempos
estimados de llegada a un paradero.

Respuesta esperada: 200 OK

{
"paradero_id": "P001",
"buses": [
{
"bus_id": "B001",
"ruta_id": "R001",
"tiempo_estimado": 5
}
]
}

Regla: Actualizar la consulta cada 10 segundos
desde Android.

## US-02 — Alertas de retraso

GET /alertas?usuario_id=U001

Descripción: Consultar alertas del usuario.

POST /alertas

Descripción: Registrar una alerta de retraso.

Datos de entrada:

{
"bus_id": "B001",
"ruta_id": "R001",
"demora_minutos": 8,
"motivo": "Tráfico en el centro"
}

Resultados esperados:
200 OK: Alertas consultadas.
201 Created: Alerta registrada.
400 Bad Request: Datos inválidos.

Regla: Las demoras superiores a 7 minutos
deben generar una notificación push.

## US-03 — Guardado de favoritos

GET /favoritos?usuario_id=U001

POST /favoritos

{
"usuario_id": "U001",
"tipo": "ruta",
"referencia_id": "R001"
}

DELETE /favoritos/:id

Resultados esperados:
200 OK: Favoritos consultados o eliminados.
201 Created: Favorito guardado.
404 Not Found: Favorito inexistente.

## US-04 — Transmisión de ubicación

POST /ubicaciones

{
"bus_id": "B001",
"latitud": 2.4448,
"longitud": -76.6147,
"fecha": "2026-10-15T20:00:00Z"
}

Resultados esperados:
201 Created: Ubicación registrada.
400 Bad Request: Coordenadas inválidas.
403 Forbidden: Operador no autorizado.

Regla: El dispositivo debe almacenar temporalmente
las posiciones si pierde la conexión.

## US-05 — Historial de viajes

GET /viajes?usuario_id=U001

POST /viajes

{
"usuario_id": "U001",
"ruta_id": "R001",
"fecha": "2026-10-15",
"hora_inicio": "08:00",
"hora_fin": "08:30"
}

DELETE /viajes/:id

Resultados esperados:
200 OK: Historial consultado o registro eliminado.
201 Created: Viaje registrado.
404 Not Found: Viaje inexistente.

## US-06 — Corrección de ubicación GPS

PATCH /ubicaciones/:id/correccion

{
"latitud": 2.4450,
"longitud": -76.6150,
"motivo": "Corrección GPS"
}

Resultados esperados:
200 OK: Corrección aceptada.
400 Bad Request: Coordenadas inválidas.
403 Forbidden: Operador no autorizado.
404 Not Found: Ubicación inexistente.
409 Conflict: Corrección inconsistente.

Reglas:
- Validar pertenencia razonable a la ruta.
- Conservar última posición válida.
- Registrar auditoría de cada corrección.

## US-07 — Rutas alternativas

GET /rutas/alternativas?ruta_id=R001&destino=Centro

Respuesta esperada: 200 OK

{
"ruta_original": "R001",
"alternativas": [
{
"ruta_id": "R002",
"nombre": "Ruta Sur",
"paradero_conexion": "P002",
"eta_minutos": 8
}
]
}

Reglas:
- Considerar rutas activas y recorridos autorizados.
- Informar cuando no existan alternativas válidas.
- Objetivo de respuesta inferior a 2 segundos.

## US-08 — Administración de rutas y vehículos

GET /rutas
GET /rutas/:id
POST /rutas
PUT /rutas/:id
PATCH /rutas/:id/estado
DELETE /rutas/:id

GET /buses
POST /buses
PUT /buses/:id
PATCH /buses/:id/asignacion

Ejemplo de creación de ruta:

{
"ruta_id": "R006",
"nombre": "Ruta Nueva",
"numero": "6",
"origen": "Norte",
"destino": "Centro",
"estado": "Activa"
}

Resultados esperados:
200 OK: Consulta o modificación exitosa.
201 Created: Recurso creado.
400 Bad Request: Datos inválidos.
403 Forbidden: Sin permisos.
404 Not Found: Recurso inexistente.
409 Conflict: Duplicado o conflicto de actualización.

Reglas:
- Identificadores únicos.
- Validar operadores y vehículos.
- Registrar auditoría.
- Impedir asignaciones a recursos suspendidos.

## US-09 — Preferencias de accesibilidad

GET /usuarios/:id/preferencias

PUT /usuarios/:id/preferencias

{
"texto_ampliado": true,
"alto_contraste": true,
"avisos_sonoros": false
}

Resultados esperados:
200 OK: Preferencias consultadas o actualizadas.
400 Bad Request: Datos inválidos.
404 Not Found: Usuario inexistente.

Regla: Conservar preferencias entre sesiones.

## US-10 — Validación y sincronización

POST /ubicaciones/sincronizar

{
"bus_id": "B001",
"posiciones": [
{
"evento_id": "EV001",
"latitud": 2.4448,
"longitud": -76.6147,
"fecha": "2026-10-15T20:00:00Z"
}
]
}

Respuesta esperada: 200 OK

{
"aceptadas": 1,
"rechazadas": 0,
"duplicadas": 0
}

Reglas:
- Validar vehículo y asignación vigente.
- Rechazar posiciones antiguas.
- Evitar duplicados usando evento_id.
- Aislar registros inconsistentes.
- Procesar posiciones en orden cronológico.
- Invalidar caché si se implementa Redis.

## Consideraciones generales

- Utilizar JSON en solicitudes y respuestas.
- Implementar validación de datos.
- Centralizar el manejo de errores.
- Proteger operaciones administrativas.
- Utilizar MongoDB para persistencia.
- Mantener consistencia entre la API y Retrofit.
- Documentar las funcionalidades implementadas.
