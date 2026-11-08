#!/usr/bin/env python3
"""
Gera as texturas do Resource Pack Hitomic a partir das artes de referencia.

Uso:  python3 gerar_texturas.py <pasta sprites_png do ZIP de referencia> <pasta raiz do resource pack>

- Itens (inventario/mao): recortados das artes de referencia, limpos (fundo/ruido removidos),
  reduzidos para SIZE x SIZE, quantizados em paleta curta e contornados (pixel art).
- Arco (+3 frames de puxar): desenhado proceduralmente.
- Armadura vestida (humanoid / humanoid_leggings 64x32): desenhada proceduralmente no UV do Minecraft.
"""
import sys, os, random, colorsys
from collections import deque
from PIL import Image, ImageDraw
import numpy as np

SIZE = 32
OUTLINE = (14, 7, 22, 255)

def clean(im):
    a = np.array(im.convert("RGBA")).astype(int)
    r, g, b, al = a[..., 0], a[..., 1], a[..., 2], a[..., 3]
    mx = np.maximum(np.maximum(r, g), b); mn = np.minimum(np.minimum(r, g), b)
    bg = (al < 128) | ((mn > 135) & ((mx - mn) < 45))          # transparente ou branco/cinza claro (fundo xadrez)
    a[bg] = 0
    h, w = al.shape
    alive = a[..., 3] > 0
    seen = np.zeros((h, w), bool); comps = []
    for y in range(h):
        for x in range(w):
            if alive[y, x] and not seen[y, x]:
                q = deque([(y, x)]); seen[y, x] = True; pts = []
                while q:
                    cy, cx = q.popleft(); pts.append((cy, cx))
                    for dy in (-1, 0, 1):
                        for dx in (-1, 0, 1):
                            ny, nx = cy + dy, cx + dx
                            if 0 <= ny < h and 0 <= nx < w and alive[ny, nx] and not seen[ny, nx]:
                                seen[ny, nx] = True; q.append((ny, nx))
                comps.append(pts)
    comps.sort(key=len, reverse=True)
    out = np.zeros_like(a)
    for pts in comps:
        if len(pts) >= max(12, 0.04 * len(comps[0])):
            for (y, x) in pts: out[y, x] = a[y, x]
    return Image.fromarray(out.astype(np.uint8), "RGBA")

def to_pixel_art(src, flip=False, stretch=False, size=SIZE, colors=16):
    im = clean(Image.open(src))
    im = im.crop(im.getbbox())
    if flip: im = im.transpose(Image.FLIP_LEFT_RIGHT)
    box = size - 2
    if stretch: nw, nh = box, box
    else:
        k = box / max(im.size); nw, nh = max(1, round(im.width * k)), max(1, round(im.height * k))
    # premultiplica alpha para nao sujar bordas
    arr = np.array(im).astype(float); al = arr[..., 3:4] / 255.0
    pm = Image.fromarray(np.dstack([arr[..., :3] * al, arr[..., 3:4]]).astype(np.uint8), "RGBA")
    sm = np.array(pm.resize((nw, nh), Image.BOX)).astype(float)
    a2 = sm[..., 3:4] / 255.0
    rgb = np.where(a2 > 0.01, sm[..., :3] / np.maximum(a2, 0.01), 0)
    mask = a2[..., 0] >= 0.5
    # paleta curta so com pixels opacos
    px = rgb[mask].astype(np.uint8).reshape(-1, 1, 3)
    pal = Image.fromarray(px).quantize(colors=colors, method=Image.MEDIANCUT)
    q = Image.fromarray(rgb.astype(np.uint8)).quantize(palette=pal, dither=Image.NONE).convert("RGB")
    qa = np.array(q)
    # realca saturacao/contraste levemente (cristais e vermelho continuam vivos em 32px)
    for y in range(nh):
        for x in range(nw):
            if mask[y, x]:
                r, g, b = qa[y, x] / 255.0
                h, s, v = colorsys.rgb_to_hsv(r, g, b)
                s = min(1, s * 1.15); v = min(1, max(0, (v - 0.5) * 1.12 + 0.5 + 0.02))
                qa[y, x] = [int(c * 255) for c in colorsys.hsv_to_rgb(h, s, v)]
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox, oy = (size - nw) // 2, (size - nh) // 2
    for y in range(nh):
        for x in range(nw):
            if mask[y, x]: canvas.putpixel((ox + x, oy + y), (*qa[y, x], 255))
    return outline(canvas)

def outline(im):
    out = im.copy(); w, h = im.size
    for y in range(h):
        for x in range(w):
            if im.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and im.getpixel((nx, ny))[3] > 0:
                        out.putpixel((x, y), OUTLINE); break
    return out

# ---------------------------------------------------------------- paleta
P = dict(
    o=(14, 7, 22), d0=(24, 15, 36), d1=(38, 24, 56), d2=(56, 34, 84), d3=(80, 48, 120),
    v1=(120, 58, 190), v2=(160, 84, 230), v3=(205, 150, 255),
    r1=(130, 14, 44), r2=(200, 24, 62), r3=(255, 70, 100),
    wood=(46, 28, 62), wood2=(70, 42, 92),
)
def C(k, a=255): return (*P[k], a)

# ---------------------------------------------------------------- arco procedural
def bezier(p0, p1, p2, n=80):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        pts.append((x, y))
    return pts

def line(img, a, b, col):
    x0, y0 = a; x1, y1 = b
    n = int(max(abs(x1 - x0), abs(y1 - y0))) * 2 + 1
    for i in range(n + 1):
        t = i / n; x = round(x0 + (x1 - x0) * t); y = round(y0 + (y1 - y0) * t)
        if 0 <= x < SIZE and 0 <= y < SIZE: img.putpixel((x, y), col)

def draw_bow(pull=-1):
    """pull=-1 repouso; 0,1,2 = estagios de puxar. Arco na diagonal '/', curva p/ cima-esquerda, corda embaixo-direita."""
    im = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    tipA, tipB = (27, 3), (3, 27)
    arc = bezier(tipA, (-3, -3), tipB, 120)
    # corpo do arco (2px de espessura, gradiente escuro -> violeta nas pontas)
    for i, (x, y) in enumerate(arc):
        t = i / (len(arc) - 1); edge = abs(t - 0.5) * 2
        for ox, oy in ((0, 0), (1, 0), (0, 1)):
            px, py = round(x) + ox, round(y) + oy
            if 0 <= px < SIZE and 0 <= py < SIZE:
                col = C("d1") if ox + oy == 0 else C("d2")
                if edge > 0.75: col = C("d3") if ox + oy else C("v1")
                im.putpixel((px, py), col)
    # veios vermelhos
    for i in range(10, 111, 12):
        x, y = arc[i]; im.putpixel((round(x), round(y)), C("r2"))
    # empunhadura + cristal
    gx, gy = round(arc[60][0]), round(arc[60][1])
    for dx in range(-1, 3):
        for dy in range(-1, 3):
            if 0 <= gx + dx < SIZE and 0 <= gy + dy < SIZE: im.putpixel((gx + dx, gy + dy), C("d0"))
    im.putpixel((gx, gy), C("v3")); im.putpixel((gx + 1, gy), C("v2")); im.putpixel((gx, gy + 1), C("v2")); im.putpixel((gx + 1, gy + 1), C("r3"))
    # pontas com cristal
    for t in (tipA, tipB): im.putpixel(t, C("v3"))
    # corda
    mid = ((tipA[0] + tipB[0]) / 2, (tipA[1] + tipB[1]) / 2)
    d = {-1: 0, 0: 2, 1: 5, 2: 8}[pull]
    nock = (mid[0] + d * 0.7071, mid[1] + d * 0.7071)
    sc = C("r3") if pull >= 0 else C("r2")
    line(im, tipA, nock, sc); line(im, nock, tipB, sc)
    if pull >= 0:   # flecha apontando para cima-esquerda, saindo do nock
        tail = (nock[0] + 1, nock[1] + 1); head = (nock[0] - 17, nock[1] - 17)
        line(im, tail, head, C("wood2"))
        hx, hy = round(head[0]), round(head[1])
        for dx, dy in ((0, 0), (-1, -1), (-1, 0), (0, -1), (1, -1), (-1, 1)):
            if 0 <= hx + dx < SIZE and 0 <= hy + dy < SIZE: im.putpixel((hx + dx, hy + dy), C("v3") if (dx, dy) == (-1, -1) else C("v2"))
        for k in (1, 2, 3):   # penas vermelhas
            x, y = round(tail[0] + k * 0.7), round(tail[1] + k * 0.7)
            for dx, dy in ((1, -1), (-1, 1)):
                if 0 <= x + dx < SIZE and 0 <= y + dy < SIZE: im.putpixel((x + dx, y + dy), C("r2"))
    return outline(im)

# ---------------------------------------------------------------- armadura vestida
def plate(img, box, rnd, tone=0):
    x0, y0, x1, y1 = box
    base = [C("d1"), C("d2"), C("d1"), C("d0"), C("d2")]
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = rnd.choice(base)
            if rnd.random() < 0.06: c = C("v1")
            if rnd.random() < 0.025: c = C("r1")
            if rnd.random() < 0.012: c = C("r2")
            if y == y0: c = C("d3")           # brilho superior
            if y == y1 - 1: c = C("d0")        # sombra inferior
            img.putpixel((x, y), c)

def px(img, x, y, k): img.putpixel((x, y), C(k))

def crystal(img, cx, cy, big=True):
    px(img, cx, cy, "v3")
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): px(img, cx + dx, cy + dy, "v2")
    if big:
        for dx, dy in ((0, -2), (0, 2), (-2, 0), (2, 0)): px(img, cx + dx, cy + dy, "v1")
    px(img, cx + 1, cy + 1, "r3") if big else None

def build_layer1():
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); rnd = random.Random(7)
    plate(im, (0, 8, 32, 16), rnd)      # cabeca: lados/frente/costas
    plate(im, (8, 0, 24, 8), rnd)       # topo + base
    plate(im, (16, 16, 40, 32), rnd)    # tronco
    plate(im, (40, 16, 56, 32), rnd)    # braco direito
    plate(im, (0, 27, 16, 32), rnd)     # botas (parte baixa das pernas)
    plate(im, (4, 16, 12, 20), rnd)     # topo/base da perna (botas)
    # --- capacete
    for y in range(9, 16): px(im, 11, y, "d0"); px(im, 12, y, "d0")           # fenda do visor
    for x in range(9, 15): px(im, x, 12, "d0")
    px(im, 10, 12, "r3"); px(im, 13, 12, "r3"); px(im, 11, 12, "r2"); px(im, 12, 12, "r2")   # olhos
    for y in range(13, 16): px(im, 11, y, "r1"); px(im, 12, y, "r1")
    crystal(im, 11, 9, False); px(im, 12, 9, "v2")
    for y in range(8, 11):                                                      # chamas violeta laterais
        for x in (1, 2, 5, 6, 17, 18, 21, 22): px(im, x, y, "v1" if (x + y) % 2 else "v2")
    for y in range(0, 8): px(im, 11, y, "r2"); px(im, 12, y, "r2")          # crista vermelha no topo
    crystal(im, 27, 12, False)                                                  # costas
    # --- peitoral
    crystal(im, 23, 24)
    for y in range(26, 32): px(im, 23, y, "r2" if y % 2 else "r1")
    for y in range(21, 24): px(im, 23, y, "r1")
    for i in range(0, 4): px(im, 21 + i, 22 + i // 2, "r1"); px(im, 26 - i, 22 + i // 2, "r1")
    for x in range(20, 28): px(im, x, 20, "v1")                                # gola violeta
    for y in range(20, 32, 3): px(im, 34, y, "v1"); px(im, 37, y + 1, "v2")    # costas
    for x in range(33, 39): px(im, x, 27, "r1")
    # ombros/bracos
    for y in range(20, 24):
        for x in range(44, 48): px(im, x, y, "d3" if y == 20 else "v1" if (x + y) % 3 == 0 else "d2")
    for y in range(24, 32, 3): px(im, 45, y, "r2"); px(im, 46, y, "v2")
    # --- botas
    for x in range(0, 16): px(im, x, 27, "r2" if x % 2 else "r1")
    for x in range(4, 8): px(im, x, 29, "v2")
    for x in range(0, 16): px(im, x, 31, "d0")
    # espelha braco para o braco esquerdo (mesma regiao do formato 64x32)
    return im

def build_layer2():
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); rnd = random.Random(21)
    plate(im, (0, 16, 16, 32), rnd)     # pernas inteiras
    plate(im, (16, 20, 40, 24), rnd)    # cinto (so as primeiras linhas do tronco)
    plate(im, (20, 16, 36, 20), rnd)
    for x in range(16, 40): px(im, x, 20, "d3"); px(im, x, 23, "r1" if x % 2 else "r2")
    crystal(im, 23, 21, False); px(im, 24, 21, "v2")                           # fivela
    for x in range(32, 36): px(im, x, 21, "v1")
    # joelheiras com cristais (frente da perna: x 4..8)
    for y in range(23, 26):
        for x in range(4, 8): px(im, x, y, "d3" if y == 23 else "d0")
    crystal(im, 5, 25, False); px(im, 6, 25, "v2")
    for y in range(27, 32, 2): px(im, 6, y, "r2")
    for y in range(20, 32): px(im, 0, y, "d0")
    return im

def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)

def main(refdir, root):
    tex = os.path.join(root, "assets/hitomic/textures")
    S = lambda n: os.path.join(refdir, n)
    items = {   # nome: (arquivo, flip, stretch)
        "capacete": ("capacete_hitomic.png", False, False),
        "peitoral": ("peitoral_hitomic.png", False, False),
        "calcas": ("calcas_hitomic.png", False, False),
        "botas": ("botas_hitomic.png", False, False),
        "espada": ("espada_hitomic.png", True, True),
        "picareta": ("picareta_hitomic_1.png", True, True),
        "machado": ("picareta_hitomic_2.png", True, True),
        "pa": ("machado_hitomic.png", True, True),     # cabeca arredondada da referencia vira a pa
        "enxada": ("enxada_hitomic.png", True, True),
        "escudo": ("escudo_hitomic.png", False, False),
        "asa": ("asa_hitomic.png", False, False),
    }
    for n, (f, flip, st) in items.items():
        save(to_pixel_art(S(f), flip, st), os.path.join(tex, "item", n + ".png"))
    save(draw_bow(-1), os.path.join(tex, "item", "arco.png"))
    for i in range(3): save(draw_bow(i), os.path.join(tex, "item", f"arco_pulling_{i}.png"))
    save(build_layer1(), os.path.join(tex, "entity/equipment/humanoid/hitomic.png"))
    save(build_layer2(), os.path.join(tex, "entity/equipment/humanoid_leggings/hitomic.png"))
    # icone do pack
    icon = Image.new("RGBA", (256, 256), (14, 7, 22, 255))
    w = Image.open(os.path.join(tex, "item", "asa.png")).resize((224, 224), Image.NEAREST)
    icon.alpha_composite(w, (16, 16)); icon.convert("RGB").save(os.path.join(root, "pack.png"))
    print("texturas geradas em", tex)

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
