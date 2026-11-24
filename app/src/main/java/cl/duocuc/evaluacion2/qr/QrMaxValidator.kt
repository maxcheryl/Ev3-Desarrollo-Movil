package cl.duocuc.evaluacion2.qr

/**
 * Revisa si un QR pertenece a la aplicación.
 * Debe comenzar con «MAX:» y llevar un código después, por ejemplo «MAX:OP-001».
 */
object QrMaxValidator {

    const val PREFIJO = "MAX:"

    fun esValido(contenido: String?): Boolean {
        return contenido != null &&
            contenido.startsWith(PREFIJO) &&
            contenido.length > PREFIJO.length
    }

    /** Quita «MAX:» y devuelve el código, por ejemplo «OP-001». */
    fun extraerCodigo(contenido: String): String {
        return contenido.removePrefix(PREFIJO)
    }
}
