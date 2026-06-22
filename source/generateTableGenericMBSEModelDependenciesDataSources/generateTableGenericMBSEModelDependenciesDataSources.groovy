import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.uml.Finder
import com.nomagic.magicdraw.uml.BaseElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.*
import com.nomagic.generictable.*
import com.nomagic.generictable.GenericTableManager

//-----------------------------------------
// Setup project and session
//-----------------------------------------
def project = Application.getInstance().getProject()
def model = project.getPrimaryModel()

SessionManager.getInstance().createSession(project, "Create MBSE Generic Table")

try {

    //-----------------------------------------
    // Find target package
    //-----------------------------------------
    def targetPackage = Finder.byName().find(project, 
        "Table 4-1 MBSE Model Dependencies and Data Sources")

    if (targetPackage == null) {
        println "ERROR: Target package not found."
        return
    }

    //-----------------------------------------
    // Create Generic Table
    //-----------------------------------------
    def table = GenericTableManager.getInstance().createGenericTable(
        project, 
        "Table 4-1 MBSE Model dependencies and data sources", 
        targetPackage
    )

    //-----------------------------------------
    // Define columns
    //-----------------------------------------
    def columns = ["Scope", "Dependencies", "Source"]

    columns.each { col ->
        GenericTableManager.getInstance().addColumn(table, col)
    }

    //-----------------------------------------
    // Helper method to add row
    //-----------------------------------------
    def addRow = { scope, dep, src ->
        def row = GenericTableManager.getInstance().addRowElement(table, targetPackage)

        GenericTableManager.getInstance().setCellValue(table, row, "Scope", scope)
        GenericTableManager.getInstance().setCellValue(table, row, "Dependencies", dep)
        GenericTableManager.getInstance().setCellValue(table, row, "Source", src)
    }

    //-----------------------------------------
    // Populate data (from your image)
    //-----------------------------------------

    // System Level and Equipment Level Modeling
    addRow("Architecture",
        "Control surface system architecture, system boundaries, schematic",
        "System Team")

    addRow("Interface",
        "Electrical ICD, Mechanical ICD, Digital ICD",
        "Electrical Team; Hardware Team; Mechanical Team; System Team; Software Team")

    addRow("Functional Allocation and Decomposition",
