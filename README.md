
# Bus Popayán

Proyecto académico de transporte público desarrollado
para Ingeniería de Software II y Bases de Datos Avanzadas.

## Tecnologías

- Android Studio y Kotlin
- Retrofit para consumir servicios REST
- Node.js y Express
- MongoDB y MongoDB Compass
- Mongoose para los modelos de datos

## Organización del proyecto

app/       Aplicación Android
backend/   Preparación del servidor Node.js
docs/      Documentación de la fase 2

## Estado actual

Fase 1: Preparación completada.
Fase 2: Diseño documentado.
Fase 3: Implementación del backend pendiente.
Fase 4: Validación y pruebas pendientes.

## Ejecutar Android

1. Instalar Android Studio.
2. Abrir la carpeta Bus_Popayan.
3. Sincronizar las dependencias de Gradle.
4. Configurar los servicios y claves requeridos.
5. Seleccionar un emulador Android.
6. Ejecutar la aplicación.

Nota: Algunas funciones requieren un backend activo.
El mapa usa OpenStreetMap (osmdroid) y las rutas OSRM:
no necesitan API key ni facturación.

La dirección del backend se cambia en un solo lugar:
app/src/main/java/com/example/bus_popayan/Config.kt

## Seguimiento en vivo (demo)

Ver GUIA_DEMO.md: cómo arrancar el servidor, compartir
la ubicación desde el panel web o la app y ver la ruta
y el tiempo de llegada en tiempo real.

## Preparar el backend

1. Instalar Node.js y npm.
2. Abrir una terminal dentro de backend.
3. Ejecutar:

npm install

4. Crear el archivo .env a partir de .env.example.
5. Configurar MONGO_URI y PORT (3000).
6. Ejecutar: npm start

## Restaurar MongoDB

Se entrega por separado el archivo:

buspopayan.archive.gz

Instalar MongoDB y MongoDB Database Tools.

Ejecutar:

mongorestore --uri="mongodb://127.0.0.1:27017" --archive=buspopayan.archive.gz --gzip

Importante: restaurar sobre una base de datos
vacía para evitar conflictos con datos existentes.

## Documentación

docs/historias-usuario.md
docs/endpoints.md
docs/modelos-mongodb.md

## Pendientes para el compañero

- Implementar el servidor Express.
- Crear y verificar modelos Mongoose.
- Implementar los endpoints documentados.
- Aplicar reglas de negocio y validaciones.
- Probar endpoints con Postman o Thunder Client.
- Verificar persistencia en MongoDB.
- Realizar las demostraciones funcionales.
