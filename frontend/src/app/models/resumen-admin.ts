// Respuesta de GET /admin/resumen (ResumenAdminResponse).
export interface ResumenAdmin {
  ventasMes: number;
  pendientesPago: number;
  enPreparacion: number;
  paraDespachar: number;
  sinStock: number;
  stockBajo: number;
  ofertasActivas: number;
}
