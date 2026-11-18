// ==========================================
// DATOS DE DEMOSTRACIÓN
// ==========================================
// Se usan cuando MongoDB no está conectado, y se agregan a los de MongoDB
// cuando sí lo está (sin duplicar ids). Así la app siempre tiene rutas y
// paraderos para la sustentación.
//
// Para corregir una ubicación: en Google Maps mantén presionado el lugar,
// copia las coordenadas (ej. 2.44133, -76.60490) y pégalas en latitud/longitud.

const rutas = [
  {
    ruta_id: "R001",
    nombre: "Ruta Universidad Autónoma",
    numero: "1",
    origen: "Terminal de Transportes",
    destino: "Universidad Autónoma del Cauca",
    estado: "Activa",
  },
  {
    ruta_id: "R002",
    nombre: "Ruta Centro – Campanario",
    numero: "2",
    origen: "Parque Caldas",
    destino: "C.C. Campanario",
    estado: "Activa",
  },
  {
    ruta_id: "R003",
    nombre: "Ruta Sur",
    numero: "3",
    origen: "Barrio Alfonso López",
    destino: "Parque Caldas",
    estado: "Inactiva",
  },
];

const paraderos = [
  // ---- R001: Terminal -> Universidad Autónoma ----
  {
    paradero_id: "P101",
    nombre: "Terminal de Transportes",
    direccion: "Carrera 7 # 1N-37",
    ruta_id: "R001",
    estado: "Activo",
    latitud: 2.45405,
    longitud: -76.59951,
  },
  {
    paradero_id: "P102",
    nombre: "Puente del Humilladero",
    direccion: "Carrera 6 con Calle 2",
    ruta_id: "R001",
    estado: "Activo",
    latitud: 2.44518,
    longitud: -76.60507,
  },
  {
    paradero_id: "P103",
    nombre: "Parque Caldas",
    direccion: "Calle 4 con Carrera 6",
    ruta_id: "R001",
    estado: "Activo",
    latitud: 2.44193,
    longitud: -76.60631,
  },
  {
    paradero_id: "P104",
    nombre: "Universidad Autónoma del Cauca",
    direccion: "Calle 5 # 3-85",
    ruta_id: "R001",
    estado: "Activo",
    latitud: 2.44129,
    longitud: -76.60283,
  },

  // ---- R002: Centro -> Campanario ----
  {
    paradero_id: "P201",
    nombre: "Parque Caldas",
    direccion: "Calle 4 con Carrera 6",
    ruta_id: "R002",
    estado: "Activo",
    latitud: 2.44193,
    longitud: -76.60631,
  },
  {
    paradero_id: "P202",
    nombre: "C.C. Campanario",
    direccion: "Carrera 9 # 24AN-21",
    ruta_id: "R002",
    estado: "Activo",
    latitud: 2.46143,
    longitud: -76.59273,
  },

  // ---- R003: Sur ----
  {
    paradero_id: "P301",
    nombre: "Galería Alfonso López",
    direccion: "Calle 13 con Carrera 6",
    ruta_id: "R003",
    estado: "Activo",
    latitud: 2.43217,
    longitud: -76.60612,
  },
];

// ---- Buses que cubren cada ruta ----
const buses = [
  { bus_id: "B001", placa: "SMP-101", ruta_id: "R001", conductor: "Carlos Muñoz", estado: "En servicio" },
  { bus_id: "B002", placa: "SMP-102", ruta_id: "R001", conductor: "Luis Hurtado", estado: "En servicio" },
  { bus_id: "B003", placa: "SMP-203", ruta_id: "R002", conductor: "Andrea Ordóñez", estado: "En servicio" },
  { bus_id: "B004", placa: "SMP-304", ruta_id: "R003", conductor: "Jorge Valencia", estado: "Fuera de servicio" },
];

// ---- Tiempos estimados de llegada programados (minutos) ----
const eta = [
  { eta_id: "E001", bus_id: "B001", ruta_id: "R001", paradero_id: "P101", minutos: 3 },
  { eta_id: "E002", bus_id: "B001", ruta_id: "R001", paradero_id: "P102", minutos: 8 },
  { eta_id: "E003", bus_id: "B001", ruta_id: "R001", paradero_id: "P103", minutos: 12 },
  { eta_id: "E004", bus_id: "B001", ruta_id: "R001", paradero_id: "P104", minutos: 16 },
  { eta_id: "E005", bus_id: "B002", ruta_id: "R001", paradero_id: "P104", minutos: 31 },
  { eta_id: "E006", bus_id: "B003", ruta_id: "R002", paradero_id: "P201", minutos: 5 },
  { eta_id: "E007", bus_id: "B003", ruta_id: "R002", paradero_id: "P202", minutos: 18 },
];

// ---- Últimas ubicaciones registradas de cada bus ----
const ubicaciones = [
  { ubicacion_id: "UB001", bus_id: "B001", latitud: 2.45405, longitud: -76.59951, hora: "07:30" },
  { ubicacion_id: "UB002", bus_id: "B002", latitud: 2.44518, longitud: -76.60507, hora: "07:32" },
  { ubicacion_id: "UB003", bus_id: "B003", latitud: 2.44193, longitud: -76.60631, hora: "07:35" },
];

// ---- Usuario de prueba (la contraseña se guarda cifrada al ejecutar el seed) ----
const usuarios = [
  { usuario_id: "U001", nombre: "Usuario Demo", email: "demo@buspopayan.com", tipo: "pasajero", password: "123456" },
];

module.exports = { rutas, paraderos, buses, eta, ubicaciones, usuarios };