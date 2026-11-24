package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import cl.duocuc.evaluacion2.qr.QrMaxValidator

class QrMaxValidatorTest : StringSpec({
    "Contenido con prefijo y codigo es valido" { QrMaxValidatorTestCases().contenidoConPrefijoYCodigoEsValido() }
    "Contenido sin prefijo no es valido" { QrMaxValidatorTestCases().contenidoSinPrefijoNoEsValido() }
    "Contenido solo con prefijo no es valido" { QrMaxValidatorTestCases().contenidoSoloConPrefijoNoEsValido() }
    "Contenido vacio onulo no es valido" { QrMaxValidatorTestCases().contenidoVacioONuloNoEsValido() }
    "Prefijo en minusculas no es valido" { QrMaxValidatorTestCases().prefijoEnMinusculasNoEsValido() }
    "Extraer codigo devuelve solo el identificador" { QrMaxValidatorTestCases().extraerCodigoDevuelveSoloElIdentificador() }
})

private class QrMaxValidatorTestCases {

    fun contenidoConPrefijoYCodigoEsValido() {
        (QrMaxValidator.esValido("MAX:OP-001")) shouldBe true
    }

    fun contenidoSinPrefijoNoEsValido() {
        (QrMaxValidator.esValido("https://ejemplo.com/op-001")) shouldBe false
    }

    fun contenidoSoloConPrefijoNoEsValido() {
        (QrMaxValidator.esValido("MAX:")) shouldBe false
    }

    fun contenidoVacioONuloNoEsValido() {
        (QrMaxValidator.esValido("")) shouldBe false
        (QrMaxValidator.esValido(null)) shouldBe false
    }

    fun prefijoEnMinusculasNoEsValido() {
        (QrMaxValidator.esValido("max:OP-001")) shouldBe false
    }

    fun extraerCodigoDevuelveSoloElIdentificador() {
        (QrMaxValidator.extraerCodigo("MAX:OP-001")) shouldBe ("OP-001")
        (QrMaxValidator.extraerCodigo("MAX:FAIRA-2026")) shouldBe ("FAIRA-2026")
    }
}
