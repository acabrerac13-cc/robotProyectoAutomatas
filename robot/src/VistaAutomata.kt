import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Visor de la misma instantánea que se exporta a JFLAP. No altera la simulación. */
class VistaAutomata(propietario: JFrame, modelo: ModeloAutomata,
                    guardar: (Component, ModeloAutomata) -> Unit) : JDialog(propietario, "Autómata del recorrido", false) {
    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        val principal = JPanel(BorderLayout(8,8))
        principal.border = javax.swing.border.EmptyBorder(10,10,10,10)
        val encabezado = JPanel(BorderLayout())
        encabezado.add(JLabel("<html><b>ADP de la ruta actual · ${modelo.cadena.length} movimientos</b><br>"+
            "Instantánea: carga c0…c${modelo.cadena.length} → recorrido q… → ACEPTA.<br>"+
            "Flechas: leer, sacar ; poner. ε = sin símbolo. Z = fondo de pila.</html>"),BorderLayout.CENTER)
        val exportar = JButton("Exportar a JFLAP")
        exportar.addActionListener { guardar(this,modelo) }
        encabezado.add(exportar,BorderLayout.EAST)
        principal.add(encabezado,BorderLayout.NORTH)
        val panel = PanelDiagrama(modelo)
        val desplazamiento = JScrollPane(panel)
        desplazamiento.verticalScrollBar.unitIncrement = 28
        val herramientas = JPanel(FlowLayout(FlowLayout.LEFT))
        herramientas.add(JLabel("Vista:"))
        val zoom = JSlider(35,150,85)
        zoom.preferredSize = Dimension(160,30)
        herramientas.add(zoom)
        zoom.addChangeListener { panel.escala = zoom.value / 100.0; panel.revalidate(); panel.repaint() }
        fun ir(nombre: String, indice: Int) {
            val b = JButton(nombre)
            b.addActionListener {
                val (x,y) = modelo.posicion(indice)
                panel.scrollRectToVisible(Rectangle(((x-80)*panel.escala).toInt(),
                    ((y-60)*panel.escala).toInt(),(180*panel.escala).toInt(),(130*panel.escala).toInt()))
            }
            herramientas.add(b)
        }
        ir("Carga",0); ir("Recorrido",modelo.cadena.length+1); ir("Aceptación",modelo.estados.lastIndex)
        val dibujo = JPanel(BorderLayout())
        dibujo.add(herramientas,BorderLayout.NORTH); dibujo.add(desplazamiento,BorderLayout.CENTER)
        val tablaModelo = object : DefaultTableModel(arrayOf("Origen","Destino","Lee","Saca","Pone"),0) {
            override fun isCellEditable(fila: Int, columna: Int) = false
        }
        modelo.transiciones.forEach { t ->
            val nombres = modelo.estados.associate {it.id to it.nombre}
            tablaModelo.addRow(arrayOf(nombres[t.origen],nombres[t.destino],
                t.leer.ifEmpty {"ε"},t.sacar.ifEmpty {"ε"},t.poner.ifEmpty {"ε"}))
        }
        val tabla = JTable(tablaModelo)
        tabla.rowHeight = 25
        val tabs = JTabbedPane()
        tabs.addTab("Diagrama",dibujo); tabs.addTab("Tabla de transiciones",JScrollPane(tabla))
        principal.add(tabs,BorderLayout.CENTER)
        val entrada = JTextArea("Cadena para probar en JFLAP (copiar solo los movimientos):\n${modelo.cadena}")
        entrada.isEditable = false; entrada.lineWrap = true; entrada.rows=3
        entrada.font=Font("Monospaced",Font.PLAIN,14)
        principal.add(JScrollPane(entrada),BorderLayout.SOUTH)
        contentPane=principal
        setSize(1050,760); setLocationRelativeTo(propietario)
    }
}

class PanelDiagrama(private val modelo: ModeloAutomata) : JPanel() {
    var escala = 0.85
    init { background = Color.WHITE }
    override fun getPreferredSize(): Dimension = Dimension((980*escala).toInt(),
        ((modelo.posicion(modelo.estados.lastIndex).second+100)*escala).toInt())
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val d = g.create() as Graphics2D
        try {
            d.scale(escala,escala)
            d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)
            val indices = modelo.estados.withIndex().associate {it.value.id to it.index}
            d.font=Font("SansSerif",Font.PLAIN,13); d.stroke=BasicStroke(1.7f)
            for(t in modelo.transiciones) {
                val (ax,ay)=modelo.posicion(indices.getValue(t.origen))
                val (bx,by)=modelo.posicion(indices.getValue(t.destino))
                val a=atan2((by-ay).toDouble(),(bx-ax).toDouble())
                val x1=ax+cos(a)*31; val y1=ay+sin(a)*31
                val x2=bx-cos(a)*33; val y2=by-sin(a)*33
                d.color=Color(70,85,105);d.draw(Line2D.Double(x1,y1,x2,y2))
                val punta=Path2D.Double()
                punta.moveTo(x2,y2)
                punta.lineTo(x2-11*cos(a-.4),y2-11*sin(a-.4))
                punta.lineTo(x2-11*cos(a+.4),y2-11*sin(a+.4));punta.closePath();d.fill(punta)
                val etiqueta=t.etiqueta(); val ancho=d.fontMetrics.stringWidth(etiqueta)
                val tx=if(ax==bx) (ax+12) else ((ax+bx-ancho)/2)
                val ty=if(ay==by) (ay-12) else ((ay+by)/2)
                d.color=Color.WHITE;d.fillRect(tx-4,ty-15,ancho+8,20)
                d.color=Color(35,45,65);d.drawString(etiqueta,tx,ty)
            }
            for((i,e) in modelo.estados.withIndex()) {
                val (x,y)=modelo.posicion(i)
                d.color=when(e.etapa){"Carga"->Color(220,235,255);"Recorrido"->Color(220,247,224);else->Color(255,229,178)}
                d.fillOval(x-30,y-30,60,60)
                d.color=Color(40,65,90);d.drawOval(x-30,y-30,60,60)
                if(e.final)d.drawOval(x-25,y-25,50,50)
                if(e.inicial) {
                    d.drawLine(x-75,y,x-33,y);d.drawLine(x-42,y-6,x-33,y);d.drawLine(x-42,y+6,x-33,y)
                }
                d.font=Font("SansSerif",Font.BOLD,if(e.final)11 else 14)
                d.drawString(e.nombre,x-d.fontMetrics.stringWidth(e.nombre)/2,y+5)
                d.font=Font("SansSerif",Font.PLAIN,12)
                d.drawString(e.etapa,x-d.fontMetrics.stringWidth(e.etapa)/2,y+49)
            }
        } finally { d.dispose() }
    }
}
