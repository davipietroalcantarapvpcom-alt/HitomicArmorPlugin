#!/usr/bin/env python3
"""Gera pack.mcmeta, modelos JSON, definicoes de item (assets/hitomic/items) e o asset de equipamento vestido."""
import json, os, sys
ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
A = os.path.join(ROOT, "assets/hitomic")

def w(path, obj):
    d = os.path.dirname(path)
    if d: os.makedirs(d, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write("\n")

# min_format/max_format: formato de resource pack do 26.3 e' 97; faixa larga evita o aviso de incompatibilidade
w(os.path.join(ROOT, "pack.mcmeta"), {"pack": {"description": "§5§lHitomic MC §r§7- Arsenal Sombrio de Cristal Violeta", "min_format": 84, "max_format": 120}})

m = lambda n: {"type": "minecraft:model", "model": f"hitomic:item/{n}"}
generated = ["capacete", "peitoral", "calcas", "botas", "asa"]
handheld = ["espada", "picareta", "machado", "pa", "enxada"]
for n in generated + handheld:
    parent = "minecraft:item/generated" if n in generated else "minecraft:item/handheld"
    w(f"{A}/models/item/{n}.json", {"parent": parent, "textures": {"layer0": f"hitomic:item/{n}"}})
    w(f"{A}/items/{n}.json", {"model": m(n)})

# ---- arco (mesmas transformacoes de exibicao do arco vanilla)
bow_display = {
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]}}
for n in ["arco", "arco_pulling_0", "arco_pulling_1", "arco_pulling_2"]:
    w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"hitomic:item/{n}"}, "display": bow_display})
w(f"{A}/items/arco.json", {"model": {
    "type": "minecraft:condition", "property": "minecraft:using_item",
    "on_false": m("arco"),
    "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
                "entries": [{"threshold": 0.65, "model": m("arco_pulling_1")}, {"threshold": 0.9, "model": m("arco_pulling_2")}],
                "fallback": m("arco_pulling_0")}}})

# ---- escudo: modelo 3D fino com textura propria (o escudo vanilla usa render especial com textura fixa)
def shield_model(blocking):
    el = [{"from": [0, 0, 7], "to": [16, 16, 9], "faces": {
        "north": {"uv": [0, 0, 16, 16], "texture": "#shield"},
        "south": {"uv": [0, 0, 16, 16], "texture": "#shield"},
        "east": {"uv": [0, 0, 1, 16], "texture": "#shield"},
        "west": {"uv": [15, 0, 16, 16], "texture": "#shield"},
        "up": {"uv": [0, 0, 16, 1], "texture": "#shield"},
        "down": {"uv": [0, 15, 16, 16], "texture": "#shield"}}}]
    common = {"gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
              "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
              "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}
    if not blocking:
        hand = {"thirdperson_righthand": {"rotation": [0, 90, -15], "translation": [3, 2, -4], "scale": [0.8, 0.8, 0.8]},
                "thirdperson_lefthand": {"rotation": [0, 90, -15], "translation": [3, 2, 4], "scale": [0.8, 0.8, 0.8]},
                "firstperson_righthand": {"rotation": [0, -50, 10], "translation": [-3, 1, -2], "scale": [0.8, 0.8, 0.8]},
                "firstperson_lefthand": {"rotation": [0, -50, 10], "translation": [-3, 1, -2], "scale": [0.8, 0.8, 0.8]}}
    else:
        hand = {"thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [-2, 2, -5], "scale": [0.9, 0.9, 0.9]},
                "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [-2, 2, 5], "scale": [0.9, 0.9, 0.9]},
                "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [-4, 3, -4], "scale": [1, 1, 1]},
                "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [4, 3, -4], "scale": [1, 1, 1]}}
    return {"textures": {"shield": "hitomic:item/escudo", "particle": "hitomic:item/escudo"}, "elements": el, "display": {**hand, **common}}
w(f"{A}/models/item/escudo.json", shield_model(False))
w(f"{A}/models/item/escudo_blocking.json", shield_model(True))
w(f"{A}/items/escudo.json", {"model": {"type": "minecraft:condition", "property": "minecraft:using_item", "on_false": m("escudo"), "on_true": m("escudo_blocking")}})

# ---- equipamento vestido (armadura Hitomic)
w(f"{A}/equipment/hitomic.json", {"layers": {"humanoid": [{"texture": "hitomic:hitomic"}], "humanoid_leggings": [{"texture": "hitomic:hitomic"}]}})
print("modelos e definicoes gerados")
