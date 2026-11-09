package com.BlandiArruti.E_commerce.admin.dto.response;

public record ResumenAdminResponse(
        Double ventasMes,
        long pendientesPago,
        long enPreparacion,
        long paraDespachar,
        long sinStock,
        long stockBajo,
        long ofertasActivas
) {}
