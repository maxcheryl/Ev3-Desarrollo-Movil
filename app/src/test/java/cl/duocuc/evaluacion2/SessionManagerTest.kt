package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion

class SessionManagerTest : StringSpec({
    "Iniciar y cerrar actualiza el estado de sesion" { SessionManagerTestCases().iniciarYcerrarActualizaElEstadoDeSesion() }
})

private class SessionManagerTestCases {

    fun iniciarYcerrarActualizaElEstadoDeSesion() {
        val manager = SessionManager()
        val usuario = UsuarioSesion(
            id = 4,
            nombre = "Ana Pérez",
            correo = "ana@example.com",
            perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
        )

        (manager.actual) shouldBe null
        manager.iniciar(usuario)
        (manager.actual) shouldBe (usuario)
        (manager.usuario.value) shouldBe (usuario)

        manager.cerrar()
        (manager.actual) shouldBe null
        (manager.usuario.value) shouldBe null
    }
}
