import java.util.ArrayDeque

/** Una casilla de la ruta; movimiento indica cómo llegar desde la anterior. */
data class PasoRuta(val fila: Int, val columna: Int, val movimiento: String)

/** Planificador BFS: cada desplazamiento ortogonal tiene coste uno.
 * La cola pertenece al planificador; la pila de aceptación está en MotorADP.
 */
object BuscadorRuta {
    fun buscar(tablero: Array<IntArray>, inicioFila: Int, inicioColumna: Int,
               metaFila: Int, metaColumna: Int): List<PasoRuta>? {
        require(tablero.isNotEmpty() && tablero[0].isNotEmpty())
        val filas = tablero.size
        val columnas = tablero[0].size
        require(tablero.all { it.size == columnas })
        fun valida(f: Int, c: Int) = f in 0 until filas && c in 0 until columnas && tablero[f][c] != 1
        if (!valida(inicioFila, inicioColumna) || !valida(metaFila, metaColumna)) return null
        val padres = IntArray(filas * columnas) { -1 }
        val movimientos = Array(filas * columnas) { "" }
        val inicio = inicioFila * columnas + inicioColumna
        val meta = metaFila * columnas + metaColumna
        val cola = ArrayDeque<Int>()
        padres[inicio] = inicio
        cola.addLast(inicio)
        val direcciones = arrayOf(Triple(0, 1, "R"), Triple(1, 0, "D"),
            Triple(0, -1, "L"), Triple(-1, 0, "U"))
        while (cola.isNotEmpty()) {
            val actual = cola.removeFirst()
            if (actual == meta) {
                val ruta = mutableListOf<PasoRuta>()
                var pos = meta
                while (pos != inicio) {
                    ruta.add(PasoRuta(pos / columnas, pos % columnas, movimientos[pos]))
                    pos = padres[pos]
                }
                ruta.add(PasoRuta(inicioFila, inicioColumna, ""))
                return ruta.asReversed()
            }
            for ((df, dc, simbolo) in direcciones) {
                val f = actual / columnas + df
                val c = actual % columnas + dc
                if (!valida(f, c)) continue
                val vecino = f * columnas + c
                if (padres[vecino] != -1) continue
                padres[vecino] = actual
                movimientos[vecino] = simbolo
                cola.addLast(vecino)
            }
        }
        return null
    }
}
