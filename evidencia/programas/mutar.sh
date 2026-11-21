#!/usr/bin/env bash
# Prueba de mutacion manual sobre el CLON temporal (nunca sobre el repo). Una mutacion por vez; despues se restaura.
S="$1"; SW="$2"; cd "$S/c" || exit 1
P=src/main/java/com/uade/skillswap
OUT="$S/mutaciones.txt"; touch "$OUT"
mutar() { # id | archivo | expresion sed | descripcion
  local id="$1" f="$P/$2" expr="$3" desc="$4"; grep -q "^$id|" "$OUT" && return
  git checkout -q -- . ; sed -i -E "$expr" "$f"
  if git diff --quiet; then echo "$id|NO APLICADA|$desc" >> "$OUT"; return; fi
  local n; n=$(git diff --numstat | awk '{print $1"+"$2"-"}')
  ./mvnw -o -q test "-Dmaven.repo.local=$SW/m2" -Djacoco.skip=true > "$S/mut-$id.log" 2>&1
  local rc=$?
  if grep -q 'COMPILATION ERROR' "$S/mut-$id.log"; then echo "$id|NO COMPILA|$desc" >> "$OUT"; return; fi
  local res; res=$(grep -E 'Tests run: [0-9]+, Failures' "$S/mut-$id.log" | tail -1 | sed -E 's/.*Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*/\2 fallas + \3 errores de \1/')
  local clases; clases=$(grep -E '^\[ERROR\] +[A-Z]\w+(Test|Base)[.>]' "$S/mut-$id.log" | sed -E 's/^\[ERROR\] +//; s/[.:>].*//' | sort -u | tr '\n' ' ')
  if [ $rc -eq 0 ]; then echo "$id|SOBREVIVE|$desc|diff $n" >> "$OUT"; else echo "$id|DETECTADA|$desc|$res|$clases" >> "$OUT"; fi
}
mutar M01 domain/model/politica/PoliticaPorUmbrales.java 's/horas >= umbrales\.horasReintegroTotal\(\)/horas > umbrales.horasReintegroTotal()/' 'borde del reintegro total: >= pasa a >'
mutar M02 domain/model/politica/PoliticaPorUmbrales.java 's/horas >= umbrales\.horasReintegroParcial\(\)/horas > umbrales.horasReintegroParcial()/' 'borde del reintegro parcial: >= pasa a >'
mutar M03 domain/model/vo/SkillCoins.java 's/, 2, RoundingMode\.HALF_UP\)\);/, 2, RoundingMode.DOWN));/' 'porcentaje redondea hacia abajo en vez de HALF_UP'
mutar M04 domain/model/wallet/Wallet.java 's/if \(saldo\.esMenorQue\(monto\)\)/if (false)/' 'la wallet deja de controlar el saldo al debitar'
mutar M05 domain/model/contratacion/Contratacion.java 's/SkillCoins reintegro = proveedorId\.equals\(canceladaPorId\)/SkillCoins reintegro = clienteId.equals(canceladaPorId)/' 'el 100 % se da cuando cancela el cliente y no el proveedor'
mutar M06 domain/model/contratacion/Contratacion.java 's/return new RepartoCancelacion\(reintegro, precioFinal\.restar\(reintegro\)\);/return new RepartoCancelacion(reintegro, SkillCoins.CERO);/' 'el proveedor no recibe compensacion al cancelar'
mutar M07 domain/model/publicacion/Publicacion.java 's/if \(cupoOcupado >= cupo\)/if (cupoOcupado > cupo)/' 'borde del cupo: admite una reserva de mas'
mutar M08 domain/model/publicacion/Publicacion.java 's/cupoOcupado = Math\.max\(0, cupoOcupado - 1\);/;/' 'liberar la franja no devuelve el cupo'
mutar M09 domain/model/publicacion/Publicacion.java 's/if \(inicio != null \&\& !inicio\.isAfter\(ahora\)\)/if (inicio != null \&\& inicio.isBefore(ahora))/' 'borde: se admite una franja que empieza exactamente ahora'
mutar M10 domain/model/calificacion/Calificacion.java 's/if \(!contratacion\.estaCompletada\(\)\)/if (false)/' 'se puede calificar una contratacion no completada'
mutar M11 application/service/CalificacionService.java 's/if \(calificacionRepository\.existe\(contratacion\.getId\(\), actorId\)\)/if (false)/' 'el servicio no controla la calificacion duplicada'
mutar M12 domain/model/precio/PromocionMontoFijo.java 's/\.restar\(monto\)\.noNegativo\(\)/.restar(monto)/' 'el descuento fijo puede dejar el precio negativo'
mutar M13 application/notificacion/NotificadorObserver.java 's/case CANCELACION -> List\.of\(e\.getClienteId\(\), e\.getProveedorId\(\)\);/case CANCELACION -> List.of(e.getClienteId());/' 'la cancelacion se avisa solo al cliente'
mutar M14 application/notificacion/NotificadorObserver.java 's/case SOLICITUD_RECIBIDA, PAGO_CONFIRMADO -> List\.of\(e\.getProveedorId\(\)\);/case SOLICITUD_RECIBIDA, PAGO_CONFIRMADO -> List.of(e.getClienteId());/' 'solicitud y pago se avisan al cliente en vez del proveedor'
mutar M15 domain/model/contratacion/Contratacion.java 's/^(\s*)exigirParte\(AccionContratacion\.ACEPTAR\.getParte\(\), actorId\);/\1;/' 'cualquiera puede aceptar una solicitud'
mutar M16 domain/model/contratacion/Contratacion.java 's/^(\s*)this\.historial\.add\(HistorialEstado\.registrar\(anterior, this\.estado, t\)\);/\1;/' 'las transiciones no dejan historial'
mutar M17 domain/model/contratacion/estado/EstadoPendiente.java 's/c\.cambiarEstado\(new EstadoAceptada\(\), t\);/c.cambiarEstado(new EstadoConfirmada(), t);/' 'aceptar salta directo a CONFIRMADA (sin pago)'
mutar M18 domain/model/perfil/Perfil.java 's/mapToInt\(c -> c\.getPuntaje\(\)\.getValor\(\)\)/mapToInt(c -> 5)/' 'la reputacion siempre da 5'
mutar M19 domain/model/publicacion/Publicacion.java 's/tipo == TipoPublicacion\.DESAFIO \&\& tipoAutor != TipoPerfil\.ORGANIZACION/tipo == TipoPublicacion.DESAFIO \&\& false/' 'un profesional puede publicar desafios'
mutar M20 domain/query/PaginaRequest.java 's/tamano > TAMANO_MAXIMO\)/tamano > TAMANO_MAXIMO + 1)/' 'borde del tamano de pagina: admite 101'
mutar M21 domain/model/promocion/Promocion.java 's/boolean hastaOk = vigenteHasta == null \|\| !ahora\.isAfter\(vigenteHasta\);/boolean hastaOk = true;/' 'las promociones no vencen'
mutar M22 domain/model/perfil/Perfil.java 's/^(\s*)if \(!activo\) \{/\1if (false) {/' 'un perfil dado de baja puede seguir operando'
mutar M23 domain/model/contratacion/Contratacion.java 's/^(\s*)exigirParte\(AccionContratacion\.PAGAR\.getParte\(\), actorId\);/\1;/' 'cualquiera puede pagar una contratacion ajena'
mutar M24 domain/model/promocion/Promocion.java 's/boolean desdeOk = vigenteDesde == null \|\| !ahora\.isBefore\(vigenteDesde\);/boolean desdeOk = vigenteDesde == null || ahora.isAfter(vigenteDesde);/' 'borde: la promocion no vale en el instante exacto en que empieza'
git checkout -q -- . ; git status --short | wc -l >> "$OUT"
