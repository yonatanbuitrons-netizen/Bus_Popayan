// ==========================================
// CARGAR TODAS LAS COLECCIONES EN MONGODB ATLAS
// ==========================================
// Uso (desde la carpeta backend):  npm run seed
// Se puede ejecutar varias veces: actualiza sin duplicar registros.

require("dotenv").config();
const mongoose = require("mongoose");
const demo = require("./datos-demo");
const { conectarConReintentoDns, explicarError } = require("./mongo-util");
const { cifrarPassword } = require("./auth");

async function main() {
  const uri = process.env.MONGO_URI;
  if (!uri) {
    console.error("Falta MONGO_URI en el archivo .env");
    process.exit(1);
  }

  try {
    await conectarConReintentoDns(mongoose, uri);
  } catch (error) {
    console.error("No se pudo conectar a MongoDB:", error.message);
    console.error(explicarError(error));
    process.exit(1);
  }

  const db = mongoose.connection.db;
  console.log(`Conectado a la base de datos "${db.databaseName}"`);

  async function guardar(coleccion, lista, campoId) {
    for (const doc of lista) {
      await db
        .collection(coleccion)
        .updateOne({ [campoId]: doc[campoId] }, { $set: doc }, { upsert: true });
    }
    console.log(`${coleccion}: ${lista.length} documentos`);
  }

  await guardar("rutas", demo.rutas, "ruta_id");
  await guardar("paraderos", demo.paraderos, "paradero_id");
  await guardar("buses", demo.buses, "bus_id");
  await guardar("eta", demo.eta, "eta_id");
  await guardar("ubicaciones", demo.ubicaciones, "ubicacion_id");

  // Usuarios: la contraseña nunca se guarda en texto plano
  const usuarios = demo.usuarios.map(({ password, ...u }) => {
    const { sal, hash } = cifrarPassword(password);
    return {
      ...u,
      password_hash: hash,
      password_sal: sal,
      fecha_registro: new Date().toISOString(),
    };
  });
  await guardar("usuarios", usuarios, "email");
  await db
    .collection("usuarios")
    .createIndex({ email: 1 }, { unique: true, sparse: true });

  console.log("");
  console.log("Usuario de prueba:  demo@buspopayan.com  /  123456");
  await mongoose.disconnect();
  console.log("Listo. Ya puedes ver las 6 colecciones en MongoDB Atlas.");
}

main();