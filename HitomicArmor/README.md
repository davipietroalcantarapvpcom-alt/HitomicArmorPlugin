# HitomicArmor — Hitomic MC

Plugin + Resource Pack exclusivos do servidor **Hitomic MC** (Paper 26.3 · Java 25).

## ⚠️ Estado de verificação (leia primeiro)

| Item | Status |
|---|---|
| Resource pack: JSON válidos, referências modelo→textura sem quebras, texturas geradas e conferidas visualmente | ✅ verificado |
| Código Java: sintaxe e tipos compilam sem erros/avisos | ✅ verificado **contra stubs** da API (sem rede e sem o `paper-api` no ambiente onde foi feito) |
| Compilação real com Maven + `paper-api` 26.3 | ❌ **não executada** |
| Teste dentro do Minecraft/Paper 26.3 (texturas aparecendo, itens reconhecidos, efeitos) | ❌ **não executado** |

Por isso o `HitomicArmor.jar` entregue foi montado com bytecode Java 21 a partir dos stubs. **Recomendado:** recompile com Maven (abaixo) antes de ir para produção; se alguma assinatura da API divergir, o Maven aponta o erro na hora. Faça o teste da seção "Checklist de teste" num servidor de teste primeiro.

## Conteúdo

- `HitomicArmor.jar` — plugin
- `HitomicResourcePack.zip` — resource pack (SHA-1: `2a10d9ebcf43ebd2652bb5bbcc5f2a354b0ff10e`)
- `HitomicArmor-fonte.zip` — projeto Maven + fonte do resource pack + scripts que geram as texturas
- `config.yml`, `plugin.yml` — cópias dos arquivos embutidos no JAR

## Instalação do plugin

1. Servidor **Paper 26.3** com **Java 25**.
2. Coloque `HitomicArmor.jar` em `plugins/` e reinicie.
3. Edite `plugins/HitomicArmor/config.yml` se quiser e use `/hitomic recarregar`.

### Compilar (Maven)
```
cd HitomicArmor
mvn clean package        # JDK 25
# resultado: target/HitomicArmor.jar
```
O `pom.xml` usa `io.papermc.paper:paper-api:[26.3.build,)` do repositório `https://repo.papermc.io/repository/maven-public/`.

## Permissão (LuckPerms)

Todos os comandos exigem `hitomicarmor.admin` (padrão: operadores). Exemplo:
```
/lp group admin permission set hitomicarmor.admin true
```

## Comandos

| Comando | O que entrega |
|---|---|
| `/armadura hitomic [jogador]` | as 4 peças da armadura |
| `/hitomic espada [jogador]` | espada |
| `/hitomic ferramentas [jogador]` | picareta, machado, pá e enxada |
| `/hitomic arco [jogador]` | arco |
| `/hitomic asa [jogador]` | Asa Hitomic |
| `/hitomic escudo [jogador]` | escudo |
| `/hitomic kit [jogador]` | tudo |
| `/hitomic recarregar` | recarrega o config.yml |

> `/hitomic ferramentas` entrega as 4 ferramentas de trabalho; espada e arco têm comando próprio e entram no `kit`.

## Efeitos

| Situação | Efeitos |
|---|---|
| 4 peças da armadura | Força II, Velocidade II, Regeneração I |
| Asa Hitomic no offhand | Força I, Velocidade I |
| Armadura completa + Asa no offhand | **Força III, Velocidade III, Regeneração I** (o combo substitui os outros; nada se soma) |

- Verificação a cada 10 ticks + reavaliação imediata em eventos (trocar mão, clicar no inventário, equipar, morrer, respawn, trocar de mundo, entrar/sair).
- O plugin só remove efeitos que **ele** aplicou. Efeito externo mais forte do mesmo tipo é respeitado; um efeito externo mais fraco que seja sobrescrito é restaurado quando o bônus acaba.
- Níveis e duração são configuráveis em `effects.levels` no `config.yml`.
- Atenção de design: a Asa ocupa o offhand, então não dá para usar o Escudo Hitomic ao mesmo tempo que o bônus da Asa.

## Identificação dos itens

Cada item grava `hitomicarmor:item_id` no **PersistentDataContainer**. O nome nunca é usado para reconhecer itens (renomear na bigorna não falsifica nada) e o material base também precisa bater.

## Atributos

- Armadura, espada, picareta, machado, pá e enxada usam **bases de Netherite** (mesma proteção, tenacidade, resistência a repulsão, durabilidade, dano, velocidade, resistência ao fogo).
- Arco e escudo usam as bases vanilla (não existe versão Netherite). `items.<item>.durability` no config sobrescreve a durabilidade se desejado.
- Armadura vestida usa o equipamento `hitomic:hitomic` do resource pack.

## Resource pack — como disponibilizar aos jogadores

1. Hospede `HitomicResourcePack.zip` num link **de download direto** (GitHub Releases, seu próprio servidor web, CDN...).
2. No `server.properties`:
```
resource-pack=https://SEU-LINK/HitomicResourcePack.zip
resource-pack-sha1=2a10d9ebcf43ebd2652bb5bbcc5f2a354b0ff10e
require-resource-pack=true
resource-pack-prompt=Hitomic MC: pacote de texturas do arsenal.
```
3. Reinicie. Se trocar o ZIP, recalcule o SHA-1 (`sha1sum HitomicResourcePack.zip`) e atualize a propriedade, senão o cliente reusa o cache antigo.

### Formato do pack
`pack.mcmeta` usa `min_format: 84` e `max_format: 120` (26.3 usa o formato de resource pack 97, segundo uma fonte de terceiros que não consegui confirmar na documentação oficial). Se o cliente avisar incompatibilidade, ajuste esses dois números.

## Texturas — decisões

- Itens em **32×32** (pixel art recriada a partir das artes de referência: limpeza de fundo, redução, paleta curta e contorno). Para 16×16, mude `SIZE` em `tools/gerar_texturas.py`.
- Ferramentas/espada foram espelhadas (a referência aponta para o lado oposto ao padrão do Minecraft).
- **Pá:** a referência não tinha pá; usei a cabeça arredondada de maça. **Machado:** o machado de duas lâminas. **Asa:** a asa aberta única.
- **Arco** (e os 3 quadros de puxar) e as **camadas da armadura vestida** (64×32) foram desenhados proceduralmente seguindo o UV do Minecraft.
- **Escudo:** o escudo vanilla usa renderização especial com textura fixa, por isso o Escudo Hitomic é um modelo 3D fino com a textura própria. As transformações de mão (`display`) são estimativas — ajuste em `models/item/escudo*.json` se a posição na mão ficar estranha.
- Asa: item de inventário/mão; não há asas desenhadas nas costas do jogador.

## Checklist de teste (num servidor de teste)

1. `/hitomic kit`: cada item aparece com textura roxa (inventário, mão, chão). Se aparecer o item vanilla, o pack não carregou.
2. Vestir a armadura: textura Hitomic no corpo (não a Netherite). Efeitos Força II/Velocidade II/Regeneração I.
3. Asa no offhand sozinha: Força I/Velocidade I. Com armadura: Força III/Velocidade III/Regeneração I.
4. Tirar qualquer peça/asa: efeitos somem em ≤0,5 s. Tome uma poção de Velocidade antes e confirme que ela não é removida.
5. Renomear item vanilla (bigorna) com o nome de um item Hitomic: **não** deve dar efeito.
6. Morrer, relogar, trocar de dimensão: efeitos voltam sozinhos se as condições forem cumpridas.
7. Arco: puxar mostra os 3 estágios. Escudo: bloquear ergue o modelo.
