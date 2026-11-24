package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionManagerTest {

    @Test
    fun iniciarYcerrarActualizaElEstadoDeSesion() {
        val manager = SessionManager()
        val usuario = UsuarioSesion(
            id = 4,
            nombre = "Ana Pérez",
            correo = "ana@example.com",
            perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
        )

        assertNull(manager.actual)
        manager.iniciar(usuario)
        assertEquals(usuario, manager.actual)
        assertEquals(usuario, manager.usuario.value)

        manager.cerrar()
        assertNull(manager.actual)
        assertNull(manager.usuario.value)
    }
}
