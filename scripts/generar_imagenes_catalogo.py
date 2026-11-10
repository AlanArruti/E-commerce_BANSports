"""
Genera fotos de prueba del catalogo (pote o frasco con etiqueta BAN Sports, color por sabor, tamano por peso).
Uso: uv run --with pillow python scripts/generar_imagenes_catalogo.py  -> deja los JPG en scripts/imagenes-generadas/
Los ids de VARIANTES/PRODUCTOS son los de la base local de Alan: ajustarlos antes de subir las fotos desde el panel.
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "imagenes-generadas")
os.makedirs(OUT, exist_ok=True)
F = "C:/Windows/Fonts/"
S = 2  # supersampling
W = H = 900 * S


def fuente(nombre, tam):
    return ImageFont.truetype(F + nombre, tam * S)


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mezclar(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def luminancia(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# ── Sabores: color de la etiqueta y del fondo ─────────────────
SABORES = {
    "Chocolate": "#6b4226", "Vainilla": "#e9d9a6", "Frutilla": "#e8506a", "Cookies": "#3d3d3d",
    "Frutos rojos": "#9b1b45", "Limón": "#f2cf3a", "Uva": "#6c3483", "Naranja": "#f08c1c", "Sandía": "#e8475a",
}

# ── Productos ──────────────────────────────────────────────────
# forma: pote | frasco. cuerpo = color del envase. acento = color de la etiqueta sin sabor.
PRODUCTOS = {
    2: dict(nombre="WHEY PROTEIN", sub="Concentrada", forma="pote", cuerpo="#1d2b4f", acento="#3e7bfa"),
    3: dict(nombre="WHEY ISOLATE", sub="Proteína aislada", forma="pote", cuerpo="#151515", acento="#c9a227"),
    4: dict(nombre="CASEÍNA", sub="Micelar", forma="pote", cuerpo="#3b2152", acento="#9b6dd6"),
    5: dict(nombre="CREATINA", sub="Monohidrato", forma="pote", cuerpo="#f4f4f4", acento="#1f8f6a"),
    6: dict(nombre="CREATINA", sub="Micronizada", forma="pote", cuerpo="#dfe4ea", acento="#2563eb"),
    7: dict(nombre="EXPLOSIVE", sub="Pre-entreno", forma="pote", cuerpo="#1a1a1a", acento="#e3262f"),
    8: dict(nombre="PRE-ENTRENO", sub="Sin cafeína", forma="pote", cuerpo="#0f4c5c", acento="#2ec4b6"),
    9: dict(nombre="BCAA 2:1:1", sub="Aminoácidos", forma="pote", cuerpo="#f7f7f7", acento="#ff7a00"),
    10: dict(nombre="GLUTAMINA", sub="L-Glutamina pura", forma="pote", cuerpo="#ffffff", acento="#0ea5e9"),
    11: dict(nombre="OMEGA 3", sub="Aceite de pescado", forma="frasco", cuerpo="#b8650f", acento="#0b6fb8"),
    12: dict(nombre="MULTIVITAMÍNICO", sub="Vitaminas y minerales", forma="frasco", cuerpo="#ffffff", acento="#22a559"),
    13: dict(nombre="MAGNESIO", sub="Citrato", forma="frasco", cuerpo="#ffffff", acento="#7c4dff"),
}

# variante -> (producto, sabor, peso/presentación)
VARIANTES = {
    2: (2, "Chocolate", "1 kg"), 3: (2, "Vainilla", "1 kg"), 4: (2, "Chocolate", "2 kg"),
    5: (3, "Frutilla", "900 g"), 6: (3, "Cookies", "900 g"),
    7: (4, "Chocolate", "1 kg"),
    8: (5, None, "300 g"), 9: (5, None, "500 g"),
    10: (6, None, "300 g"),
    11: (7, "Frutos rojos", None), 12: (7, "Limón", None),
    13: (8, "Uva", None),
    14: (9, "Naranja", None), 15: (9, "Sandía", None),
    16: (10, None, "300 g"),
    17: (11, None, "60 cápsulas"), 18: (11, None, "120 cápsulas"),
    19: (12, None, "60 cápsulas"),
    20: (13, None, "90 cápsulas"),
}

# Tamaño del envase según el peso (el de 2 kg se ve más grande que el de 1 kg).
ESCALA = {"300 g": 0.78, "500 g": 0.88, "900 g": 0.95, "1 kg": 0.97, "2 kg": 1.1,
          "60 cápsulas": 0.9, "90 cápsulas": 0.97, "120 cápsulas": 1.05}


def fondo(color):
    base = mezclar(hexrgb(color), (255, 255, 255), 0.82)
    img = Image.new("RGB", (W, H), base)
    d = ImageDraw.Draw(img)
    # Círculo suave del color del sabor detrás del envase.
    r = int(W * 0.36)
    halo = Image.new("L", (W, H), 0)
    ImageDraw.Draw(halo).ellipse((W // 2 - r, H // 2 - r - 40 * S, W // 2 + r, H // 2 + r - 40 * S), fill=255)
    halo = halo.filter(ImageFilter.GaussianBlur(60 * S))
    img.paste(mezclar(hexrgb(color), (255, 255, 255), 0.45), mask=halo)
    return img, d


def sombra(img, cx, piso, ancho):
    capa = Image.new("L", (W, H), 0)
    ImageDraw.Draw(capa).ellipse((cx - ancho // 2, piso - 22 * S, cx + ancho // 2, piso + 22 * S), fill=120)
    capa = capa.filter(ImageFilter.GaussianBlur(18 * S))
    img.paste((40, 40, 40), mask=capa)


def degradado_cilindro(ancho, alto, color, brillo=0.35):
    """Rectángulo con sombreado horizontal para que parezca cilíndrico."""
    c = hexrgb(color)
    tira = Image.new("RGB", (ancho, 1))
    for x in range(ancho):
        t = x / (ancho - 1)
        if t < 0.22:
            col = mezclar(c, (255, 255, 255), brillo * (t / 0.22))
        elif t < 0.32:
            col = mezclar(c, (255, 255, 255), brillo * (1 - (t - 0.22) / 0.1))
        else:
            col = mezclar(c, (0, 0, 0), 0.38 * ((t - 0.32) / 0.68) ** 1.5)
        tira.putpixel((x, 0), col)
    return tira.resize((ancho, alto))


def texto_centrado(d, cx, y, txt, fnt, color, ancho_max=None):
    if ancho_max:
        while d.textlength(txt, font=fnt) > ancho_max and fnt.size > 14 * S:
            fnt = ImageFont.truetype(fnt.path, fnt.size - 2 * S)
    w = d.textlength(txt, font=fnt)
    d.text((cx - w / 2, y), txt, font=fnt, fill=color)
    return fnt.size


def envase(img, p, sabor, peso):
    escala = ESCALA.get(peso, 1.0)
    frasco = p["forma"] == "frasco"
    ancho = int((300 if frasco else 400) * S * escala)
    alto = int((440 if frasco else 470) * S * escala)
    cx = W // 2
    piso = int(H * 0.86)
    top = piso - alto
    sombra(img, cx, piso, int(ancho * 1.25))

    cuerpo = degradado_cilindro(ancho, alto, p["cuerpo"])
    mascara = Image.new("L", (ancho, alto), 0)
    ImageDraw.Draw(mascara).rounded_rectangle((0, 0, ancho - 1, alto - 1), radius=int(ancho * 0.07), fill=255)
    img.paste(cuerpo, (cx - ancho // 2, top), mascara)

    # Tapa
    tapa_alto = int(alto * (0.17 if frasco else 0.13))
    tapa_ancho = int(ancho * (0.72 if frasco else 1.04))
    color_tapa = "#f2f2f2" if frasco else p["acento"] if luminancia(hexrgb(p["cuerpo"])) > 200 else "#202020"
    tapa = degradado_cilindro(tapa_ancho, tapa_alto, color_tapa, 0.25)
    mt = Image.new("L", (tapa_ancho, tapa_alto), 0)
    ImageDraw.Draw(mt).rounded_rectangle((0, 0, tapa_ancho - 1, tapa_alto - 1), radius=int(tapa_alto * 0.25), fill=255)
    img.paste(tapa, (cx - tapa_ancho // 2, top - tapa_alto + 6 * S), mt)
    d = ImageDraw.Draw(img)
    for i in range(1, 7):  # estrías de la tapa
        x = cx - tapa_ancho // 2 + i * tapa_ancho // 7
        d.line((x, top - tapa_alto + 14 * S, x, top - 2 * S), fill=mezclar(hexrgb(color_tapa), (0, 0, 0), 0.12), width=2 * S)

    # Etiqueta
    color_etq = hexrgb(SABORES[sabor]) if sabor else hexrgb(p["acento"])
    margen = int(alto * 0.14)
    e_top, e_bot = top + margen, piso - int(alto * 0.1)
    e_izq, e_der = cx - ancho // 2, cx + ancho // 2
    etq = degradado_cilindro(ancho, e_bot - e_top, "#ffffff", 0.0)
    img.paste(etq, (e_izq, e_top))
    d = ImageDraw.Draw(img)
    franja = int((e_bot - e_top) * 0.3)
    d.rectangle((e_izq, e_bot - franja, e_der, e_bot), fill=color_etq)
    d.rectangle((e_izq, e_top, e_der, e_top + 10 * S), fill=hexrgb(p["acento"]))

    util = ancho * 0.84
    y = e_top + 26 * S
    texto_centrado(d, cx, y, "BAN SPORTS", fuente("bahnschrift.ttf", 20), (120, 120, 120))
    y += 34 * S
    tam = texto_centrado(d, cx, y, p["nombre"], fuente("impact.ttf", 58), (25, 25, 25), util)
    y += tam + 10 * S
    texto_centrado(d, cx, y, p["sub"].upper(), fuente("arialbd.ttf", 20), hexrgb(p["acento"]), util)

    claro = luminancia(color_etq) > 165
    tinta = (30, 30, 30) if claro else (255, 255, 255)
    yb = e_bot - franja + 16 * S
    if sabor:
        texto_centrado(d, cx, yb, sabor.upper(), fuente("arialbd.ttf", 32), tinta, util)
        yb += 44 * S
    if peso:
        texto_centrado(d, cx, yb, peso, fuente("arial.ttf", 26 if sabor else 34), tinta, util)

    # Brillo vertical sobre todo el envase
    brillo = Image.new("L", (W, H), 0)
    bx = cx - ancho // 2 + int(ancho * 0.12)
    ImageDraw.Draw(brillo).rectangle((bx, top + 10 * S, bx + int(ancho * 0.06), piso - 10 * S), fill=70)
    brillo = brillo.filter(ImageFilter.GaussianBlur(8 * S))
    img.paste((255, 255, 255), mask=brillo)


def dibujar(nombre_archivo, p, sabor, peso):
    img, _ = fondo(SABORES[sabor] if sabor else p["acento"])
    envase(img, p, sabor, peso)
    img = img.resize((W // S, H // S), Image.LANCZOS)
    img.save(os.path.join(OUT, nombre_archivo), "JPEG", quality=88)


if __name__ == "__main__":
    for idv, (idp, sabor, peso) in VARIANTES.items():
        dibujar(f"v{idv}.jpg", PRODUCTOS[idp], sabor, peso)
    # La foto del producto es la de su primera variante (la que se ve en catálogo).
    primera = {}
    for idv, (idp, sabor, peso) in VARIANTES.items():
        primera.setdefault(idp, (sabor, peso))
    for idp, (sabor, peso) in primera.items():
        dibujar(f"p{idp}.jpg", PRODUCTOS[idp], sabor, peso)
    # Hoja de contacto para revisar todo de un vistazo.
    archivos = sorted(f for f in os.listdir(OUT) if f.endswith(".jpg") and f[0] in "pv")
    hoja = Image.new("RGB", (6 * 220, ((len(archivos) + 5) // 6) * 240), "white")
    dh = ImageDraw.Draw(hoja)
    for n, f in enumerate(archivos):
        im = Image.open(os.path.join(OUT, f)).resize((210, 210))
        x, y = (n % 6) * 220, (n // 6) * 240
        hoja.paste(im, (x + 5, y + 25))
        dh.text((x + 5, y + 5), f, fill="black")
    hoja.save(os.path.join(OUT, "_hoja.jpg"), quality=85)
    print(len(archivos), "imágenes")
