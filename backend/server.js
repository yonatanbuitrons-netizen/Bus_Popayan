require("dotenv").config();

const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const http = require("http");
const os = require("os");
const path = require("path");
const { Server } = require("socket.io");
const demo = require("./datos-demo");
const { conectarConReintentoDns, explicarError } = require("./mongo-util");
const { crearRutasAuth } = require("./auth");

// ==========================================
// 1. CONFIGURACIÓN DEL SERVIDOR
// ==========================================

const app = express();
const server = http.createServer(app);

app.use(cors());
app.use(express.json());

// Página web para que el compañero transmita su ubicación
app.use(express.static(path.join(__dirname, "public")));

function mongoListo(res) {
  if (mongoose.connection.readyState === 1) return true;
  res.status(503).json({
    error: "La base de datos no está conectada",
  });
  return false;
}

const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI;

const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"],
  },
});

// Últimas posiciones en vivo por ruta
const posicionesActivas = new Map();

// socket.id -> ruta que ese cliente está transmitiendo
const transmisores = new Map();

// ==========================================
// 2. ESTADO DEL BACKEND
// ==========================================

app.get("/api", (req, res) => {
  res.json({
    proyecto: "Bus Popayán",
    estado: "Backend funcionando",
    database:
      mongoose.connection.readyState === 1
        ? "Conectada"
        : "Desconectada",
    websocket: "Socket.IO configurado",
    en_vivo: posicionesActivas.size,
  });
});

app.get("/api/en-vivo", (req, res) => {
  res.json([...posicionesActivas.values()]);
});

// ==========================================
// 2.1 AUTENTICACIÓN (registro e inicio de sesión)
// ==========================================
app.use(
  "/api/auth",
  crearRutasAuth(() =>
    mongoose.connection.readyState === 1 ? mongoose.connection.db : null
  )
);

// ==========================================
// 3. CONSULTAR RUTAS
// ==========================================

async function leerColeccion(nombre, filtro) {
  if (mongoose.connection.readyState !== 1) return [];
  try {
    return await mongoose.connection.db
      .collection(nombre)
      .find(filtro)
      .toArray();
  } catch (error) {
    console.error(`Error leyendo ${nombre}:`, error.message);
    return [];
  }
}

function unirSinRepetir(deMongo, deDemo, campoId) {
  const ids = new Set(deMongo.map((d) => d[campoId]));
  return [...deMongo, ...deDemo.filter((d) => !ids.has(d[campoId]))];
}

app.get("/api/rutas", async (req, res) => {
  const { estado } = req.query;
  const filtro = estado ? { estado: String(estado) } : {};

  const deMongo = await leerColeccion("rutas", filtro);
  const deDemo = demo.rutas.filter(
    (r) => !estado || r.estado === String(estado)
  );

  res.json(unirSinRepetir(deMongo, deDemo, "ruta_id"));
});

// ==========================================
// 4. CONSULTAR PARADEROS
// ==========================================

app.get("/api/paraderos", async (req, res) => {
  const { ruta_id } = req.query;
  const filtro = ruta_id ? { ruta_id: String(ruta_id) } : {};

  const deMongo = await leerColeccion("paraderos", filtro);
  const deDemo = demo.paraderos.filter(
    (p) => !ruta_id || p.ruta_id === String(ruta_id)
  );

  res.json(unirSinRepetir(deMongo, deDemo, "paradero_id"));
});

// ==========================================
// 5. CONSULTAR TIEMPOS DE LLEGADA (ETA)
// ==========================================

function distanciaMetros(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const rad = Math.PI / 180;
  const dLat = (lat2 - lat1) * rad;
  const dLon = (lon2 - lon1) * rad;
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

app.get("/api/eta/:paradero_id", async (req, res) => {
  const { paradero_id } = req.params;
  const id = String(paradero_id);

  const llegadas = await leerColeccion("eta", { paradero_id: id });

  const paradero =
    (await leerColeccion("paraderos", { paradero_id: id }))[0] ||
    demo.paraderos.find((p) => p.paradero_id === id);

  const enVivo = paradero && posicionesActivas.get(paradero.ruta_id);

  if (
    enVivo &&
    typeof paradero.latitud === "number" &&
    typeof paradero.longitud === "number"
  ) {
    // Distancia por calles ≈ 1.3 × línea recta; bus urbano ≈ 25 km/h
    const metros =
      distanciaMetros(
        enVivo.latitud,
        enVivo.longitud,
        paradero.latitud,
        paradero.longitud
      ) * 1.3;
    const minutos = Math.max(1, Math.round(metros / (25000 / 60)));

    llegadas.unshift({
      eta_id: "EN-VIVO",
      bus_id: `${enVivo.nombre} (en vivo)`,
      ruta_id: paradero.ruta_id,
      paradero_id: id,
      minutos,
    });
  }

  res.json(llegadas);
});

// ==========================================
// 6. REGISTRAR USUARIOS
// ==========================================

app.post("/api/usuarios", async (req, res) => {
  try {
    if (!mongoListo(res)) return;

    const usuario = req.body;

    if (
      !usuario ||
      typeof usuario !== "object" ||
      Array.isArray(usuario) ||
      Object.keys(usuario).length === 0
    ) {
      return res.status(400).json({
        error: "Debes proporcionar los datos del usuario",
      });
    }

    const resultado = await mongoose.connection.db
      .collection("usuarios")
      .insertOne(usuario);

    res.status(201).json({
      ...usuario,
      _id: resultado.insertedId.toString(),
    });
  } catch (error) {
    console.error("Error al registrar usuario:", error);

    res.status(500).json({
      error: "No se pudo registrar el usuario",
    });
  }
});

// ==========================================
// 7. WEBSOCKET - SEGUIMIENTO EN TIEMPO REAL
// ==========================================

// Guarda la última posición en vivo en la colección "ubicaciones"
// (como máximo una vez cada 10 s por ruta, para no saturar la BD)
const ultimoGuardado = new Map();

function guardarUbicacion(posicion) {
  if (mongoose.connection.readyState !== 1) return;
  const ahora = Date.now();
  if (ahora - (ultimoGuardado.get(posicion.ruta_id) || 0) < 10000) return;
  ultimoGuardado.set(posicion.ruta_id, ahora);

  const busId = `EN-VIVO-${posicion.ruta_id}`;
  mongoose.connection.db
    .collection("ubicaciones")
    .updateOne(
      { ubicacion_id: `UV-${posicion.ruta_id}` },
      {
        $set: {
          ubicacion_id: `UV-${posicion.ruta_id}`,
          bus_id: busId,
          ruta_id: posicion.ruta_id,
          conductor: posicion.nombre,
          latitud: posicion.latitud,
          longitud: posicion.longitud,
          hora: new Date(ahora).toLocaleTimeString("es-CO", { hour12: false }),
        },
      },
      { upsert: true }
    )
    .catch((e) => console.warn("No se guardó la ubicación:", e.message));
}

function detenerTransmision(socket) {
  const ruta = transmisores.get(socket.id);
  if (!ruta) return;

  transmisores.delete(socket.id);
  posicionesActivas.delete(ruta);

  io.to(`ruta:${ruta}`).emit("bus:fuera", { ruta_id: ruta });
  console.log(`Transmisión detenida en ${ruta}`);
}

io.on("connection", (socket) => {
  console.log("Cliente conectado:", socket.id);

  socket.on("ruta:seguir", (rutaId) => {
    if (typeof rutaId !== "string") return;

    const ruta = rutaId.trim();

    if (!/^R[0-9]{3,}$/.test(ruta)) return;

    socket.join(`ruta:${ruta}`);

    console.log(`Cliente ${socket.id} siguiendo ${ruta}`);

    const posicion = posicionesActivas.get(ruta);

    if (posicion) {
      socket.emit("bus:ubicacion", posicion);
    }
  });

  socket.on("conductor:ubicacion", (datos) => {
    if (!datos || typeof datos !== "object") return;

    const { ruta_id, latitud, longitud, nombre, precision } = datos;

    if (
      typeof ruta_id !== "string" ||
      !/^R[0-9]{3,}$/.test(ruta_id) ||
      typeof latitud !== "number" ||
      typeof longitud !== "number" ||
      !Number.isFinite(latitud) ||
      !Number.isFinite(longitud) ||
      latitud < -90 ||
      latitud > 90 ||
      longitud < -180 ||
      longitud > 180
    ) {
      socket.emit("ubicacion:error", {
        error: "Datos de ubicación inválidos",
      });
      return;
    }

    const posicion = {
      ruta_id,
      latitud,
      longitud,
      nombre:
        typeof nombre === "string" && nombre.trim()
          ? nombre.trim().slice(0, 40)
          : "Compañero",
      precision:
        typeof precision === "number" && Number.isFinite(precision)
          ? precision
          : null,
      timestamp: new Date().toISOString(),
      // Quién envía: la app lo usa para no mostrarse a sí misma como "bus"
      emisor: socket.id,
    };

    posicionesActivas.set(ruta_id, posicion);
    transmisores.set(socket.id, ruta_id);

    io.to(`ruta:${ruta_id}`).emit("bus:ubicacion", posicion);

    guardarUbicacion(posicion);

    console.log(`GPS ${ruta_id}: ${latitud}, ${longitud}`);
  });

  socket.on("ruta:dejar", (rutaId) => {
    if (typeof rutaId === "string") {
      socket.leave(`ruta:${rutaId.trim()}`);
    }
  });

  socket.on("conductor:detener", () => detenerTransmision(socket));

  socket.on("disconnect", () => {
    detenerTransmision(socket);
    console.log("Cliente desconectado:", socket.id);
  });
});

// ==========================================
// 8. INICIAR SERVIDOR Y CONECTAR MONGODB
// ==========================================

function ipsLocales() {
  return Object.values(os.networkInterfaces())
    .flat()
    .filter((i) => i && i.family === "IPv4" && !i.internal)
    .map((i) => i.address);
}

async function conectarMongo() {
  if (!MONGO_URI) {
    console.warn("Falta MONGO_URI en .env: se usan los datos de demostración.");
    return;
  }

  try {
    await conectarConReintentoDns(mongoose, MONGO_URI);
    console.log(
      `MongoDB Atlas conectado (base de datos "${mongoose.connection.db.databaseName}")`
    );
    // Un correo solo puede tener una cuenta
    await mongoose.connection.db
      .collection("usuarios")
      .createIndex({ email: 1 }, { unique: true, sparse: true })
      .catch((e) => console.warn("Índice de usuarios:", e.message));
  } catch (error) {
    console.error("No se pudo conectar a MongoDB:", error.message);
    console.error(explicarError(error));
    console.error("El seguimiento en vivo sigue funcionando. Reintento en 30 s...");
    setTimeout(conectarMongo, 30000);
  }
}

function iniciarServidor() {
  server.listen(PORT, "0.0.0.0", () => {
    console.log(`Servidor funcionando en http://localhost:${PORT}/api`);
    console.log(`Socket.IO disponible en el puerto ${PORT}`);
    console.log("");
    console.log("Panel para compartir ubicación:");
    console.log(`  En este PC:        http://localhost:${PORT}/`);
    ipsLocales().forEach((ip) =>
      console.log(`  Desde la red WiFi: http://${ip}:${PORT}/`)
    );
  });

  conectarMongo();
}

iniciarServidor();