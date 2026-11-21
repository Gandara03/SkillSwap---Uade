"""Evidencia del area 4: dependencias de Clean Architecture revisando los imports por paquete.

Uso:  python verificar_capas.py SkillSwap

Lee todos los .java de src/main y, para cada uno, toma las clases del proyecto que nombra (por import o con
nombre completo dentro del codigo) y las librerias externas que importa. Reglas (las de la seccion 6 de la consigna):
  - domain: no nombra ninguna otra capa ni jakarta.persistence, org.hibernate, java.sql, javax.swing;
  - application: solo domain (y application); ninguna libreria de persistencia ni de UI;
  - infrastructure: domain y application; nunca presentation ni bootstrap;
  - presentation: solo application.port.in, dto, command, exception y recomendacion (enum), domain.query,
    domain.exception (solo ManejadorErrores) y enums del dominio; nunca infrastructure, services ni port.out;
  - presentation.ui.swing: solo su contrato (ui.swing.contrato);
  - bootstrap: cualquiera (composition root).
"""
import io
import os
import re
import sys

repo = sys.argv[1]
raiz = os.path.join(repo, 'src', 'main', 'java')
BASE = 'com.uade.skillswap.'
PROHIBIDAS_NUCLEO = ('jakarta.persistence', 'org.hibernate', 'java.sql', 'javax.swing', 'java.awt', 'org.flywaydb',
                     'org.h2', 'com.mysql')

clases = {}  # nombre completo -> (capa, es_enum, ruta)
textos = {}
for carpeta, _, nombres in os.walk(raiz):
    for n in nombres:
        if not n.endswith('.java'):
            continue
        ruta = os.path.join(carpeta, n)
        texto = io.open(ruta, encoding='utf-8').read()
        sin = re.sub(r'/\*.*?\*/', ' ', texto, flags=re.S)
        sin = re.sub(r'//[^\n]*', ' ', sin)
        paquete = re.search(r'package ([\w.]+);', sin).group(1)
        fqn = paquete + '.' + n[:-5]
        es_enum = re.search(r'\benum ' + n[:-5] + r'\b', sin) is not None
        clases[fqn] = (paquete[len(BASE):].split('.')[0] if paquete.startswith(BASE) else '?', es_enum, ruta)
        textos[fqn] = sin


def subpaquete(fqn):
    return fqn[len(BASE):].rsplit('.', 1)[0]


violaciones = []
matriz = {}
externas = {}
for fqn, (capa, _, ruta) in sorted(clases.items()):
    sin = textos[fqn]
    propio = subpaquete(fqn)
    usados = set(re.findall(r'com\.uade\.skillswap\.[a-z_.]+\.[A-Z]\w*', sin))
    libs = set(m.rsplit('.', 1)[0] for m in re.findall(r'import (?:static )?((?:jakarta|javax|org|com\.mysql|java\.sql|java\.awt)[\w.]*)', sin))
    for lib in libs:
        externas.setdefault(capa, set()).add('.'.join(lib.split('.')[:2]))
        if capa in ('domain', 'application') and lib.startswith(PROHIBIDAS_NUCLEO):
            violaciones.append('%s importa %s' % (fqn[len(BASE):], lib))
        if capa == 'presentation' and lib.startswith(('jakarta.persistence', 'org.hibernate', 'java.sql')):
            violaciones.append('%s importa %s' % (fqn[len(BASE):], lib))
    for u in usados:
        if u not in clases or u == fqn:
            # puede ser un tipo anidado (Clase.Anidada): se busca la clase que lo contiene
            padre = u.rsplit('.', 1)[0]
            if padre in clases:
                u = padre
            else:
                continue
        capa_u, enum_u, _ = clases[u]
        sub_u = subpaquete(u)
        matriz.setdefault((capa, capa_u), 0)
        matriz[(capa, capa_u)] += 1
        corto, corto_u = fqn[len(BASE):], u[len(BASE):]
        if capa == 'domain' and capa_u != 'domain':
            violaciones.append('%s -> %s' % (corto, corto_u))
        elif capa == 'application' and capa_u not in ('domain', 'application'):
            violaciones.append('%s -> %s' % (corto, corto_u))
        elif capa == 'infrastructure' and capa_u in ('presentation', 'bootstrap'):
            violaciones.append('%s -> %s' % (corto, corto_u))
        elif capa == 'presentation':
            if propio.startswith('presentation.ui.swing') and not propio.endswith('contrato'):
                if not sub_u.startswith('presentation.ui.swing'):
                    violaciones.append('%s (pantalla) -> %s' % (corto, corto_u))
            elif propio.startswith('presentation.ui.swing.contrato'):
                if not sub_u.startswith('presentation.ui.swing.contrato'):
                    violaciones.append('%s (contrato) -> %s' % (corto, corto_u))
            elif capa_u in ('infrastructure', 'bootstrap'):
                violaciones.append('%s -> %s' % (corto, corto_u))
            elif capa_u == 'application':
                if not sub_u.startswith(('application.port.in', 'application.dto', 'application.command',
                                         'application.exception')) and not enum_u:
                    violaciones.append('%s -> %s' % (corto, corto_u))
            elif capa_u == 'domain':
                permitido = sub_u.startswith('domain.query') or enum_u \
                    or (sub_u.startswith('domain.exception') and corto.endswith('ManejadorErrores'))
                if not permitido:
                    violaciones.append('%s -> %s (clase de dominio que no es enum)' % (corto, corto_u))

capas = ['domain', 'application', 'infrastructure', 'presentation', 'bootstrap']
conteo = {c: sum(1 for v in clases.values() if v[0] == c) for c in capas}
print('[CAPAS] archivos por capa: %s (total %d)' % (conteo, len(clases)))
print('[CAPAS] referencias entre capas (fila usa columna):')
print('[CAPAS]   %-15s %s' % ('', ' '.join('%-14s' % c for c in capas)))
for a in capas:
    print('[CAPAS]   %-15s %s' % (a, ' '.join('%-14d' % matriz.get((a, b), 0) for b in capas)))
for c in capas:
    print('[CAPAS] librerias externas en %s: %s' % (c, sorted(externas.get(c, []))))
print('[CAPAS] VIOLACIONES: %d' % len(violaciones))
for v in violaciones:
    print('[CAPAS]   - ' + v)
