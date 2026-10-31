import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.*
import javax.swing.*
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File

class RobotLaberinto : JFrame() {
    private val laberinto = Array<IntArray?>(FILAS) { IntArray(COLUMNAS) }
    private val botones = Array(FILAS) { fila ->
        Array(COLUMNAS) { columna -> CasillaRobot(fila, columna) }
    }
    private var robotFila = 0
    private var robotColumna = 0

    private inner class CasillaRobot(val fila: Int, val columna: Int) : JButton() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (fila != robotFila || columna != robotColumna) return

            val dibujo = g.create() as Graphics2D
            try {
                dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val escala = minOf(width / 40.0, height / 44.0)
                dibujo.translate((width - 32 * escala) / 2, (height - 38 * escala) / 2)
                dibujo.scale(escala, escala)
                dibujo.color = Color(30, 50, 80)
                dibujo.stroke = BasicStroke(2f)
                dibujo.drawLine(16, 3, 16, 8)
                dibujo.color = Color(255, 170, 40)
                dibujo.fillOval(13, 0, 6, 6)
                dibujo.color = Color(30, 50, 80)
                dibujo.fillRoundRect(2, 8, 28, 18, 7, 7)
                dibujo.color = Color(190, 230, 255)
                dibujo.fillRoundRect(5, 11, 22, 12, 5, 5)
                dibujo.color = Color(30, 50, 80)
                dibujo.fillOval(8, 14, 5, 5)
                dibujo.fillOval(19, 14, 5, 5)
                dibujo.fillRoundRect(8, 27, 16, 8, 3, 3)
                dibujo.drawLine(3, 28, 6, 32)
                dibujo.drawLine(26, 32, 29, 28)
                dibujo.fillRect(8, 35, 6, 3)
                dibujo.fillRect(18, 35, 6, 3)
                dibujo.color = Color(255, 170, 40)
                dibujo.fillOval(14, 29, 4, 4)
            } finally {
                dibujo.dispose()
            }
        }
    }

    // Entrada inicial
    private var entradaFila = 0
    private var entradaColumna = 0

    // Salida inicial
    private var salidaFila = 14
    private var salidaColumna = 14

    private var herramientaActual = Herramienta.PARED

    private var lblEstado: JLabel? = null
    private var lblHerramienta: JLabel? = null
    private var txtPila: JTextArea? = null
    private var txtRuta: JTextArea? = null

    private var btnBuscar: JButton? = null
    private var lblPila: JLabel? = null
    private var lblOperacion: JLabel? = null
    private var txtEntrada: JTextArea? = null
    private var motor: MotorADP? = null
    private var rutaPreparada = ""
    private var casillasRuta: List<PasoRuta> = emptyList()
    private val casillasPendientes = mutableSetOf<Pair<Int, Int>>()
    private var btnPausa: JButton? = null
    private var demora = 220

    // Para mostrar la animación
    private var timerAnimacion: Timer? = null

    internal enum class Herramienta {
        PARED,
        BORRAR,
        ENTRADA,
        SALIDA
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    init {
        setTitle("Robot en Laberinto - ADP con aceptación por pila vacía")
        setDefaultCloseOperation(EXIT_ON_CLOSE)
        setSize(1350, 900)
        setLocationRelativeTo(null)

        crearInterfaz()
        inicializarLaberinto()
        actualizarTablero()

        setVisible(true)
    }

    // =========================================================
    // CREAR INTERFAZ
    // =========================================================
    private fun crearInterfaz() {
        val principal = JPanel(BorderLayout(10, 10))
        principal.setBorder(EmptyBorder(10, 10, 10, 10))

        // -----------------------------------------------------
        // TÍTULO
        // -----------------------------------------------------
        val titulo = JLabel(
            "ROBOT 15 × 15 · RUTA MÁS CORTA → CARGAR PILA → RECORRER",
            SwingConstants.CENTER
        )

        titulo.setFont(Font("Arial", Font.BOLD, 24))
        principal.add(titulo, BorderLayout.NORTH)

        // -----------------------------------------------------
        // TABLERO
        // -----------------------------------------------------
        val panelTablero = JPanel(
            GridLayout(FILAS, COLUMNAS, 1, 1)
        )

        panelTablero.setBackground(Color.DARK_GRAY)

        for (fila in 0..<FILAS) {
            for (columna in 0..<COLUMNAS) {
                val boton = botones[fila][columna]

                boton.setMargin(Insets(0, 0, 0, 0))
                boton.setFont(Font("Arial", Font.BOLD, 10))
                boton.setFocusPainted(false)

                val f = fila
                val c = columna

                boton.addActionListener(ActionListener { _: ActionEvent? -> modificarCasilla(f, c) })

                panelTablero.add(boton)
            }
        }

        principal.add(panelTablero, BorderLayout.CENTER)

        // -----------------------------------------------------
        // PANEL DERECHO
        // -----------------------------------------------------
        val panelDerecho = object : JPanel() {
            override fun getPreferredSize(): Dimension = Dimension(380, super.getPreferredSize().height)
        }
        panelDerecho.setLayout(
            BoxLayout(panelDerecho, BoxLayout.Y_AXIS)
        )

        lblEstado = JLabel("Listo para comenzar")
        lblEstado!!.setFont(Font("Arial", Font.BOLD, 16))

        lblHerramienta = JLabel("Herramienta: PARED")

        panelDerecho.add(lblEstado)
        panelDerecho.add(Box.createVerticalStrut(5))
        panelDerecho.add(lblHerramienta)
        panelDerecho.add(Box.createVerticalStrut(15))

        // -----------------------------------------------------
        // BOTONES DE EDICIÓN
        // -----------------------------------------------------
        val btnPared = JButton("Agregar pared")
        val btnBorrar = JButton("Borrar")
        val btnEntrada = JButton("Colocar entrada")
        val btnSalida = JButton("Colocar salida")

        configurarBoton(btnPared)
        configurarBoton(btnBorrar)
        configurarBoton(btnEntrada)
        configurarBoton(btnSalida)

        btnPared.addActionListener(ActionListener { _: ActionEvent? ->
            herramientaActual = Herramienta.PARED
            lblHerramienta!!.setText("Herramienta: PARED")
        })

        btnBorrar.addActionListener(ActionListener { _: ActionEvent? ->
            herramientaActual = Herramienta.BORRAR
            lblHerramienta!!.setText("Herramienta: BORRAR")
        })

        btnEntrada.addActionListener(ActionListener { _: ActionEvent? ->
            herramientaActual = Herramienta.ENTRADA
            lblHerramienta!!.setText("Herramienta: ENTRADA")
        })

        btnSalida.addActionListener(ActionListener { _: ActionEvent? ->
            herramientaActual = Herramienta.SALIDA
            lblHerramienta!!.setText("Herramienta: SALIDA")
        })

        panelDerecho.add(btnPared)
        panelDerecho.add(Box.createVerticalStrut(5))

        panelDerecho.add(btnBorrar)
        panelDerecho.add(Box.createVerticalStrut(5))

        panelDerecho.add(btnEntrada)
        panelDerecho.add(Box.createVerticalStrut(5))

        panelDerecho.add(btnSalida)

        panelDerecho.add(Box.createVerticalStrut(20))

        // -----------------------------------------------------
        // BUSCAR SALIDA
        // -----------------------------------------------------
        btnBuscar = JButton("BUSCAR Y EJECUTAR")

        btnBuscar!!.setFont(
            Font("Arial", Font.BOLD, 16)
        )

        configurarBoton(btnBuscar!!)

        btnBuscar!!.addActionListener(ActionListener { _: ActionEvent? -> buscarSalida() })

        panelDerecho.add(btnBuscar)

        panelDerecho.add(Box.createVerticalStrut(5))

        val btnLimpiarRuta = JButton("Limpiar recorrido")
        configurarBoton(btnLimpiarRuta)

        btnLimpiarRuta.addActionListener(ActionListener { _: ActionEvent? ->
            detenerAnimacion()
            limpiarRecorrido()
        })

        panelDerecho.add(btnLimpiarRuta)

        panelDerecho.add(Box.createVerticalStrut(5))

        val btnLimpiarTodo = JButton("Nuevo laberinto")
        configurarBoton(btnLimpiarTodo)

        btnLimpiarTodo.addActionListener(ActionListener { _: ActionEvent? -> nuevoLaberinto() })

        panelDerecho.add(btnLimpiarTodo)
        panelDerecho.add(Box.createVerticalStrut(8))
        val btnVerAutomata = JButton("Ver autómata")
        val btnExportarAutomata = JButton("Exportar a JFLAP")
        configurarBoton(btnVerAutomata)
        configurarBoton(btnExportarAutomata)
        btnVerAutomata.addActionListener {
            val modelo = modeloActualParaDiagrama()
            if (modelo != null) VistaAutomata(this, modelo, ::guardarAutomata).isVisible = true
        }
        btnExportarAutomata.addActionListener {
            val modelo = modeloActualParaDiagrama()
            if (modelo != null) guardarAutomata(this, modelo)
        }
        panelDerecho.add(btnVerAutomata)
        panelDerecho.add(Box.createVerticalStrut(5))
        panelDerecho.add(btnExportarAutomata)

        // -----------------------------------------------------
        // PILA
        // -----------------------------------------------------
        panelDerecho.add(Box.createVerticalStrut(20))

        btnPausa = JButton("Pausar")
        configurarBoton(btnPausa!!)
        btnPausa!!.isEnabled = false
        btnPausa!!.addActionListener {
            val reloj = timerAnimacion
            if (reloj != null) {
                if (reloj.isRunning) {
                    reloj.stop()
                    btnPausa!!.text = "Continuar"
                } else {
                    reloj.start()
                    btnPausa!!.text = "Pausar"
                }
            }
        }
        panelDerecho.add(btnPausa)
        panelDerecho.add(JLabel("Velocidad: lento ← → rápido"))
        val velocidad = JSlider(1, 10, 6)
        velocidad.maximumSize = Dimension(Int.MAX_VALUE, 35)
        velocidad.addChangeListener {
            demora = 520 - velocidad.value * 50
            timerAnimacion?.delay = demora
        }
        panelDerecho.add(velocidad)
        panelDerecho.add(JLabel("<html>ENTRADA PENDIENTE<br>R: derecha · D: abajo<br>L: izquierda · U: arriba</html>"))
        txtEntrada = JTextArea(3, 22)
        txtEntrada!!.isEditable = false
        txtEntrada!!.lineWrap = true
        txtEntrada!!.font = Font("Monospaced", Font.PLAIN, 14)
        val scrollEntrada = JScrollPane(txtEntrada)
        scrollEntrada.maximumSize = Dimension(Int.MAX_VALUE, 75)
        panelDerecho.add(scrollEntrada)
        lblOperacion = JLabel(" ")
        lblOperacion!!.alignmentX = Component.LEFT_ALIGNMENT
        panelDerecho.add(lblOperacion)
        panelDerecho.add(JLabel("Amarillo: ruta prevista · Azul: recorrido"))
        panelDerecho.add(Box.createVerticalStrut(10))
        lblPila = JLabel("PILA · TOPE ARRIBA")
        lblPila!!.setFont(Font("Arial", Font.BOLD, 14))
        panelDerecho.add(lblPila)

        txtPila = JTextArea()
        txtPila!!.setEditable(false)
        txtPila!!.setFont(Font("Monospaced", Font.PLAIN, 13))

        val scrollPila = JScrollPane(txtPila)
        scrollPila.setPreferredSize(Dimension(280, 150))
        scrollPila.setMaximumSize(
            Dimension(Int.MAX_VALUE, 150)
        )

        panelDerecho.add(scrollPila)

        // -----------------------------------------------------
        // RUTA
        // -----------------------------------------------------
        panelDerecho.add(Box.createVerticalStrut(10))

        val lblRuta = JLabel("MOVIMIENTOS")
        lblRuta.setFont(Font("Arial", Font.BOLD, 14))

        panelDerecho.add(lblRuta)

        txtRuta = JTextArea()
        txtRuta!!.setEditable(false)
        txtRuta!!.setLineWrap(true)
        txtRuta!!.setWrapStyleWord(true)

        val scrollRuta = JScrollPane(txtRuta)
        scrollRuta.setPreferredSize(Dimension(280, 130))
        scrollRuta.setMaximumSize(
            Dimension(Int.MAX_VALUE, 130)
        )

        panelDerecho.add(scrollRuta)

        for (componente in panelDerecho.components) {
            if (componente is JComponent) componente.alignmentX = Component.LEFT_ALIGNMENT
        }
        val scrollDerecho = JScrollPane(panelDerecho)
        scrollDerecho.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollDerecho.preferredSize = Dimension(405, 0)
        scrollDerecho.border = null
        scrollDerecho.verticalScrollBar.unitIncrement = 18
        principal.add(scrollDerecho, BorderLayout.EAST)

        add(principal)
    }

    // Los botones generan una instantánea nueva del tablero; no reutilizan
    // un diagrama antiguo después de cambiar paredes, entrada o salida.
    private fun modeloActualParaDiagrama(): ModeloAutomata? {
        val ruta = BuscadorRuta.buscar(Array(FILAS) { laberinto[it]!!.copyOf() },
            entradaFila, entradaColumna, salidaFila, salidaColumna)
        if (ruta == null) {
            JOptionPane.showMessageDialog(this,
                "No hay una ruta hacia la salida en este tablero.\n" +
                    "El diagrama y la exportación representan una ruta aceptada.\n" +
                    "Puedes usar BUSCAR Y EJECUTAR para observar la exploración sin salida.",
                "Autómata del recorrido", JOptionPane.INFORMATION_MESSAGE)
            return null
        }
        return AutomataRobot.construir(ruta)
    }

    private fun guardarAutomata(padre: Component, modelo: ModeloAutomata) {
        val selector = JFileChooser()
        selector.dialogTitle = "Guardar autómata para JFLAP"
        selector.fileFilter = FileNameExtensionFilter("Autómata de JFLAP (*.jff)", "jff")
        selector.selectedFile = File("Robot_Automata.jff")
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) return
        var archivo = selector.selectedFile
        if (!archivo.extension.equals("jff", ignoreCase = true)) archivo = File(archivo.parentFile, archivo.name + ".jff")
        if (archivo.exists() && JOptionPane.showConfirmDialog(padre,
                "El archivo ya existe. ¿Deseas reemplazarlo?", "Guardar autómata",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return
        try {
            AutomataRobot.exportar(modelo, archivo)
            JOptionPane.showMessageDialog(padre,
                "Autómata guardado en:\n${archivo.absolutePath}\n\n" +
                    "En JFLAP: File → Open y selecciona el .jff.\n" +
                    "Prueba la cadena de movimientos que aparece en Ver autómata.",
                "Exportación completada", JOptionPane.INFORMATION_MESSAGE)
        } catch (ex: Exception) {
            JOptionPane.showMessageDialog(padre,"No se pudo guardar el archivo:\n${ex.message}",
                "Error al exportar",JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun configurarBoton(boton: JButton) {
        boton.setAlignmentX(CENTER_ALIGNMENT)

        boton.setMaximumSize(
            Dimension(Int.MAX_VALUE, 35)
        )

        boton.setFocusPainted(false)
    }

    // =========================================================
    // INICIALIZAR
    // =========================================================
    private fun inicializarLaberinto() {
        robotFila = entradaFila
        robotColumna = entradaColumna
        for (fila in 0..<FILAS) {
            for (columna in 0..<COLUMNAS) {
                laberinto[fila]!![columna] = CAMINO
            }
        }

        laberinto[entradaFila]!![entradaColumna] = ENTRADA
        laberinto[salidaFila]!![salidaColumna] = SALIDA
    }

    // =========================================================
    // MODIFICAR CASILLA
    // =========================================================
    private fun modificarCasilla(fila: Int, columna: Int) {
        detenerAnimacion()

        limpiarRecorridoSinActualizar()
        limpiarPaneles()

        when (herramientaActual) {
            Herramienta.PARED -> if (!esEntrada(fila, columna)
                && !esSalida(fila, columna)
            ) {
                laberinto[fila]!![columna] = PARED
            }

            Herramienta.BORRAR -> if (!esEntrada(fila, columna)
                && !esSalida(fila, columna)
            ) {
                laberinto[fila]!![columna] = CAMINO
            }

            Herramienta.ENTRADA -> if (!esSalida(fila, columna)) {
                laberinto[entradaFila]!![entradaColumna] = CAMINO

                entradaFila = fila
                entradaColumna = columna

                laberinto[fila]!![columna] = ENTRADA
            }

            Herramienta.SALIDA -> if (!esEntrada(fila, columna)) {
                laberinto[salidaFila]!![salidaColumna] = CAMINO

                salidaFila = fila
                salidaColumna = columna

                laberinto[fila]!![columna] = SALIDA
            }

        }

        robotFila = entradaFila
        robotColumna = entradaColumna
        actualizarTablero()
    }

    // =========================================================
    // BUSCAR SALIDA
    // =========================================================
    private fun buscarSalida() {
        detenerAnimacion()
        limpiarRecorridoSinActualizar()
        limpiarPaneles()
        lblEstado!!.text = "1/3 · CALCULANDO RUTA MÁS CORTA"
        // BFS prepara el camino. No se anima la exploración ni se mueve el robot.
        val ruta = BuscadorRuta.buscar(Array(FILAS) { laberinto[it]!!.copyOf() },
            entradaFila, entradaColumna, salidaFila, salidaColumna)
        if (ruta == null) {
            iniciarExploracionSinSalida()
            return
        }
        casillasRuta = ruta
        rutaPreparada = ruta.drop(1).joinToString("") { it.movimiento }
        mostrarRuta(ruta)
        btnBuscar!!.isEnabled = false
        btnPausa!!.isEnabled = true
        btnPausa!!.text = "Pausar"
        iniciarVistaRuta(ruta.size - 1)
    }

    // =========================================================
    // MOSTRAR RUTA
    // =========================================================
    private fun mostrarRuta(ruta: List<PasoRuta>) {
        val movimientos = StringBuilder()

        for (i in 1..<ruta.size) {
            movimientos.append(ruta.get(i).movimiento)

            if (i < ruta.size - 1) {
                movimientos.append(" → ")
            }
        }

        txtRuta!!.setText(
            ("Ruta más corta encontrada:\n\n"
                    + movimientos
                    + "\n\nTotal de movimientos: "
                    + (ruta.size - 1))
        )
    }

    // =========================================================
    // ANIMACIÓN
    // =========================================================
    private data class PasoExploracion(
        val fila: Int,
        val columna: Int,
        val retroceso: Boolean,
        val pila: List<Pair<Int, Int>>
    )

    /** Solo se usa cuando BFS no encontró ruta. Reproduce avances y retrocesos
     * continuos por todas las casillas accesibles, antes de anunciar el fallo.
     * La pila de búsqueda es auxiliar y no representa aceptación del ADP.
     */
    private fun iniciarExploracionSinSalida() {
        val visitado = Array(FILAS) { BooleanArray(COLUMNAS) }
        val pila = mutableListOf<Pair<Int, Int>>()
        val pasos = mutableListOf<PasoExploracion>()
        val direcciones = arrayOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
        fun explorar(fila: Int, columna: Int) {
            visitado[fila][columna] = true
            pila.add(fila to columna)
            pasos.add(PasoExploracion(fila, columna, false, pila.toList()))
            for ((df, dc) in direcciones) {
                val f = fila + df
                val c = columna + dc
                if (f !in 0 until FILAS || c !in 0 until COLUMNAS) continue
                if (visitado[f][c] || laberinto[f]!![c] == PARED) continue
                explorar(f, c)
                pasos.add(PasoExploracion(fila, columna, true, pila.toList()))
            }
            pila.removeAt(pila.lastIndex)
        }
        explorar(entradaFila, entradaColumna)
        var indice = 0
        btnBuscar!!.isEnabled = false
        btnPausa!!.isEnabled = true
        btnPausa!!.text = "Pausar"
        lblEstado!!.text = "EXPLORANDO CAMINOS..."
        lblOperacion!!.text = "<html>Naranja: casillas exploradas.<br>El robot avanza y retrocede para probar los caminos.</html>"
        lblPila!!.text = "PILA AUXILIAR DE BÚSQUEDA"
        txtPila!!.text = "Preparando la búsqueda..."
        txtRuta!!.text = "Explorando los caminos accesibles desde la entrada..."
        actualizarTablero()
        timerAnimacion = Timer(demora) {
            if (indice < pasos.size) {
                val paso = pasos[indice++]
                robotFila = paso.fila
                robotColumna = paso.columna
                if (!esEntrada(robotFila, robotColumna) && !esSalida(robotFila, robotColumna)) {
                    laberinto[robotFila]!![robotColumna] = EXPLORADO
                }
                lblEstado!!.text = if (paso.retroceso) "RETROCEDIENDO..." else "EXPLORANDO CAMINOS..."
                txtPila!!.text = "TOPE\n──────\n" + paso.pila.asReversed().joinToString("\n") {
                    "q" + obtenerNumeroEstado(it.first, it.second)
                }
                actualizarTablero()
            } else {
                timerAnimacion!!.stop()
                btnBuscar!!.isEnabled = true
                btnPausa!!.isEnabled = false
                btnPausa!!.text = "Pausar"
                lblEstado!!.text = "SIN RUTA · BÚSQUEDA AGOTADA"
                lblOperacion!!.text = "<html>Se agotaron los caminos accesibles.<br>La pila auxiliar vacía indica fallo de búsqueda.</html>"
                txtPila!!.text = "TOPE\n──────\n(vacía: búsqueda agotada)"
                txtRuta!!.text = "No existe una ruta hacia la salida. El robot regresó a la entrada."
                actualizarTablero()
                JOptionPane.showMessageDialog(this,
                    "El robot exploró todos los caminos accesibles y regresó al inicio.\n" +
                        "No existe una ruta hacia la salida.",
                    "Sin salida", JOptionPane.WARNING_MESSAGE)
            }
        }
        timerAnimacion!!.start()
    }

    private fun iniciarVistaRuta(movimientos: Int) {
        var indice = 1
        lblEstado!!.text = "1/3 · TRAZANDO RUTA HACIA LA SALIDA"
        lblOperacion!!.text = "<html>Amarillo: ruta prevista.<br>La pila se cargará al terminar el trazado.</html>"
        lblPila!!.text = "PILA · TODAVÍA NO INICIADA"
        txtPila!!.text = "La carga comienza después del trazado."
        txtEntrada!!.text = rutaPreparada
        actualizarTablero()
        timerAnimacion = Timer(demora) {
            if (indice < casillasRuta.size) {
                val casilla = casillasRuta[indice++]
                casillasPendientes.add(casilla.fila to casilla.columna)
                actualizarTablero()
            } else {
                timerAnimacion!!.stop()
                iniciarEjecucionADP(movimientos)
            }
        }
        timerAnimacion!!.start()
    }

    private fun iniciarEjecucionADP(movimientos: Int) {
        limpiarRecorridoSinActualizar()
        lblPila!!.text = "PILA DEL ADP · TOPE ARRIBA"
        motor = MotorADP(rutaPreparada, rutaPreparada, entradaFila, entradaColumna,
            salidaFila, salidaColumna) { f, c ->
            f in 0 until FILAS && c in 0 until COLUMNAS && laberinto[f]!![c] != PARED
        }
        actualizarMotor()
        timerAnimacion = Timer(demora) {
            val adp = motor ?: return@Timer
            val consumidosAntes = adp.consumidos
            adp.paso()
            if (adp.consumidos > consumidosAntes) {
                casillasPendientes.remove(adp.fila to adp.columna)
            }
            robotFila = adp.fila
            robotColumna = adp.columna
            if (adp.consumidos > 0 && !esEntrada(robotFila, robotColumna) && !esSalida(robotFila, robotColumna)) {
                laberinto[robotFila]!![robotColumna] = RUTA
            }
            actualizarMotor()
            if (adp.fase == MotorADP.Fase.ACEPTADO || adp.fase == MotorADP.Fase.RECHAZADO) {
                timerAnimacion!!.stop()
                btnBuscar!!.isEnabled = true
                btnPausa!!.isEnabled = false
                btnPausa!!.text = "Pausar"
                val aceptado = adp.fase == MotorADP.Fase.ACEPTADO
                JOptionPane.showMessageDialog(this,
                    if (aceptado) "ACEPTADO POR PILA VACÍA\n\nRobot en la salida.\n" +
                        "Entrada completamente consumida.\nZ₀ retirado.\nMovimientos: $movimientos"
                    else adp.mensaje,
                    if (aceptado) "Meta alcanzada" else "Ruta rechazada",
                    if (aceptado) JOptionPane.INFORMATION_MESSAGE else JOptionPane.WARNING_MESSAGE)
            }
        }
        timerAnimacion!!.start()
    }

    private fun actualizarMotor() {
        val adp = motor ?: return
        lblEstado!!.text = when (adp.fase) {
            MotorADP.Fase.CARGANDO -> "2/3 · CARGANDO PILA"
            MotorADP.Fase.EJECUTANDO -> "3/3 · LEER, DESAPILAR Y AVANZAR"
            MotorADP.Fase.ACEPTADO -> "✓ ACEPTADO · PILA VACÍA"
            MotorADP.Fase.RECHAZADO -> "✗ RECHAZADO"
        }
        txtEntrada!!.text = if (adp.pendiente.isEmpty()) "ε (entrada consumida)" else adp.pendiente
        lblOperacion!!.text = "<html><body style=\"width: 260px\">" + adp.mensaje + "</body></html>"
        txtPila!!.text = "TOPE\n──────\n" +
            if (adp.pila.isEmpty()) "ε (pila vacía)" else
                adp.pila.asReversed().joinToString("\n") { if (it == 'Z') "Z₀ (fondo)" else it.toString() }
        actualizarTablero()
    }

    private fun limpiarPaneles() {
        motor = null
        rutaPreparada = ""
        casillasRuta = emptyList()
        casillasPendientes.clear()
        txtPila!!.text = ""
        txtRuta!!.text = ""
        txtEntrada!!.text = ""
        lblPila!!.text = "PILA · TOPE ARRIBA"
        lblOperacion!!.text = " "
        lblEstado!!.text = "Listo para comenzar"
    }

    // =========================================================
    // ACTUALIZAR TABLERO
    // =========================================================
    private fun actualizarTablero() {
        for (fila in 0..<FILAS) {
            for (columna in 0..<COLUMNAS) {
                val boton = botones[fila][columna]

                val numeroEstado =
                    obtenerNumeroEstado(fila, columna)

                boton.setText("q" + numeroEstado)

                when (laberinto[fila]!![columna]) {
                    CAMINO -> {
                        boton.setBackground(Color.WHITE)
                        boton.setForeground(Color.BLACK)
                    }

                    PARED -> {
                        boton.setBackground(Color.DARK_GRAY)
                        boton.setForeground(Color.WHITE)
                    }

                    ENTRADA -> {
                        boton.setBackground(
                            Color(100, 220, 120)
                        )
                        boton.setForeground(Color.BLACK)
                        boton.setText("IN")
                    }

                    SALIDA -> {
                        boton.setBackground(
                            Color(255, 100, 100)
                        )
                        boton.setForeground(Color.BLACK)
                        boton.setText("OUT")
                    }

                    EXPLORADO -> {
                        boton.background = Color(255, 190, 120)
                        boton.foreground = Color.BLACK
                    }

                    VISITADO -> {
                        boton.background = Color(255, 220, 120)
                        boton.foreground = Color.BLACK
                    }

                    RUTA -> {
                        boton.setBackground(
                            Color(100, 180, 255)
                        )
                        boton.setForeground(Color.BLACK)
                    }
                }
                if ((fila to columna) in casillasPendientes) {
                    boton.background = Color(255, 220, 120)
                    boton.foreground = Color.BLACK
                }
                if (fila == robotFila && columna == robotColumna) boton.text = ""
                boton.repaint()
            }
        }
    }

    // =========================================================
    // LIMPIAR RECORRIDO
    // =========================================================
    private fun limpiarRecorrido() {
        limpiarRecorridoSinActualizar()

        limpiarPaneles()

        lblEstado!!.setText("Listo para comenzar")

        actualizarTablero()
    }

    private fun limpiarRecorridoSinActualizar() {
        robotFila = entradaFila
        robotColumna = entradaColumna
        for (fila in 0..<FILAS) {
            for (columna in 0..<COLUMNAS) {
                if (laberinto[fila]!![columna] == RUTA
                    || laberinto[fila]!![columna] == VISITADO
                    || laberinto[fila]!![columna] == EXPLORADO
                ) {
                    laberinto[fila]!![columna] = CAMINO
                }
            }
        }

        laberinto[entradaFila]!![entradaColumna] = ENTRADA
        laberinto[salidaFila]!![salidaColumna] = SALIDA
    }

    // =========================================================
    // NUEVO LABERINTO
    // =========================================================
    private fun nuevoLaberinto() {
        detenerAnimacion()

        entradaFila = 0
        entradaColumna = 0

        salidaFila = 14
        salidaColumna = 14

        inicializarLaberinto()

        limpiarPaneles()

        lblEstado!!.setText("Nuevo laberinto")

        actualizarTablero()
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================
    private fun esEntrada(fila: Int, columna: Int): Boolean {
        return fila == entradaFila
                && columna == entradaColumna
    }

    private fun esSalida(fila: Int, columna: Int): Boolean {
        return fila == salidaFila
                && columna == salidaColumna
    }

    private fun obtenerNumeroEstado(fila: Int, columna: Int): Int {
        return fila * COLUMNAS + columna
    }

    private fun detenerAnimacion() {
        if (timerAnimacion != null
            && timerAnimacion!!.isRunning()
        ) {
            timerAnimacion!!.stop()
        }

        btnPausa?.isEnabled = false
        btnPausa?.text = "Pausar"
        if (btnBuscar != null) {
            btnBuscar!!.setEnabled(true)
        }
    }

    companion object {
        // =========================================================
        // CONFIGURACIÓN GENERAL
        // =========================================================
        private const val FILAS = 15
        private const val COLUMNAS = 15

        // Tipos de casilla
        private const val CAMINO = 0
        private const val PARED = 1
        private const val ENTRADA = 2
        private const val SALIDA = 3
        private const val RUTA = 4
        private const val VISITADO = 5
        private const val EXPLORADO = 6

        // =========================================================
        // MAIN
        // =========================================================
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater(
                Runnable { RobotLaberinto() }
            )
        }
    }
}
