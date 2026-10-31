/**
 * Ejecutor de una ruta preparada. La búsqueda BFS es una fase anterior.
 * La lista usa el último elemento como TOPE; 'Z' representa Z₀.
 * Cada paso de carga es una transición epsilon que apila un movimiento.
 * Cada paso de ejecución consume un símbolo de entrada y desapila el mismo.
 */
class MotorADP(
    private val ruta: String,
    val entrada: String,
    filaInicial: Int,
    columnaInicial: Int,
    private val filaMeta: Int,
    private val columnaMeta: Int,
    private val libre: (Int, Int) -> Boolean
) {
    enum class Fase { CARGANDO, EJECUTANDO, ACEPTADO, RECHAZADO }
    var fase = Fase.CARGANDO
        private set
    var fila = filaInicial
        private set
    var columna = columnaInicial
        private set
    var consumidos = 0
        private set
    var mensaje = "Pila inicial: Z₀. Preparando la carga."
        private set
    private val memoria = mutableListOf('Z')
    private var indiceCarga = ruta.length - 1
    val pila: List<Char> get() = memoria.toList()
    val pendiente: String get() = entrada.substring(consumidos)

    init {
        require(ruta.all { it in "RDLU" })
    }

    fun paso() {
        when (fase) {
            Fase.CARGANDO -> {
                if (indiceCarga >= 0) {
                    val simbolo = ruta[indiceCarga--]
                    memoria.add(simbolo)
                    mensaje = "APILAR $simbolo · ε (sin consumir entrada)"
                } else {
                    fase = Fase.EJECUTANDO
                    mensaje = "Pila cargada. Robot en la entrada; comienza la ejecución."
                }
            }
            Fase.EJECUTANDO -> {
                val tope = memoria.last()
                if (tope == 'Z') {
                    if (consumidos == entrada.length && fila == filaMeta && columna == columnaMeta) {
                        memoria.removeAt(memoria.lastIndex)
                        fase = Fase.ACEPTADO
                        mensaje = "DESAPILAR Z₀ · ε · Meta alcanzada, entrada consumida y pila vacía."
                    } else {
                        rechazar("Queda entrada pendiente o el robot no está en la meta. Z₀ se conserva.")
                    }
                    return
                }
                if (consumidos == entrada.length) {
                    rechazar("Se terminó la entrada antes de completar la ruta.")
                    return
                }
                val simbolo = entrada[consumidos]
                if (simbolo != tope) {
                    rechazar("Entrada $simbolo distinta del tope $tope; no hay transición válida.")
                    return
                }
                val df = when (simbolo) { 'D' -> 1; 'U' -> -1; else -> 0 }
                val dc = when (simbolo) { 'R' -> 1; 'L' -> -1; else -> 0 }
                if (!libre(fila + df, columna + dc)) {
                    rechazar("El movimiento $simbolo choca con una pared o sale del tablero.")
                    return
                }
                fila += df
                columna += dc
                consumidos++
                memoria.removeAt(memoria.lastIndex)
                mensaje = "LEER $simbolo · DESAPILAR $simbolo · Avanzar a q${fila * 15 + columna}"
            }
            Fase.ACEPTADO, Fase.RECHAZADO -> Unit
        }
    }

    private fun rechazar(motivo: String) {
        fase = Fase.RECHAZADO
        mensaje = "RECHAZADO: $motivo"
    }
}
