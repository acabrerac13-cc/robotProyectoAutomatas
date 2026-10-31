import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Una instantánea del ADP para UNA ruta preparada. No representa el planificador BFS. */
data class EstadoRobot(val id: Int, val nombre: String, val etapa: String,
                       val inicial: Boolean = false, val final: Boolean = false)
data class TransicionRobot(val origen: Int, val destino: Int,
                           val leer: String, val sacar: String, val poner: String) {
    fun etiqueta(): String = "${leer.ifEmpty { "ε" }}, ${sacar.ifEmpty { "ε" }} ; ${poner.ifEmpty { "ε" }}"
}
data class ModeloAutomata(val estados: List<EstadoRobot>, val transiciones: List<TransicionRobot>,
                          val ruta: List<PasoRuta>, val cadena: String) {
    /** Disposición serpenteante: cuatro estados por fila, sin cruces. */
    fun posicion(indice: Int): Pair<Int, Int> {
        val fila = indice / 4
        val columna = if (fila % 2 == 0) indice % 4 else 3 - indice % 4
        return (115 + columna * 240) to (100 + fila * 145)
    }
}

object AutomataRobot {
    fun construir(ruta: List<PasoRuta>): ModeloAutomata {
        require(ruta.isNotEmpty()) { "Se necesita una ruta válida." }
        require(ruta.all { it.fila in 0..14 && it.columna in 0..14 })
        require(ruta.map { it.fila to it.columna }.distinct().size == ruta.size) { "La ruta debe ser simple." }
        for (i in 1 until ruta.size) {
            val previo = ruta[i-1]; val actual = ruta[i]
            val esperado = when (actual.fila-previo.fila to actual.columna-previo.columna) {
                (0 to 1) -> "R"; (1 to 0) -> "D"; (0 to -1) -> "L"; (-1 to 0) -> "U"
                else -> error("La ruta contiene un salto entre casillas.")
            }
            require(actual.movimiento == esperado) { "Movimiento incorrecto en la ruta." }
        }
        val cadena = ruta.drop(1).joinToString("") { it.movimiento }
        val estados = mutableListOf<EstadoRobot>()
        val transiciones = mutableListOf<TransicionRobot>()
        val n = cadena.length
        for (i in 0..n) estados.add(EstadoRobot(225+i, "c$i", "Carga", inicial = i==0))
        var tope = "Z"
        for (i in 0 until n) {
            val simbolo = cadena[n-1-i].toString()
            transiciones.add(TransicionRobot(225+i, 226+i, "", tope, simbolo+tope))
            tope = simbolo
        }
        for (p in ruta) {
            val id = p.fila*15+p.columna
            estados.add(EstadoRobot(id, "q$id", "Recorrido"))
        }
        val entrada = ruta.first().let {it.fila*15+it.columna}
        transiciones.add(TransicionRobot(225+n, entrada, "", tope, tope))
        for (i in 1 until ruta.size) {
            val a = ruta[i-1]; val b = ruta[i]
            transiciones.add(TransicionRobot(a.fila*15+a.columna, b.fila*15+b.columna,
                b.movimiento, b.movimiento, ""))
        }
        val idAcepta = 226+n
        estados.add(EstadoRobot(idAcepta, "ACEPTA", "Aceptación", final = true))
        val salida = ruta.last().let {it.fila*15+it.columna}
        transiciones.add(TransicionRobot(salida, idAcepta, "", "Z", ""))
        return ModeloAutomata(estados, transiciones, ruta.toList(), cadena)
    }

    fun exportar(modelo: ModeloAutomata, archivo: File) {
        val documento = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        fun nodo(padre: org.w3c.dom.Node, nombre: String, texto: String? = null): org.w3c.dom.Element {
            val e = documento.createElement(nombre)
            if (texto != null) e.textContent = texto
            padre.appendChild(e)
            return e
        }
        val estructura = nodo(documento, "structure")
        nodo(estructura, "type", "pda")
        val automata = nodo(estructura, "automaton")
        modelo.estados.forEachIndexed { indice, estado ->
            val e = nodo(automata, "state")
            e.setAttribute("id", estado.id.toString()); e.setAttribute("name", estado.nombre)
            val (x,y) = modelo.posicion(indice)
            nodo(e,"x",x.toDouble().toString()); nodo(e,"y",y.toDouble().toString())
            if (estado.inicial) nodo(e,"initial")
            if (estado.final) nodo(e,"final")
        }
        for (t in modelo.transiciones) {
            val e = nodo(automata,"transition")
            nodo(e,"from",t.origen.toString()); nodo(e,"to",t.destino.toString())
            nodo(e,"read",t.leer); nodo(e,"pop",t.sacar); nodo(e,"push",t.poner)
        }
        val transformador = TransformerFactory.newInstance().newTransformer()
        transformador.setOutputProperty(OutputKeys.INDENT,"yes")
        transformador.setOutputProperty(OutputKeys.ENCODING,"UTF-8")
        transformador.transform(DOMSource(documento),StreamResult(archivo))
    }
}
