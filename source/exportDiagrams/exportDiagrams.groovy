import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.export.image.ImageExporter
import java.io.File

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

def outputRoot = new File(scriptDir, "CameoExports")
outputRoot.mkdirs()

