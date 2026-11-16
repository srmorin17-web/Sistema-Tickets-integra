package com.fundacioncalma.tickets.backend.rrhh.cliente;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Caché en memoria con vencimiento, para no consultar a RR. HH. en cada pantalla. Solo se guardan resultados positivos. */
public final class CacheTemporal<K, V> {

    private static final int MAXIMO_ENTRADAS = 5000;

    private record Entrada<V>(V valor, long venceEnMs) {}

    private final ConcurrentHashMap<K, Entrada<V>> mapa = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final LongSupplier reloj;

    public CacheTemporal(Duration ttl) {
        this(ttl, System::currentTimeMillis);
    }

    CacheTemporal(Duration ttl, LongSupplier reloj) {
        this.ttlMs = ttl.toMillis();
        this.reloj = reloj;
    }

    public Optional<V> obtener(K clave) {
        Entrada<V> entrada = mapa.get(clave);
        if (entrada == null) {
            return Optional.empty();
        }
        if (reloj.getAsLong() >= entrada.venceEnMs()) {
            mapa.remove(clave);
            return Optional.empty();
        }
        return Optional.of(entrada.valor());
    }

    public void guardar(K clave, V valor) {
        if (ttlMs <= 0) {
            return; // caché desactivada
        }
        if (mapa.size() >= MAXIMO_ENTRADAS) {
            mapa.clear();
        }
        mapa.put(clave, new Entrada<>(valor, reloj.getAsLong() + ttlMs));
    }
}
