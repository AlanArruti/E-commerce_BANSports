package com.BlandiArruti.E_commerce.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final int BLOQUEO_MINUTOS = 15;

    private final Map<String, Integer> intentos = new ConcurrentHashMap<>();
    private final Map<String, Instant> bloqueadoHasta = new ConcurrentHashMap<>();

    public void loginExitoso(String email) {
        intentos.remove(email);
        bloqueadoHasta.remove(email);
    }

    public void loginFallido(String email) {
        int count = intentos.merge(email, 1, Integer::sum);
        if (count >= MAX_INTENTOS) {
            bloqueadoHasta.put(email, Instant.now().plus(BLOQUEO_MINUTOS, ChronoUnit.MINUTES));
        }
    }

    public boolean estaBloqueado(String email) {
        Instant hasta = bloqueadoHasta.get(email);
        if (hasta == null) return false;
        if (Instant.now().isAfter(hasta)) {
            intentos.remove(email);
            bloqueadoHasta.remove(email);
            return false;
        }
        return true;
    }

    public long minutosRestantes(String email) {
        Instant hasta = bloqueadoHasta.get(email);
        if (hasta == null) return 0;
        return Math.max(ChronoUnit.MINUTES.between(Instant.now(), hasta), 1);
    }
}
