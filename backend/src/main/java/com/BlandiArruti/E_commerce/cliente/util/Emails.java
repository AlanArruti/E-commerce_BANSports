package com.BlandiArruti.E_commerce.cliente.util;

import java.util.Locale;
import java.util.Set;

/**
 * Reglas para comparar emails de clientes. Sin esto, registrarse como "Juan@Gmail.com" y después
 * entrar con Google ("juan@gmail.com") creaba dos cuentas distintas.
 */
public final class Emails {

    /** Gmail ignora los puntos y lo que va después de "+" en el usuario: todas son la misma casilla. */
    private static final Set<String> DOMINIOS_GMAIL = Set.of("gmail.com", "googlemail.com");

    private Emails() {
    }

    /** Sin espacios alrededor y en minúsculas: así se guarda y así se busca. */
    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Para una casilla de Gmail, el usuario sin puntos ni "+etiqueta" ("juan.perez+compras@gmail.com"
     * → "juanperez"). Para cualquier otro dominio, null: ahí los puntos sí cuentan.
     */
    public static String usuarioGmail(String email) {
        String normal = normalizar(email);
        if (normal == null) {
            return null;
        }
        int arroba = normal.lastIndexOf('@');
        if (arroba <= 0 || !DOMINIOS_GMAIL.contains(normal.substring(arroba + 1))) {
            return null;
        }
        String usuario = normal.substring(0, arroba);
        int mas = usuario.indexOf('+');
        if (mas >= 0) {
            usuario = usuario.substring(0, mas);
        }
        usuario = usuario.replace(".", "");
        return usuario.isEmpty() ? null : usuario;
    }
}
