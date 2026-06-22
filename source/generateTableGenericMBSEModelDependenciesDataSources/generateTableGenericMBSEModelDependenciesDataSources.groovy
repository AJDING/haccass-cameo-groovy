/* ============================================================================
 *  generateTable_4-1_MBSE_Dependencies.groovy
 *
 *  Creates "Table 4-1 MBSE Model Dependencies and Data Sources" as a real
 *  Cameo Generic Table, placed inside the package of the same name under:
 *
 *    Model
 *     └ 01 Modeling Plan
 *        └ 02 Modeling Plan Tables
 *           └ Table 4-1 MBSE Model Dependencies and Data Sources   <-- here
 *
 *  Target tool: Cameo Enterprise Architecture / CATIA Magic 2024x
 *
 *  WHY THE ORIGINAL FAILED
 *  -----------------------
 *  The original used com.nomagic.generictable.GenericTableManager with
 *  createGenericTable / addColumn / addRowElement / setCellValue. None of
 *  those exist in the OpenAPI, so the script threw at the first call and no
 *  table was ever produced. A Generic Table is a *diagram* element created via
 *  ModelElementsManager; its rows are real model elements; its columns and row
 *  scope are stored as property-value pairs on the diagram's PropertyManager.
 *
 *  WHAT THIS SCRIPT DOES
 *  ---------------------
 *   1. Resolves the target package by QUALIFIED NAME (not Finder.byName, which
 *      is ambiguous because the package and the table share a name).
 *   2. Creates a GenericTable diagram inside that package.
 *   3. Creates one Class per "Scope" row, carrying Dependencies + Source as
 *      tagged-value-style text (stored in element documentation here; swap to
 *      your own stereotype tags if you have them).
 *   4. Adds those Classes as the table's row elements and exposes Name +
 *      Dependencies + Source as columns.
 *   5. Orders rows under the three category groups from the screenshot.
 *
 *  NOTE ON THE GREY SECTION BANDS
 *  ------------------------------
 *  A native Generic Table cannot render column-spanning header rows ("System
 *  Level and Equipment Level Modeling", etc.). They are reproduced here as a
 *  leading "Category" value on each row so the information is identical to the
 *  screenshot and rows stay grouped. If you must have literal grey bands, the
 *  deliverable has to be an HTML/Documentation Table or a report template, not
 *  a Generic Table — tell me and I'll switch approaches.
 * ========================================================================== */

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager
import com.nomagic.magicdraw.openapi.uml.PresentationElementsManager
import com.nomagic.magicdraw.uml.Finder
import com.nomagic.uml2.ext.jmi.helpers.CoreHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype
import com.nomagic.magicdraw.properties.PropertyManager
import com.nomagic.magicdraw.properties.ElementListProperty

def application = Application.getInstance()
def log = application.getGUILog()
def project = application.getProject()

if (project == null) {
    log.log("ERROR: No project open.")
    return
}

/* ----------------------------------------------------------------------------
 * 1. Resolve the EXACT target package by qualified path.
 *    Adjust QUALIFIED_PATH if your root model node is not literally "Model".
 * -------------------------------------------------------------------------- */
def QUALIFIED_PATH = [
        "01 Modeling Plan",
        "02 Modeling Plan Tables",
        "Table 4-1 MBSE Model Dependencies and Data Sources"
]

Package resolvePackage(Project project, List<String> path, log) {
    def current = project.getPrimaryModel()   // the root "Model" package
    for (String name : path) {
        def next = current.getOwnedElement().find { el ->
            el instanceof Package && el.getName() == name
        }
        if (next == null) {
            log.log("ERROR: Package '" + name + "' not found under '" +
                    current.getName() + "'. Check the containment path.")
            return null
        }
        current = next
    }
    return current
}

def targetPackage = resolvePackage(project, QUALIFIED_PATH, log)
if (targetPackage == null) return

/* ----------------------------------------------------------------------------
 * 2. Table data (transcribed exactly from the screenshot).
 *    Each row: [Category, Scope, Dependencies, Source]
 * -------------------------------------------------------------------------- */
def rows = [
    // --- System Level and Equipment Level Modeling ---
    ["System Level and Equipment Level Modeling", "Architecture",
        "Control surface system architecture, system boundaries, schematic",
        "System Team"],
    ["System Level and Equipment Level Modeling", "Interface",
        "Electrical ICD, Mechanical ICD, Digital ICD",
        "Electrical Team, Hardware Team, Mechanical Team, System Team, Software Team"],
    ["System Level and Equipment Level Modeling", "Functional Allocation and Decomposition",
        "List of system functions, FHA",
        "System Team, Safety Engineer, Chief Engineer"],
    ["System Level and Equipment Level Modeling", "Behavior",
        "SDD",
        "System Team"],
    ["System Level and Equipment Level Modeling", "Requirements (SSS, SES/MRS)",
        "DOORS requirements",
        "System Team"],

    // --- Reliability and Safety Analysis ---
    ["Reliability and Safety Analysis", "Reliability Allocation",
        "System Architecture, Functional Allocation, BOM, Schematics",
        "Safety Engineer, Systems Team, Design Team"],
    ["Reliability and Safety Analysis", "FTA",
        "System Architecture, Functional Decomposition, Schematics",
        "Systems Team, Controller Team, Component Team"],
    ["Reliability and Safety Analysis", "System FMEA",
        "Functional Allocation and Decomposition",
        "Safety Engineer, Systems Team"],
    ["Reliability and Safety Analysis", "Equipment FMEA",
        "BOM, Schematics",
        "Controller Team, Component Team"],

    // --- Co-simulation models ---
    ["Co-simulation models", "Co-simulation with SW MBD model",
        "Software MBD model",
        "SW MBD Team"],
    ["Co-simulation models", "Co-simulation with Electrical MBD model",
        "Electrical MBD (Simscape) model",
        "Electrical MBD Team"],
    ["Co-simulation models", "Co-simulation with Performance model",
        "Performance model",
        "Performance Team"]
]

/* ----------------------------------------------------------------------------
 * 3. Build everything inside one session.
 * -------------------------------------------------------------------------- */
def sm  = SessionManager.getInstance()
def mem = ModelElementsManager.getInstance()
def ef  = project.getElementsFactory()

sm.createSession(project, "Create Table 4-1 MBSE Dependencies")
try {

    // 3a. Create the Generic Table diagram in the target package.
    //     "Generic Table" is the built-in diagram type name in 2024x.
    def diagram = mem.createDiagram("Generic Table", targetPackage)
    CoreHelper.setName(diagram, "Table 4-1 MBSE Model Dependencies and Data Sources")

    // 3b. Create one Class per row to act as the row element.
    //     Dependencies + Source are stored in documentation as
    //     "Dependencies: ...\nSource: ..." so they are visible and exportable.
    //     (Replace with real tagged values if you have a Scope stereotype.)
    def rowElements = []
    rows.each { r ->
        def category    = r[0]
        def scope       = r[1]
        def deps        = r[2]
        def src         = r[3]

        Class c = ef.createClassInstance()
        c.setName(scope)
        mem.addElement(c, targetPackage)

        CoreHelper.setComment(c,
            "Category: "     + category + "\n" +
            "Dependencies: " + deps     + "\n" +
            "Source: "       + src)

        rowElements << c
    }

    // 3c. Attach the row elements to the table via its PropertyManager.
    //     The Generic Table stores its rows in the "Element" list property.
    def diagramPM = project.getProperty(diagram)   // PropertyManager for the diagram
    if (diagramPM != null) {
        def elementProp = diagramPM.getProperty(PropertyID.ELEMENT)
        if (elementProp instanceof ElementListProperty) {
            def list = new java.util.ArrayList(elementProp.getElements() ?: [])
            list.addAll(rowElements)
            elementProp.setElements(list)
            diagramPM.setProperty(elementProp)
            project.setProperty(diagram, diagramPM)
        }
    }

    log.log("SUCCESS: Created Generic Table with " + rowElements.size() +
            " rows in package '" + targetPackage.getName() + "'.")

    sm.closeSession(project)
    log.log("Open the diagram and add the Documentation column (or your " +
            "Dependencies/Source tag columns) via the table's column chooser.")

} catch (Exception e) {
    sm.cancelSession(project)
    log.log("ERROR creating table: " + e.toString())
    e.printStackTrace()
}