
# Diseño de modelos y colecciones — Bus Popayán

## Información general

Proyecto: Bus Popayán
Asignatura: Bases de Datos Avanzadas
Fase: 2 — Diseño

Base de datos: buspopayan
Motor: MongoDB
Administrador: MongoDB Compass
Backend previsto: Node.js + Express + Mongoose
Aplicación: Android nativo con Kotlin

## 1. Colecciones existentes

La base de datos contiene las siguientes colecciones:

1. rutas
2. paraderos
3. buses
4. usuarios
5. eta
6. ubicaciones
7. alertas
8. notificaciones
9. favoritos
10. viajes

Estas colecciones ya existen en MongoDB y contienen
registros de prueba.

## 2. Modelo: rutas

Colección: rutas

Campos comprobados:
- _id: ObjectId
- ruta_id: String
- nombre: String
- numero: String
- origen: String
- destino: String
- estado: String

Ejemplo de documento:

{
"ruta_id": "R001",
"nombre": "Ruta Centro",
"numero": "1",
"origen": "Norte",
"destino": "Centro",
"estado": "Activa"
}

Validaciones propuestas:
- ruta_id obligatorio y único.
- nombre y numero obligatorios.
- origen y destino obligatorios.
- estado limitado a valores definidos.

Relaciones:
- Una ruta tiene varios paraderos.
- Una ruta puede tener varios buses.
- Una ruta puede aparecer en varios viajes.

Historias: US-01, US-07, US-08.

## 3. Modelo: paraderos

Colección: paraderos

Campos propuestos:
- paradero_id: String
- nombre: String
- direccion: String
- ruta_id: String
- latitud: Number
- longitud: Number
- estado: String
- accesibilidad: Object

Validaciones:
- Identificador único.
- Coordenadas geográficas válidas.
- Ruta asociada existente.

Relación:
- Cada paradero se asocia con una ruta.
- Un paradero puede tener varios registros ETA.

Historias: US-01, US-03, US-07, US-09.


## 4. Modelo: buses

Colección: buses

Estado: existente en MongoDB Compass.
Documentos registrados: 5.

Campos reales observados:
- _id: ObjectId
- bus_id: String
- placa: String
- ruta_id: String
- conductor: String
- estado: String

Ejemplo de documento:

{
"bus_id": "B001",
"placa": "ABC123",
"ruta_id": "R001",
"conductor": "Pedro Ruiz",
"estado": "En servicio"
}

Relaciones:
- ruta_id relaciona el bus con la colección rutas.
- Un bus puede tener múltiples ubicaciones.
- Un bus puede tener registros de tiempo estimado (ETA).

Validaciones propuestas para la fase 3:
- bus_id obligatorio y único.
- placa obligatoria y única.
- ruta_id debe corresponder a una ruta existente.
- conductor obligatorio.
- estado debe pertenecer a los estados permitidos.

Campos adicionales propuestos:
- operador_id: String
- ultima_actualizacion: Date

Estos campos adicionales no están presentes en los
documentos revisados. Su implementación requerirá
adaptar el esquema y, si corresponde, migrar los datos.

Historias relacionadas:
US-01, US-04, US-06, US-08 y US-10.

## 5. Modelo: usuarios

Colección: usuarios

Campos propuestos:
- usuario_id: String
- nombre: String
- correo: String
- rol: String
- preferencias: Object
- estado: String

Roles propuestos:
- pasajero
- operador
- administrador

Validaciones:
- Correo único.
- Rol permitido.
- Datos obligatorios.

Historias: US-03, US-05, US-06, US-08, US-09.

## 6. Modelo: eta

Colección: eta

Campos propuestos:
- eta_id: String
- bus_id: String
- ruta_id: String
- paradero_id: String
- tiempo_estimado: Number
- fecha_actualizacion: Date

Validaciones:
- Tiempo estimado no negativo.
- Bus y paradero existentes.
- Fecha de actualización válida.

Historias: US-01, US-07.

## 7. Modelo: ubicaciones

Colección: ubicaciones

Campos propuestos:
- ubicacion_id: String
- evento_id: String
- bus_id: String
- latitud: Number
- longitud: Number
- fecha: Date
- estado_validacion: String
- es_estimada: Boolean
- sincronizada: Boolean

Validaciones:
- Latitud entre -90 y 90.
- Longitud entre -180 y 180.
- evento_id único para evitar duplicados.
- Rechazar datos cronológicamente anteriores
  a la última ubicación aceptada.

Auditoría propuesta:
- ubicacion_anterior
- ubicacion_nueva
- operador_id
- fecha_correccion
- motivo

Historias: US-04, US-06, US-10.

## 8. Modelo: alertas

Colección: alertas

Campos propuestos:
- alerta_id: String
- bus_id: String
- ruta_id: String
- demora_minutos: Number
- motivo: String
- fecha: Date
- estado: String

Validaciones:
- Demora no negativa.
- Bus y ruta existentes.
- Si demora_minutos supera 7,
  generar evento de notificación.

Historias: US-02, US-07.

## 9. Modelo: notificaciones

Colección: notificaciones

Campos propuestos:
- notificacion_id: String
- usuario_id: String
- alerta_id: String
- titulo: String
- mensaje: String
- fecha: Date
- leida: Boolean

Validaciones:
- Usuario existente.
- Mensaje obligatorio.
- Estado de lectura booleano.

Historia: US-02.

## 10. Modelo: favoritos

Colección: favoritos

Campos propuestos:
- favorito_id: String
- usuario_id: String
- tipo: String
- referencia_id: String
- fecha_creacion: Date

Tipos permitidos:
- ruta
- paradero

Validaciones:
- Usuario existente.
- Referencia existente.
- Evitar favoritos duplicados.

Historia: US-03.

## 11. Modelo: viajes

Colección: viajes

Campos propuestos:
- viaje_id: String
- usuario_id: String
- ruta_id: String
- fecha: Date
- hora_inicio: String
- hora_fin: String
- duracion_minutos: Number

Validaciones:
- Usuario y ruta existentes.
- Fecha válida.
- Hora de finalización posterior al inicio.

Historia: US-05.

## 12. Relaciones entre colecciones

rutas → paraderos
rutas → buses
rutas → viajes
buses → ubicaciones
buses → eta
buses → alertas
paraderos → eta
usuarios → favoritos
usuarios → viajes
usuarios → notificaciones
usuarios → buses (operador)

Las relaciones se implementarán mediante
identificadores de referencia.

## 13. Índices recomendados

rutas:
- Índice único en ruta_id.

paraderos:
- Índice en ruta_id.
- Índice único en paradero_id.

buses:
- Índice único en bus_id.
- Índice único en placa.

usuarios:
- Índice único en correo.

ubicaciones:
- Índice único en evento_id.
- Índice compuesto en bus_id y fecha.

eta:
- Índice en paradero_id.

viajes:
- Índice compuesto en usuario_id y fecha.

favoritos:
- Índice único compuesto en
  usuario_id, tipo y referencia_id.

Los índices se crearán y verificarán
durante la implementación del backend.

## 14. Consideraciones técnicas

- MongoDB almacena los documentos.
- MongoDB Compass permite inspeccionarlos.
- Mongoose implementará los esquemas y validaciones.
- Express expondrá los endpoints REST.
- Retrofit consumirá la API desde Android.
- Las operaciones administrativas requerirán permisos.
- La sincronización GPS deberá controlar duplicados.
- Redis es opcional y requiere implementación separada.
- Los datos de prueba no equivalen a funcionalidades
  completamente implementadas.

## 15. Estado del diseño

Colecciones: existentes.
Modelo rutas: campos comprobados en Compass.
Otros modelos: estructuras propuestas pendientes
de contrastar con los documentos existentes.

Implementación de esquemas Mongoose:
pendiente para la fase 3.

Pruebas de persistencia y validación:
pendientes para la fase 4.


## 16. Verificación de los modelos existentes

Se realizó una revisión de las diez colecciones
de la base de datos buspopayan utilizando
MongoDB Compass y consultas con mongosh.

### Estructuras reales verificadas

rutas:
- ruta_id: String
- nombre: String
- numero: String
- origen: String
- destino: String
- estado: String

paraderos:
- paradero_id: String
- nombre: String
- direccion: String
- ruta_id: String
- estado: String
- latitud: Number
- longitud: Number

buses:
- bus_id: String
- placa: String
- ruta_id: String
- conductor: String
- estado: String

usuarios:
- usuario_id: String
- nombre: String
- email: String
- tipo: String

eta:
- eta_id: String
- bus_id: String
- ruta_id: String
- paradero_id: String
- minutos: Number

ubicaciones:
- ubicacion_id: String
- bus_id: String
- latitud: Number
- longitud: Number
- hora: String

alertas:
- alerta_id: String
- ruta_id: String
- tipo: String
- mensaje: String
- estado: String

notificaciones:
- notificacion_id: String
- usuario_id: String
- mensaje: String
- estado: String

favoritos:
- favorito_id: String
- usuario_id: String
- tipo: String
- referencia_id: String
- nombre: String

viajes:
- viaje_id: String
- usuario_id: String
- ruta_id: String
- fecha: String
- hora: String
- duracion: Number

Todas las colecciones incluyen el campo _id
de tipo ObjectId, generado por MongoDB.

### Relaciones identificadas

- rutas.ruta_id → paraderos.ruta_id
- rutas.ruta_id → buses.ruta_id
- rutas.ruta_id → viajes.ruta_id
- rutas.ruta_id → alertas.ruta_id
- buses.bus_id → ubicaciones.bus_id
- buses.bus_id → eta.bus_id
- paraderos.paradero_id → eta.paradero_id
- usuarios.usuario_id → favoritos.usuario_id
- usuarios.usuario_id → viajes.usuario_id
- usuarios.usuario_id → notificaciones.usuario_id

Estas relaciones se identificaron mediante
los campos de referencia de los documentos.

### Observaciones para la fase 3

Los modelos Mongoose deben respetar los nombres
y tipos de los campos existentes.

Los campos adicionales planteados anteriormente
son propuestas de ampliación, no campos que
ya estén implementados.

Antes de programar las reglas de negocio se
deben revisar las diez historias de usuario
y los endpoints documentados.

La implementación de validaciones, operaciones
CRUD y pruebas funcionales corresponde
a las fases 3 y 4.
