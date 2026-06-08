import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.export.image.ImageExporter
import com.nomagic.magicdraw.ui.browser.Browser
import com.nomagic.magicdraw.uml.DiagramTypeConstants

import com.nomagic.magicdraw.matrix.MatrixManager
import com.nomagic.magicdraw.matrix.Matrix
import com.nomagic.magicdraw.matrix.MatrixExporter

import java.io.File

def project = Application.getInstance().getProject()
if (project == null) {
    println "No open project."
    return
}

// OUTPUT ROOT DIRECTORY
def outputRoot = new File("C:/Temp/CameoExports")
outputRoot.mkdirs()

println "Export starting..."

/////////////////////////////////////////////
// DIAGRAM EXPORT SECTION
/////////////////////////////////////////////

def diagrams = project.getDiagrams()

diagrams.each { d ->
    try {
        DiagramPresentationElement dpe = project.getDiagram(d)

        if (dpe == null) return

        // Get diagram type (friendly)
        String type = d.getHumanType()
        String safeType = type.replaceAll("[^a-zA-Z0-9_]", "_")

        // Folder by type
        File typeDir = new File(outputRoot, "Diagrams/${safeType}")
        typeDir.mkdirs()

        // File name cleanup
        String name = d.getName().replaceAll("[^a-zA-Z0-9_]", "_")

        File outputFile = new File(typeDir, name + ".png")

        // Export image
        ImageExporter.export(dpe, ImageExporter.PNG, outputFile, 300)

        println "Exported diagram: ${type} -> ${name}"

    } catch (Exception e) {
        println "Failed diagram: " + d.getName()
    }
}

/////////////////////////////////////////////
// TABLE / MATRIX EXPORT SECTION
/////////////////////////////////////////////

def matrices = MatrixManager.getInstance().getMatrices(project)

matrices.each { m ->
    try {
        String type = "Matrix"
        String safeType = type.replaceAll("[^a-zA-Z0-9_]", "_")

        File typeDir = new File(outputRoot, "Tables/${safeType}")
        typeDir.mkdirs()

        String name = m.getName().replaceAll("[^a-zA-Z0-9_]", "_")

        File outputFile = new File(typeDir, name + ".xlsx")

        // Export matrix to Excel
        MatrixExporter.exportToExcel(m, outputFile)

        println "Exported matrix: ${name}"

    } catch (Exception e) {
        println "Failed matrix: " + m.getName()
    }
}

/////////////////////////////////////////////
// GENERIC TABLES EXPORT (if available)
/////////////////////////////////////////////

try {
    def tables = com.nomagic.magicdraw.generic.table.GenericTableManager.getInstance().getTables(project)

    tables.each { t ->
        try {
            String type = "GenericTable"
            String safeType = type.replaceAll("[^a-zA-Z0-9_]", "_")

            File typeDir = new File(outputRoot, "Tables/${safeType}")
            typeDir.mkdirs()

            String name = t.getName().replaceAll("[^a-zA-Z0-9_]", "_")

            File outputFile = new File(typeDir, name + ".xlsx")

            com.nomagic.magicdraw.generic.table.export.GenericTableExporter.exportToExcel(t, outputFile)

            println "Exported table: ${name}"

        } catch (Exception ex) {
            println "Failed table: " + t.getName()
        }
    }

} catch (Exception e) {
    println "Generic tables not available or plugin missing."
}

println "Export complete!"