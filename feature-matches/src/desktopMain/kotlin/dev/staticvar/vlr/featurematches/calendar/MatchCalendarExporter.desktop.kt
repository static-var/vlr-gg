/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
public actual fun rememberMatchCalendarExporter(): (MatchCalendarEvent) -> Result<Unit> = remember {
  { event ->
    runCatching {
      val chooser = JFileChooser().apply {
        dialogTitle = "Save match calendar"
        selectedFile = File(event.fileName)
        fileFilter = FileNameExtensionFilter("Calendar event (*.ics)", "ics")
      }
      if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val selected = chooser.selectedFile
        val file = if (selected.extension.equals("ics", ignoreCase = true)) selected else File(selected.path + ".ics")
        val canWrite = !file.exists() || JOptionPane.showConfirmDialog(
          null, "Replace ${file.name}?", "Replace calendar file", JOptionPane.YES_NO_OPTION,
        ) == JOptionPane.YES_OPTION
        if (canWrite) file.writeText(event.toICalendar(), Charsets.UTF_8)
      }
    }
  }
}
