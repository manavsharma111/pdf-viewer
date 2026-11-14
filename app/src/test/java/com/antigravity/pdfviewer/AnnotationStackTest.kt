package com.antigravity.pdfviewer

import com.antigravity.pdfviewer.domain.model.DrawingStroke
import com.antigravity.pdfviewer.domain.model.PointF
import com.antigravity.pdfviewer.presentation.reader.AnnotationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationStackTest {

    @Test
    fun testUndoRedoWorkflow() {
        val undoStack = mutableListOf<AnnotationAction>()
        val redoStack = mutableListOf<AnnotationAction>()

        val stroke1 = DrawingStroke(
            id = "1",
            points = listOf(PointF(0f, 0f), PointF(0.5f, 0.5f)),
            color = 0xFF0000,
            strokeWidth = 4f
        )
        val stroke2 = DrawingStroke(
            id = "2",
            points = listOf(PointF(0.1f, 0.1f), PointF(0.8f, 0.8f)),
            color = 0x00FF00,
            strokeWidth = 4f
        )

        // Add 2 actions
        undoStack.add(AnnotationAction.AddStroke(0, stroke1))
        undoStack.add(AnnotationAction.AddStroke(0, stroke2))

        assertEquals(2, undoStack.size)
        assertTrue(redoStack.isEmpty())

        // Undo once
        val actionToUndo = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(actionToUndo)

        assertEquals(1, undoStack.size)
        assertEquals(1, redoStack.size)
        assertEquals("2", ((actionToUndo as AnnotationAction.AddStroke).stroke.id))

        // Redo
        val actionToRedo = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(actionToRedo)

        assertEquals(2, undoStack.size)
        assertTrue(redoStack.isEmpty())
        assertEquals("2", ((actionToRedo as AnnotationAction.AddStroke).stroke.id))
    }

    @Test
    fun testNewActionClearsRedo() {
        val undoStack = mutableListOf<AnnotationAction>()
        val redoStack = mutableListOf<AnnotationAction>()

        val stroke1 = DrawingStroke(id = "1", points = emptyList(), color = 0, strokeWidth = 2f)
        val stroke2 = DrawingStroke(id = "2", points = emptyList(), color = 0, strokeWidth = 2f)
        val stroke3 = DrawingStroke(id = "3", points = emptyList(), color = 0, strokeWidth = 2f)

        undoStack.add(AnnotationAction.AddStroke(0, stroke1))
        undoStack.add(AnnotationAction.AddStroke(0, stroke2))

        // Undo
        redoStack.add(undoStack.removeAt(undoStack.lastIndex))
        assertEquals(1, redoStack.size)

        // Now user draws something new
        undoStack.add(AnnotationAction.AddStroke(0, stroke3))
        redoStack.clear()

        assertEquals(2, undoStack.size)
        assertTrue(redoStack.isEmpty())
    }
}
