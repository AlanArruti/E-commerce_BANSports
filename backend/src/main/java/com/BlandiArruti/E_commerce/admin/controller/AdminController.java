package com.BlandiArruti.E_commerce.admin.controller;

import com.BlandiArruti.E_commerce.admin.dto.response.MetricasAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.ResumenAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.VarianteStockBajoResponse;
import com.BlandiArruti.E_commerce.admin.service.AdminResumenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Panel de administración", description = "Resumen del panel y datos de stock (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminResumenService adminResumenService;

    @Operation(summary = "Resumen del panel", description = "Ventas del mes, pedidos por estado, stock y ofertas.")
    @GetMapping("/resumen")
    public ResponseEntity<ResumenAdminResponse> resumen() {
        return ResponseEntity.ok(adminResumenService.resumen());
    }

    @Operation(summary = "Métricas del panel",
            description = "Ventas por día, comparación con el período anterior, pedidos por estado y "
                    + "productos más vendidos. El período es de 7, 30 o 90 días.")
    @GetMapping("/metricas")
    public ResponseEntity<MetricasAdminResponse> metricas(@RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(adminResumenService.metricas(dias));
    }

    @Operation(summary = "Variantes con stock bajo o sin stock")
    @GetMapping("/variantes/stock-bajo")
    public ResponseEntity<List<VarianteStockBajoResponse>> variantesStockBajo() {
        return ResponseEntity.ok(adminResumenService.variantesStockBajo());
    }
}
