# Guía de diseño — BAN Sports

Todas las variables están en `src/styles/variables.css`. Usalas siempre en lugar de escribir colores o medidas a mano:

```css
.mi-boton {
  background: var(--color-verde);
  color: var(--color-texto-sobre-verde);
  border-radius: var(--radio);
}
```

## Colores

| Variable | Valor | Para qué se usa |
|---|---|---|
| `--color-negro` | `#0a0d0b` | Fondo general de la app |
| `--color-negro-profundo` | `#060807` | Footer |
| `--color-superficie` | `#121714` | Tarjetas, inputs, paneles |
| `--color-superficie-2` | `#1a211d` | Fondo de imágenes e íconos |
| `--color-borde` | `#26302a` | Bordes y separadores |
| `--color-borde-input` | `#3a463f` | Borde de inputs y botones secundarios |
| `--color-verde` | `#2be37a` | Acento: botones principales, links, detalles |
| `--color-verde-hover` | `#6df0a4` | Hover de links verdes |
| `--color-texto-sobre-verde` | `#06110a` | Texto encima del verde (siempre negro, nunca blanco) |
| `--color-texto` | `#f2f5f3` | Texto principal |
| `--color-texto-suave` | `#c9d2cc` | Listas y textos de apoyo |
| `--color-texto-secundario` | `#9ba8a0` | Descripciones y placeholders |
| `--color-error` | `#ff7a6b` | Borde e ícono de error |
| `--color-error-texto` | `#ffb3a9` | Texto de los mensajes de error |
| `--color-error-fondo` | `#2a1512` | Fondo de las alertas de error |

## Tipografías

- `--fuente-titulos`: **Barlow Condensed** (800, en mayúsculas). Para títulos grandes usá la clase `.titulo-display`.
- `--fuente-texto`: **Inter**. Para todo el resto.

Se cargan desde Google Fonts en `src/index.html`.

## Medidas

- Radios: `--radio-chico` (6px), `--radio` (10px), `--radio-grande` (16px).
- Espaciados: `--espacio-1` (4px) a `--espacio-8` (64px).
- `--alto-input` (52px) y `--alto-boton` (56px). Todo lo que se toca mide al menos 44px.

## Clases globales (en `src/styles.css`)

- Botones: `.btn` + `.btn-primario` / `.btn-secundario` / `.btn-social`, `.btn-bloque` (ancho completo), `.btn-icono`.
- Formularios: `.campo`, `.input`, `.input-error`, `.input-ok`, `.input-con-boton` + `.btn-ojo`, `.check`.
- Mensajes: `.msj-error` (debajo del campo), `.alerta-error` y `.alerta-ok` (arriba del formulario).
- Otros: `.contenedor`, `.logo`, `.link`, `.separador`, `.solo-lectores`, `.acento`.

## Mensajes de validación

Están todos en `src/app/validaciones/mensajes.ts` (formularios) y `src/app/validaciones/errores-api.ts` (errores del back).
Regla: cada error tiene su propio mensaje, que dice qué está mal y cómo corregirlo. Nunca "Campo inválido" ni "Error".
