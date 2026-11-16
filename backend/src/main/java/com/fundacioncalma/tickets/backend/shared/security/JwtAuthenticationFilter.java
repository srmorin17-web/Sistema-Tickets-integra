package com.fundacioncalma.tickets.backend.shared.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fundacioncalma.tickets.backend.usuario.controller.CuentaController;
import com.fundacioncalma.tickets.backend.usuario.dto.EstadoCuenta;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            username = jwtService.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // El rol y el estado de la cuenta se leen de la base de datos, no del token: una cuenta desactivada o con otro
                // rol surte efecto de inmediato; el JWT de acceso dura 15 minutos.
                Optional<EstadoCuenta> cuenta = usuarioRepository.findEstadoCuenta(username);
                if (cuenta.isEmpty() || !cuenta.get().activo() || !cuenta.get().activada()) {
                    rechazar(response, HttpServletResponse.SC_UNAUTHORIZED, "Cuenta inactiva o inexistente");
                    return;
                }
                // Con una contraseña temporal solo se permite cambiarla
                if (cuenta.get().debeCambiarContrasena() && !CuentaController.RUTA_CAMBIO_CONTRASENA.equals(request.getRequestURI())) {
                    rechazar(response, HttpServletResponse.SC_FORBIDDEN, "Debe cambiar su contraseña antes de continuar");
                    return;
                }

                String role = cuenta.get().rol();
                List<GrantedAuthority> autoridades = new ArrayList<>();
                autoridades.add(new SimpleGrantedAuthority("ROLE_" + role));
                // El Director tiene los mismos permisos que el Superadmin (decisión de la Dirección).
                // Para separarlos de nuevo basta con eliminar esta línea.
                if ("Director".equals(role)) {
                    autoridades.add(new SimpleGrantedAuthority("ROLE_Superadmin"));
                }
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, autoridades
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token inválido o expirado (o error al consultar la cuenta): se continúa sin autenticación y el SecurityConfig lo bloquea
        }
        filterChain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
