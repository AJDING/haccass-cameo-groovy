// =============================================================
// Export All Diagrams to Word-Ready Document
// Language: Groovy (for Cameo EA Macro Engine)
// 
// HOW TO USE:
//   1. Open your model in Cameo EA
//   2. Go to Tools > Macros > Create Macro...
//   3. Set Language to "Groovy"
//   4. Paste this entire script
//   5. Click Run
//   6. Select an output folder when prompted
//   7. Open the generated .html file in Microsoft Word
//   8. File > Save As > Word Document (.docx)
// =============================================================

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.export.image.ImageExporter
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import javax.swing.JFileChooser
import javax.swing.JOptionPane

// --- Step 1: Get the active project ---
def project = Application.getInstance().getProject()
if (project == null) {
    JOptionPane.showMessageDialog(null, "No project is open. Please open a model first.")
    return
}

def projectName = project.getName()
def allDiagrams = project.getDiagrams()

if (allDiagrams == null || allDiagrams.isEmpty()) {
    JOptionPane.showMessageDialog(null, "No diagrams found in the current project.")
    return
}

// --- Step 2: Ask user to select output folder ---
def chooser = new JFileChooser()
chooser.setDialogTitle("Select Output Folder for Diagram Export")
chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
chooser.setAcceptAllFileFilterUsed(false)
int result = chooser.showOpenDialog(null)

if (result != JFileChooser.APPROVE_OPTION) {
    return // User cancelled
}

def outputDir = chooser.getSelectedFile()
def imagesDir = new File(outputDir, "diagram_images")
imagesDir.mkdirs()

// --- Step 3: Group diagrams by type ---
def diagramsByType = [:].withDefault { [] }

allDiagrams.each { DiagramPresentationElement dpe ->
    def diagramType = dpe.getDiagramType()?.getType() ?: "Unknown"
    def diagramName = dpe.getDiagram()?.getName() ?: "Unnamed"
    diagramsByType[diagramType] << [name: diagramName, element: dpe]
}

// Sort the types alphabetically
def sortedTypes = diagramsByType.keySet().sort()

// --- Step 4: Export each diagram as PNG ---
def exportedCount = 0
def failedCount = 0
def diagramData = [:].withDefault { [] } // type -> list of [name, filename]

sortedTypes.each { type ->
    // Create subfolder for each diagram type
    def safeType = type.replaceAll(/[^a-zA-Z0-9_ -]/, "_")
    def typeDir = new File(imagesDir, safeType)
    typeDir.mkdirs()
    
    diagramsByType[type].sort { it.name }.each { entry ->
        def dpe = entry.element
        def name = entry.name
        def safeName = name.replaceAll(/[^a-zA-Z0-9_ -]/, "_").take(80)
        def imageFile = new File(typeDir, "${safeName}.png")
        
        // Handle duplicate filenames
        int counter = 1
        while (imageFile.exists()) {
            imageFile = new File(typeDir, "${safeName}_${counter}.png")
            counter++
        }
        
        try {
            // Open/load the diagram before exporting
            dpe.ensureLoaded()
            dpe.open()
            ImageExporter.export(dpe, ImageExporter.PNG, imageFile)
            diagramData[type] << [name: name, file: imageFile, relativePath: "diagram_images/${safeType}/${imageFile.getName()}"]
            exportedCount++
        } catch (Exception e) {
            Application.getInstance().getGUILog().log("Failed to export: ${name} - ${e.getMessage()}")
            failedCount++
        }
    }
}

// --- Step 5: Generate HTML document (openable in Word) ---
def htmlFile = new File(outputDir, "${projectName}_All_Diagrams.html")

def html = new StringBuilder()
html.append("""<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${projectName} - All Diagrams</title>
<style>
    body { 
        font-family: Arial, sans-serif; 
        margin: 40px; 
        color: #333; 
        line-height: 1.5;
    }
    h1 { 
        color: #1B3A5C; 
        font-size: 28pt; 
        border-bottom: 3px solid #2E75B6; 
        padding-bottom: 10px;
        page-break-before: always;
    }
    h1:first-of-type {
        page-break-before: avoid;
    }
    h2 { 
        color: #2E75B6; 
        font-size: 16pt; 
        margin-top: 30px;
    }
    .title-page {
        text-align: center; 
        padding-top: 200px;
        page-break-after: always;
    }
    .title-page h1 { 
        font-size: 36pt; 
        border: none; 
        page-break-before: avoid;
    }
    .title-page .subtitle { 
        font-size: 18pt; 
        color: #2E75B6; 
    }
    .title-page .meta { 
        font-size: 12pt; 
        color: #666; 
        margin-top: 40px; 
    }
    .toc { 
        page-break-after: always; 
    }
    .toc h1 { 
        page-break-before: avoid; 
    }
    .toc ul { 
        list-style: none; 
        padding-left: 0; 
    }
    .toc li { 
        margin: 6px 0; 
        font-size: 12pt; 
    }
    .toc .type-entry { 
        font-weight: bold; 
        font-size: 13pt; 
        margin-top: 12px; 
    }
    .toc .count { 
        color: #666; 
        font-weight: normal; 
    }
    .diagram-container { 
        margin: 20px 0 40px 0; 
        text-align: center;
    }
    .diagram-container img { 
        max-width: 100%; 
        border: 1px solid #ccc; 
    }
    .caption { 
        font-style: italic; 
        color: #666; 
        font-size: 10pt; 
        margin-top: 6px; 
    }
    .summary-table {
        border-collapse: collapse;
        width: 60%;
        margin: 20px auto;
    }
    .summary-table th {
        background-color: #1B3A5C;
        color: white;
        padding: 8px 16px;
        text-align: left;
        font-size: 11pt;
    }
    .summary-table td {
        padding: 6px 16px;
        border: 1px solid #ccc;
        font-size: 11pt;
    }
    .summary-table tr:nth-child(even) {
        background-color: #EBF1F8;
    }
</style>
</head>
<body>
""")

// Title Page
html.append("""
<div class="title-page">
    <h1>${projectName}</h1>
    <div class="subtitle">Complete Diagram Export</div>
    <div class="meta">
        <p>Generated: ${new Date().format('yyyy-MM-dd HH:mm')}</p>
        <p>Total Diagrams: ${exportedCount}</p>
        <p>Diagram Types: ${sortedTypes.size()}</p>
        <p>Tool: Cameo Enterprise Architecture 2024x</p>
    </div>
</div>
""")

// Table of Contents
html.append("""<div class="toc"><h1>Table of Contents</h1><ul>""")

def figureNum = 1
sortedTypes.each { type ->
    def count = diagramData[type].size()
    html.append("""<li class="type-entry">${type} <span class="count">(${count} diagram${count > 1 ? 's' : ''})</span></li><ul>""")
    diagramData[type].each { entry ->
        html.append("""<li>Figure ${figureNum}: ${entry.name}</li>""")
        figureNum++
    }
    html.append("</ul>")
}
html.append("</ul>")

// Summary Table
html.append("""
<table class="summary-table">
    <tr><th>Diagram Type</th><th>Count</th></tr>
""")
sortedTypes.each { type ->
    html.append("""<tr><td>${type}</td><td>${diagramData[type].size()}</td></tr>""")
}
html.append("""
    <tr style="font-weight:bold; background-color:#D6E4F0;">
        <td>Total</td><td>${exportedCount}</td>
    </tr>
</table>
</div>
""")

// Diagram Sections
figureNum = 1
sortedTypes.each { type ->
    html.append("""<h1>${type}</h1>""")
    
    diagramData[type].each { entry ->
        html.append("""
        <h2>Figure ${figureNum}: ${entry.name}</h2>
        <div class="diagram-container">
            <img src="${entry.relativePath}" alt="${entry.name}">
            <div class="caption">Figure ${figureNum} — ${entry.name} (${type})</div>
        </div>
        """)
        figureNum++
    }
}

html.append("""
</body>
</html>
""")

htmlFile.text = html.toString()

// --- Step 6: Report results ---
def message = """Export Complete!

Diagrams exported: ${exportedCount}
Failed: ${failedCount}
Diagram types: ${sortedTypes.size()}

Output files:
  HTML Document: ${htmlFile.getAbsolutePath()}
  Image Folder:  ${imagesDir.getAbsolutePath()}

NEXT STEPS:
  1. Open the HTML file in Microsoft Word
  2. File > Save As > Word Document (.docx)
  3. Update the Table of Contents (right-click TOC > Update Field)
"""

Application.getInstance().getGUILog().log(message)
JOptionPane.showMessageDialog(null, message, "Export Complete", JOptionPane.INFORMATION_MESSAGE)
