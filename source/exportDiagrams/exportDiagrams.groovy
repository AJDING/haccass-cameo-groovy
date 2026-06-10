import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.export.image.ImageExporter
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane

def project = Application.getInstance().getProject()
if (project == null) {
    println "No open project."
    return
}

/////////////////////////////////////////////
// OUTPUT DIRECTORY (SAME AS SCRIPT LOCATION)
/////////////////////////////////////////////

def scriptDir
try {
    def scriptPath = this.class.protectionDomain.codeSource.location.toURI()
    def scriptFile = new File(scriptPath)
    scriptDir = scriptFile.isFile() ? scriptFile.getParentFile() : scriptFile
} catch (Exception e) {
    // fallback if Cameo hides script path
    scriptDir = new File(System.getProperty("user.dir"))
}

/////////////////////////////////////////////
// USER FOLDER SELECTION (OVERRIDES scriptDir)
// Borrowed from exportDiagramsTest.groovy
// Lets user pick the exact output location,
// bypassing Cameo's sandboxed script path.
/////////////////////////////////////////////

def chooser = new JFileChooser()
chooser.setDialogTitle("Select Output Folder for Diagram/Table Export")
chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
chooser.setAcceptAllFileFilterUsed(false)
// Default to the detected scriptDir so the dialog opens near the script
if (scriptDir != null && scriptDir.exists()) {
    chooser.setCurrentDirectory(scriptDir)
}
int chooserResult = chooser.showOpenDialog(null)

if (chooserResult == JFileChooser.APPROVE_OPTION) {
    scriptDir = chooser.getSelectedFile()
} else {
    JOptionPane.showMessageDialog(null, "No folder selected. Export cancelled.")
    return
}

def outputRoot = new File(scriptDir, "CameoExports")
outputRoot.mkdirs()

println "SCRIPT LOCATION: " + scriptDir.getAbsolutePath()
println "EXPORT DIRECTORY: " + outputRoot.getAbsolutePath()
println "Export starting..."

/////////////////////////////////////////////
// DIAGRAM EXPORT
/////////////////////////////////////////////

def diagrams = project.getDiagrams()
println "Total diagrams found: " + diagrams.size()

diagrams.each { d ->
    try {
        // d IS already the DiagramPresentationElement from getDiagrams()
        // No need for project.getDiagram(d) which returns null
        def dpe = d
        if (dpe == null) return

        // Load and open the diagram so ImageExporter can render it
        dpe.ensureLoaded()
        dpe.open()

        String type = dpe.getDiagramType()?.getType() ?: "Unknown"
        String safeType = type.replaceAll("[^a-zA-Z0-9_]", "_")

        File typeDir = new File(outputRoot, "Diagrams/${safeType}")
        typeDir.mkdirs()

        String rawName = dpe.getDiagram()?.getName() ?: "Unnamed"
        String name = rawName.replaceAll("[^a-zA-Z0-9_]", "_")
        File outputFile = new File(typeDir, name + ".png")

        // Handle duplicate filenames
        int dupeCounter = 1
        while (outputFile.exists()) {
            outputFile = new File(typeDir, name + "_" + dupeCounter + ".png")
            dupeCounter++
        }

        ImageExporter.export(dpe, ImageExporter.PNG, outputFile)

        if (outputFile.exists()) {
            println "SUCCESS: " + outputFile.getAbsolutePath()
        } else {
            println "FAILED TO CREATE: " + outputFile.getAbsolutePath()
        }

    } catch (Exception e) {
        println "Failed diagram: " + (d.getDiagram()?.getName() ?: "unknown") + " - " + e.getMessage()
    }
}

/////////////////////////////////////////////
// MATRIX EXPORT (SAFE REFLECTION)
/////////////////////////////////////////////

try {
    def matrixManagerClass = Class.forName("com.nomagic.magicdraw.matrix.MatrixManager")
    def matrixExporterClass = Class.forName("com.nomagic.magicdraw.matrix.MatrixExporter")

    def manager = matrixManagerClass.getMethod("getInstance").invoke(null)
    def matrices = matrixManagerClass.getMethod("getMatrices", project.getClass()).invoke(manager, project)

    matrices.each { m ->
        try {
            File typeDir = new File(outputRoot, "Tables/Matrix")
            typeDir.mkdirs()

            String name = m.getName().replaceAll("[^a-zA-Z0-9_]", "_")
            File outputFile = new File(typeDir, name + ".xlsx")

            matrixExporterClass.getMethod("exportToExcel", m.getClass(), File.class)
                    .invoke(null, m, outputFile)

            println "Exported matrix: " + outputFile.getAbsolutePath()

        } catch (Exception ex) {
            println "Failed matrix: " + m.getName()
        }
    }

} catch (Exception e) {
    println "Matrix plugin not available. Skipping matrix export."
}

/////////////////////////////////////////////
// GENERIC TABLE EXPORT (SAFE)
/////////////////////////////////////////////

try {
    def gtManagerClass = Class.forName("com.nomagic.magicdraw.generic.table.GenericTableManager")
    def gtExporterClass = Class.forName("com.nomagic.magicdraw.generic.table.export.GenericTableExporter")

    def manager = gtManagerClass.getMethod("getInstance").invoke(null)
    def tables = gtManagerClass.getMethod("getTables", project.getClass()).invoke(manager, project)

    tables.each { t ->
        try {
            File typeDir = new File(outputRoot, "Tables/GenericTable")
            typeDir.mkdirs()

            String name = t.getName().replaceAll("[^a-zA-Z0-9_]", "_")
            File outputFile = new File(typeDir, name + ".xlsx")

            gtExporterClass.getMethod("exportToExcel", t.getClass(), File.class)
                    .invoke(null, t, outputFile)

            println "Exported table: " + outputFile.getAbsolutePath()

        } catch (Exception ex) {
            println "Failed table: " + t.getName()
        }
    }

} catch (Exception e) {
    println "Generic Table plugin not available. Skipping table export."
}

println "Export complete!"