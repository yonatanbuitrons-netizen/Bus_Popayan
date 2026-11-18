// ==========================================
// AUTENTICACIÓN: REGISTRO E INICIO DE SESIÓN
// ==========================================
// Los usuarios se guardan en la colección "usuarios" de MongoDB Atlas.
// La contraseña nunca se guarda en texto plano: se guarda su hash
// (scrypt + sal aleatoria), igual que hacen los sistemas reales.

const crypto = require("crypto");
const express = require("express");

const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

function cifrarPassword(password, sal = crypto.randomBytes(16).toString("hex")) {
  const hash = crypto.scryptSync(password, sal, 64).toString("hex");
  return { sal, hash };
}

function passwordCorrecta(password, sal, hashGuardado) {
  const { hash } = cifrarPassword(password, sal);
  const a = Buffer.from(hash, "hex");
  const b = Buffer.from(hashGuardado || "", "hex");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

// Datos públicos del usuario (nunca se devuelve el hash)
function publico(u) {
  return {
    usuario_id: u.usuario_id,
    nombre: u.nombre,
    email: u.email,
    tipo: u.tipo,
  };
}

function crearRutasAuth(obtenerDb) {
  const router = express.Router();

  function db(res) {
    const d = obtenerDb();
    if (!d) {
      res.status(503).json({
        error: "La base de datos no está conectada. Intenta de nuevo en unos segundos.",
      });
    }
    return d;
  }

  // POST /api/auth/registro  { nombre, email, password }
  router.post("/registro", async (req, res) => {
    const nombre = String(req.body?.nombre ?? "").trim();
    const email = String(req.body?.email ?? "").trim().toLowerCase();
    const password = String(req.body?.password ?? "");

    if (nombre.length < 3) {
      return res.status(400).json({ error: "El nombre debe tener al menos 3 letras" });
    }
    if (!EMAIL_VALIDO.test(email)) {
      return res.status(400).json({ error: "El correo electrónico no es válido" });
    }
    if (password.length < 6) {
      return res.status(400).json({ error: "La contraseña debe tener al menos 6 caracteres" });
    }

    const d = db(res);
    if (!d) return;

    try {
      const usuarios = d.collection("usuarios");

      if (await usuarios.findOne({ email })) {
        return res.status(409).json({ error: "Ya existe una cuenta con ese correo" });
      }

      const { sal, hash } = cifrarPassword(password);
      const usuario = {
        usuario_id: "U" + Date.now(),
        nombre,
        email,
        tipo: "pasajero",
        password_hash: hash,
        password_sal: sal,
        fecha_registro: new Date().toISOString(),
      };

      await usuarios.insertOne(usuario);
      console.log(`Usuario registrado: ${email}`);
      res.status(201).json(publico(usuario));
    } catch (error) {
      if (error && error.code === 11000) {
        return res.status(409).json({ error: "Ya existe una cuenta con ese correo" });
      }
      console.error("Error en registro:", error.message);
      res.status(500).json({ error: "No se pudo crear la cuenta" });
    }
  });

  // POST /api/auth/login  { email, password }
  router.post("/login", async (req, res) => {
    const email = String(req.body?.email ?? "").trim().toLowerCase();
    const password = String(req.body?.password ?? "");

    if (!email || !password) {
      return res.status(400).json({ error: "Escribe tu correo y tu contraseña" });
    }

    const d = db(res);
    if (!d) return;

    try {
      const usuario = await d.collection("usuarios").findOne({ email });

      if (!usuario || !passwordCorrecta(password, usuario.password_sal, usuario.password_hash)) {
        return res.status(401).json({ error: "Correo o contraseña incorrectos" });
      }

      console.log(`Inicio de sesión: ${email}`);
      res.json(publico(usuario));
    } catch (error) {
      console.error("Error en login:", error.message);
      res.status(500).json({ error: "No se pudo iniciar sesión" });
    }
  });

  return router;
}

module.exports = { crearRutasAuth, cifrarPassword, passwordCorrecta };