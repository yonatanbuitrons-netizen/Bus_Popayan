
require("dotenv").config();

const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");

const app = express();

// ==========================================
// 1. CONFIGURACIÓN DEL SERVIDOR
// ==========================================

app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI;

// ==========================================
// 2. VERIFICAR ESTADO DEL BACKEND
// ==========================================

app.get("/api", (req, res) => {
  res.json({
    proyecto: "Bus Popayán",
    estado: "Backend funcionando",
    database:
      mongoose.connection.readyState === 1
        ? "Conectada"
        : "Desconectada",
  });
});

// ==========================================
// 3. OBTENER TODAS LAS RUTAS
// ==========================================

app.get("/api/rutas", async (req, res) => {
  try {
    const { estado } = req.query;

    const filtro = estado
      ? { estado: String(estado) }
      : {};

    const rutas = await mongoose.connection.db
      .collection("rutas")
      .find(filtro)
      .toArray();

    res.json(rutas);
  } catch (error) {
    console.error("Error al obtener rutas:", error);

    res.status(500).json({
      error: "No se pudieron obtener las rutas",
    });
  }
});

// ==========================================
// 4. OBTENER PARADEROS POR RUTA
// ==========================================

app.get("/api/paraderos", async (req, res) => {
  try {
    const { ruta_id } = req.query;

    const filtro = ruta_id
      ? { ruta_id: String(ruta_id) }
      : {};

    const paraderos = await mongoose.connection.db
      .collection("paraderos")
      .find(filtro)
      .toArray();

    res.json(paraderos);
  } catch (error) {
    console.error("Error al obtener paraderos:", error);

    res.status(500).json({
      error: "No se pudieron obtener los paraderos",
    });
  }
});

// ==========================================
// 5. OBTENER TIEMPOS DE LLEGADA (ETA)
// ==========================================

// Ejemplo:
// GET /api/eta/P001

app.get("/api/eta/:paradero_id", async (req, res) => {
  try {
    const { paradero_id } = req.params;

    const llegadas = await mongoose.connection.db
      .collection("eta")
      .find({
        paradero_id: String(paradero_id),
      })
      .toArray();

    res.json(llegadas);
  } catch (error) {
    console.error("Error al consultar ETA:", error);

    res.status(500).json({
      error: "No se pudo consultar el tiempo de llegada",
    });
  }
});

// ==========================================
// 6. REGISTRAR USUARIOS
// ==========================================

app.post("/api/usuarios", async (req, res) => {
  try {
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
// 7. INICIAR SERVIDOR Y CONECTAR MONGODB
// ==========================================

async function iniciarServidor() {
  try {
    if (!MONGO_URI) {
      throw new Error("Falta MONGO_URI en .env");
    }

    await mongoose.connect(MONGO_URI);

    console.log("MongoDB conectado correctamente");

    app.listen(PORT, () => {
      console.log(
        `Servidor funcionando en http://localhost:${PORT}/api`
      );
    });
  } catch (error) {
    console.error("Error al iniciar:", error.message);
    process.exit(1);
  }
}

iniciarServidor();
