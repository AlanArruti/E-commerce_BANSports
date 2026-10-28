package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.enums.Rol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UsuarioDetails implements UserDetails, OAuth2User {
    private final Long id;
    private final String username;
    private final String password;
    private final Rol rol;
    private final boolean activo;
    private final Map<String, Object> attributes;

    private UsuarioDetails(Long id, String username, String password, Rol rol, boolean activo) {
        this(id, username, password, rol, activo, Map.of());
    }

    private UsuarioDetails(Long id, String username, String password, Rol rol, boolean activo, Map<String, Object> attributes) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
        this.attributes = attributes;
    }

    public static UsuarioDetails fromCliente(Cliente cliente) {
        return new UsuarioDetails(
                cliente.getId(),
                cliente.getEmail(),
                cliente.getContrasenia(),
                Rol.CLIENTE,
                cliente.isActivo()
        );
    }

    public static UsuarioDetails fromClienteOAuth2(Cliente cliente, Map<String, Object> attributes) {
        return new UsuarioDetails(
                cliente.getId(),
                cliente.getEmail(),
                cliente.getContrasenia(),
                Rol.CLIENTE,
                cliente.isActivo(),
                attributes
        );
    }

    public static UsuarioDetails fromAdmin(Administrador admin) {
        return new UsuarioDetails(
                admin.getId(),
                admin.getUsername(),
                admin.getContrasenia(),
                Rol.ADMIN,
                true
        );
    }

    //estos no son de UserDetails pero lo vamos necesitar en el JwtUtil para armar el token

    public Long getId() { return id; }
    public Rol getRol() { return rol; }

    // ── UserDetails ──────────────────────────────────────────────────────────
    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return activo; }

    // ── OAuth2User ───────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public String getName() { return username; }
}
