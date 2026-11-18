// Utilidades de conexión a MongoDB Atlas
const dns = require("dns");

// Algunas redes (y algunos Mac) no resuelven los registros SRV que usa
// "mongodb+srv://". Si pasa eso, se reintenta usando DNS públicos.
async function conectarConReintentoDns(mongoose, uri) {
  const opciones = { serverSelectionTimeoutMS: 10000 };
  try {
    await mongoose.connect(uri, opciones);
  } catch (error) {
    const esDns = /querySrv|ENOTFOUND|ECONNREFUSED.*_mongodb/i.test(
      error.message
    );
    if (!esDns) throw error;

    console.warn("Problema de DNS con Atlas; reintentando con DNS 8.8.8.8 / 1.1.1.1…");
    dns.setServers(["8.8.8.8", "1.1.1.1"]);
    await mongoose.connect(uri, opciones);
  }
}

// Mensaje en español según el error más común
function explicarError(error) {
  const m = String(error && error.message);

  if (/bad auth|authentication failed/i.test(m)) {
    return "→ Usuario o contraseña incorrectos. Revísalos en Atlas > Database Access " +
      "y en MONGO_URI del archivo .env (si la contraseña tiene @ : / ? # % cámbiala por una sin símbolos).";
  }
  if (/whitelist|IP.*not.*allowed|Could not connect to any servers|ReplicaSetNoPrimary|Server selection timed out/i.test(m)) {
    return "→ Atlas está bloqueando tu red. En Atlas > Network Access > Add IP Address " +
      "elige 'Allow access from anywhere' (0.0.0.0/0) y espera 1 minuto.";
  }
  if (/querySrv|ENOTFOUND/i.test(m)) {
    return "→ Tu red no resuelve la dirección de Atlas. Prueba con otra red (ej. datos del celular).";
  }
  return "→ Revisa MONGO_URI en el archivo .env.";
}

module.exports = { conectarConReintentoDns, explicarError };
