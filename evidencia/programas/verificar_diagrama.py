"""Evidencia del area 3 (diagrama): compara uml/00-aplicacion-completa.puml contra el codigo fuente.

Uso:  python verificar_diagrama.py SkillSwap

Verifica, con expresiones regulares sobre los .java (aproximado pero independiente del script del equipo):
  1. cada clase dibujada existe, en la capa de su package y con el mismo tipo (clase, abstracta, interfaz);
  2. cada atributo dibujado existe con ese tipo;
  3. cada operacion dibujada existe con los mismos nombres y tipos de parametros y el mismo retorno;
  4. al reves: los metodos publicos de interfaces, controllers, services y repositorios estan todos dibujados;
  5. realizacion = implements, herencia = extends, y al reves entre clases dibujadas;
  6. asociacion, composicion y agregacion: hay un campo de ese tipo o un campo ...Id (se listan las que van por id);
  7. dependencia: el tipo se usa en el codigo de la clase;
  8. toda asociacion, composicion y agregacion lleva cardinalidad en las dos puntas;
  9. que clases quedan sin ninguna flecha y que roles de cada patron no estan dibujados.
"""
import io
import os
import re
import sys

repo = sys.argv[1]
fuentes = os.path.join(repo, 'src', 'main', 'java')
puml = io.open(os.path.join(repo, 'uml', '00-aplicacion-completa.puml'), encoding='utf-8').read()

# ---------- indice del codigo ----------
archivos = {}
for carpeta, _, nombres in os.walk(fuentes):
    for n in nombres:
        if n.endswith('.java'):
            archivos.setdefault(n[:-5], []).append(os.path.join(carpeta, n))


def leer(ruta):
    texto = io.open(ruta, encoding='utf-8').read()
    texto = re.sub(r'/\*.*?\*/', ' ', texto, flags=re.S)
    texto = re.sub(r'//[^\n]*', ' ', texto)
    return re.sub(r'\s+', ' ', texto)


def norm(tipo):
    return re.sub(r'\s+', '', tipo).replace('final', '')


errores, avisos = [], []

# ---------- parseo del puml ----------
clases = {}  # nombre -> dict(tipo, capa, campos, metodos)
capa = None
actual = None
for linea in puml.splitlines():
    l = linea.strip()
    m = re.match(r'package "(\w+)"', l)
    if m:
        capa = m.group(1)
        continue
    m = re.match(r'(abstract class|class|interface) (\w+)', l)
    if m and capa:
        actual = {'tipo': m.group(1), 'capa': capa, 'campos': [], 'metodos': []}
        clases[m.group(2)] = actual
        if not l.endswith('{'):
            actual = None
        continue
    if l == '}':
        if actual is not None:
            actual = None
        else:
            capa = None
        continue
    if actual is not None and l:
        if l[0] not in '+-#~':
            errores.append('miembro sin visibilidad: ' + l)
            continue
        m = re.match(r'([+\-#~])\s*(\{static\}\s*)?(\w+)\((.*)\)\s*:\s*(.+)$', l)
        if m:
            params = []
            crudo = m.group(4).strip()
            if crudo:
                nivel, trozo = 0, ''
                for ch in crudo:
                    if ch == '<':
                        nivel += 1
                    if ch == '>':
                        nivel -= 1
                    if ch == ',' and nivel == 0:
                        params.append(trozo)
                        trozo = ''
                    else:
                        trozo += ch
                params.append(trozo)
            pares = []
            for p in params:
                if ':' not in p:
                    errores.append('parametro sin tipo: ' + l)
                    continue
                nombre, tipo = p.split(':', 1)
                pares.append((nombre.strip(), norm(tipo)))
            actual['metodos'].append((m.group(1), bool(m.group(2)), m.group(3), pares, norm(m.group(5))))
            continue
        m = re.match(r'([+\-#~])\s*(\{static\}\s*)?(\w+)\s*:\s*(.+)$', l)
        if m:
            actual['campos'].append((m.group(1), m.group(3), norm(m.group(4))))
        elif '(' in l:
            errores.append('operacion sin tipo de retorno: ' + l)

VIS = {'+': 'public', '-': 'private', '#': 'protected'}
MODIFICADORES = {'public', 'protected', 'private', 'static', 'final', 'abstract', 'default', 'synchronized', 'Override'}
fuente = {}
for nombre, c in clases.items():
    rutas = archivos.get(nombre, [])
    rutas = [r for r in rutas if os.sep + c['capa'].lower() + os.sep in r]
    if len(rutas) != 1:
        errores.append('%s: %d archivos en la capa %s' % (nombre, len(rutas), c['capa']))
        continue
    src = leer(rutas[0])
    fuente[nombre] = src
    decl = re.search(r'\b(public |final |abstract )*(class|interface|record|enum) ' + nombre + r'\b[^{]*', src)
    texto_decl = decl.group(0) if decl else ''
    real = 'interface' if ' interface ' in ' ' + texto_decl else ('abstract class' if 'abstract' in texto_decl else 'class')
    if real != c['tipo']:
        errores.append('%s: dibujada como %s y es %s' % (nombre, c['tipo'], real))
    c['decl'] = texto_decl
    for vis, campo, tipo in c['campos']:
        if not re.search(re.escape(tipo.split('<')[0]) + r'(<[^;=]*>)? ' + campo + r'\b ?[;=]', src):
            errores.append('%s: atributo %s: %s no esta en el codigo' % (nombre, campo, tipo))
    # metodos reales: (nombre, [(param, tipo)], retorno, es_publico)
    reales = []
    for mm in re.finditer(r'(public |protected |private |static |final |abstract |default |synchronized )*'
                          r'(<[^>]+> )?([\w.<>\[\], ?]+?) (\w+) ?\(([^)]*)\) ?(throws [\w., ]+)?[{;]', src):
        mods, _, ret, metodo, ps = mm.group(0).split('(')[0], mm.group(2), mm.group(3), mm.group(4), mm.group(5)
        if ret.strip() in ('return', 'new', 'throw', 'else') or metodo in ('if', 'for', 'while', 'switch', 'catch'):
            continue
        pares = []
        for p in [x for x in re.split(r',(?![^<]*>)', ps) if x.strip()]:
            trozos = p.strip().split(' ')
            pares.append((trozos[-1], norm(' '.join(trozos[:-1]))))
        publico = 'public ' in mods or 'public ' in ret + ' ' or c['tipo'] == 'interface'
        # el tipo de retorno capturado puede traer modificadores, la anotacion y el parametro generico del metodo
        limpio = [t for t in re.split(r' (?![^<]*>)', ret.strip()) if t not in MODIFICADORES and not t.startswith('<')]
        reales.append((metodo, pares, norm(' '.join(limpio)), publico))
    for vis, estatico, metodo, pares, ret in c['metodos']:
        candidatos = [r for r in reales if r[0] == metodo and len(r[1]) == len(pares)]
        if not candidatos:
            errores.append('%s: operacion %s/%d no esta en el codigo' % (nombre, metodo, len(pares)))
            continue
        if not any(r[1] == pares and r[2] == ret for r in candidatos):
            errores.append('%s.%s: firma dibujada %s -> %s; en el codigo %s' % (
                nombre, metodo, pares, ret, [(r[1], r[2]) for r in candidatos]))
    if c['tipo'] == 'interface' or nombre.endswith(('Controller', 'Service', 'Repository', 'Facade', 'Adapter')):
        dibujados = {(m[2], len(m[3])) for m in c['metodos']}
        for metodo, pares, ret, publico in reales:
            if publico and metodo != nombre and (metodo, len(pares)) not in dibujados:
                avisos.append('%s: el metodo publico %s/%d no esta dibujado' % (nombre, metodo, len(pares)))

# ---------- relaciones ----------
rel = []  # (origen, destino, tipo, card_origen, card_destino, linea)
for linea in puml.splitlines():
    l = linea.strip()
    m = re.match(r'(\w+)\s*("[^"]*")?\s*([<|.*o\-\[\]a-z>]+?)\s*("[^"]*")?\s*(\w+)(\s*:.*)?$', l)
    if not m or m.group(1) not in clases or m.group(5) not in clases or 'hidden' in l:
        continue
    a, ca, flecha, cb, b = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)
    flecha = re.sub(r'\[.*?\]|[a-z]', '', flecha)
    nucleo = flecha.replace('-', '').replace('.', '') if True else flecha
    punteada = '.' in flecha
    if '<|' in flecha:
        rel.append((b, a, 'realizacion' if punteada else 'herencia', cb, ca, l))
    elif '|>' in flecha:
        rel.append((a, b, 'realizacion' if punteada else 'herencia', ca, cb, l))
    elif '*' in flecha:
        rel.append((a, b, 'composicion', ca, cb, l))
    elif 'o' in m.group(3).replace('down', '').replace('right', '').replace('hidden', ''):
        rel.append((a, b, 'agregacion', ca, cb, l))
    elif punteada:
        rel.append((a, b, 'dependencia', ca, cb, l) if '>' in flecha else (b, a, 'dependencia', cb, ca, l))
    else:
        rel.append((a, b, 'asociacion', ca, cb, l) if flecha.endswith('>') else (b, a, 'asociacion', cb, ca, l))

por_id = []
for a, b, tipo, ca, cb, l in rel:
    if a not in fuente or b not in fuente:
        continue
    decl = clases[a].get('decl', '')
    if tipo == 'realizacion' and not re.search(r'implements [^{]*\b' + b + r'\b', decl):
        errores.append('realizacion sin implements: ' + l)
    if tipo == 'herencia' and not re.search(r'extends [^{]*\b' + b + r'\b', decl):
        errores.append('herencia sin extends: ' + l)
    if tipo in ('asociacion', 'composicion', 'agregacion'):
        if not ca or not cb:
            errores.append('falta cardinalidad: ' + l)
        if not re.search(r'\b' + b + r'\b[^;=()]* \w+ ?[;=]', fuente[a]):
            if re.search(r'Long \w*Id ?[;=]', fuente[a]):
                por_id.append('%s -> %s (%s)' % (a, b, tipo))
            else:
                errores.append('%s sin campo que la respalde: %s' % (tipo, l))
    if tipo == 'dependencia' and not re.search(r'\b' + b + r'\b', fuente[a]):
        errores.append('dependencia sin uso en el codigo: ' + l)

dibujadas = {(a, b) for a, b, t, _, _, _ in rel if t in ('realizacion', 'herencia')}
for nombre, c in clases.items():
    decl = c.get('decl', '')
    for otra in clases:
        if otra != nombre and re.search(r'(implements|extends) [^{]*\b' + otra + r'\b', decl) \
                and (nombre, otra) not in dibujadas:
            errores.append('%s implementa o extiende %s y no hay flecha' % (nombre, otra))

con_flecha = {x for a, b, *_ in rel for x in (a, b)}
sueltas = sorted(set(clases) - con_flecha)

# ---------- roles de los patrones ----------
ROLES = {
    'State': ['EstadoContratacion', 'EstadoContratacionBase', 'EstadoPendiente', 'EstadoAceptada', 'EstadoConfirmada',
              'EstadoEnCurso', 'EstadoCompletada', 'EstadoRechazada', 'EstadoPublicacion', 'EstadoPublicacionBase',
              'EstadoBorrador', 'EstadoPublicada', 'EstadoPausada', 'EstadoCerrada', 'Contratacion', 'Publicacion'],
    'Strategy': ['PoliticaCancelacion', 'PoliticaFlexible', 'PoliticaEstricta', 'EstrategiaRecomendacion',
                 'RecomendacionPorAfinidad', 'RecomendacionPorPopularidad', 'Contratacion', 'RecomendacionService'],
    'Decorator': ['PrecioPublicacion', 'PrecioBase', 'PromocionDecorator', 'PromocionPorcentaje', 'PromocionMontoFijo'],
    'Observer': ['ContratacionSubject', 'Contratacion', 'ContratacionObserver', 'NotificadorObserver',
                 'ContratacionEvent'],
    'Adapter': ['IdentidadPort', 'IdentidadMockAdapter', 'CanalNotificacionPort', 'EmailCanalAdapter',
                'PushCanalAdapter', 'TransaccionPort', 'JpaTransaccionAdapter', 'UiControllers', 'UiControllersAdapter'],
    'Simple Factory': ['CanalNotificacionFactory', 'CanalNotificacionFactoryImpl', 'PoliticaCancelacionFactory',
                       'EstrategiaRecomendacionFactory', 'PrecioPublicacionFactory', 'EstadoContratacionFactory',
                       'EstadoPublicacionFactory'],
    'Facade': ['PerfilPublicoFacade', 'PerfilPublicoUseCase', 'PerfilUseCase', 'CalificacionUseCase',
               'PublicacionUseCase', 'PerfilController'],
}

# ---------- informe ----------
tipos = {}
for _, _, t, _, _, _ in rel:
    tipos[t] = tipos.get(t, 0) + 1
porcapa = {}
for c in clases.values():
    porcapa[c['capa']] = porcapa.get(c['capa'], 0) + 1
print('[DIAGRAMA] clases dibujadas: %d %s' % (len(clases), porcapa))
print('[DIAGRAMA] atributos: %d, operaciones: %d' % (sum(len(c['campos']) for c in clases.values()),
                                                     sum(len(c['metodos']) for c in clases.values())))
print('[DIAGRAMA] relaciones: %d %s' % (len(rel), tipos))
print('[DIAGRAMA] notas o estereotipos: %d' % len(re.findall(r'^\s*note |<<', puml, flags=re.M)))
print('[DIAGRAMA] clases sin ninguna flecha: %s' % sueltas)
print('[DIAGRAMA] relaciones entre entidades respaldadas por un campo ...Id: %d' % len(por_id))
for patron, roles in ROLES.items():
    faltan = [r for r in roles if r not in clases]
    print('[DIAGRAMA] %-14s dibujados %d de %d; faltan: %s' % (patron, len(roles) - len(faltan), len(roles), faltan))
total = sum(len(v) for v in archivos.values())
print('[DIAGRAMA] clases del codigo: %d; dibujadas: %d' % (total, len(clases)))
print('[DIAGRAMA] ERRORES: %d' % len(errores))
for e in errores:
    print('[DIAGRAMA]   - ' + e)
print('[DIAGRAMA] AVISOS (metodos publicos sin dibujar): %d' % len(avisos))
for a in avisos[:40]:
    print('[DIAGRAMA]   - ' + a)
