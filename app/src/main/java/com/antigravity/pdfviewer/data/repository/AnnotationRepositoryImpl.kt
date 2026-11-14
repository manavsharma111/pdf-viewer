package com.antigravity.pdfviewer.data.repository

import com.antigravity.pdfviewer.data.local.dao.AnnotationDao
import com.antigravity.pdfviewer.data.local.entity.AnnotationEntity
import com.antigravity.pdfviewer.domain.model.AnnotationType
import com.antigravity.pdfviewer.domain.model.DrawingStroke
import com.antigravity.pdfviewer.domain.model.HighlightAnnotation
import com.antigravity.pdfviewer.domain.model.PageAnnotations
import com.antigravity.pdfviewer.domain.model.PointF
import com.antigravity.pdfviewer.domain.model.TextAnnotation
import com.antigravity.pdfviewer.domain.repository.AnnotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AnnotationRepositoryImpl(
    private val dao: AnnotationDao
) : AnnotationRepository {

    override fun getAnnotations(documentUri: String): Flow<Map<Int, PageAnnotations>> {
        return dao.getAnnotationsForDocument(documentUri).map { entities ->
            val resultMap = mutableMapOf<Int, PageAnnotations>()

            // Group by pageIndex
            val grouped = entities.groupBy { it.pageIndex }
            for ((pageIndex, pageEntities) in grouped) {
                val strokes = mutableListOf<DrawingStroke>()
                val textNotes = mutableListOf<TextAnnotation>()
                val highlights = mutableListOf<HighlightAnnotation>()

                for (entity in pageEntities) {
                    try {
                        val json = JSONObject(entity.dataJson)
                        when (entity.type) {
                            "STROKE" -> {
                                val pointsArr = json.getJSONArray("points")
                                val points = mutableListOf<PointF>()
                                for (i in 0 until pointsArr.length()) {
                                    val pObj = pointsArr.getJSONObject(i)
                                    points.add(PointF(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                                }
                                strokes.add(
                                    DrawingStroke(
                                        id = entity.id,
                                        points = points,
                                        color = json.getInt("color"),
                                        strokeWidth = json.getDouble("width").toFloat(),
                                        alpha = json.optDouble("alpha", 1.0).toFloat(),
                                        isEraser = json.optBoolean("eraser", false)
                                    )
                                )
                            }
                            "TEXT" -> {
                                val pObj = json.getJSONObject("pos")
                                textNotes.add(
                                    TextAnnotation(
                                        id = entity.id,
                                        text = json.getString("text"),
                                        position = PointF(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()),
                                        color = json.getInt("color"),
                                        fontSize = json.getDouble("size").toFloat()
                                    )
                                )
                            }
                            "HIGHLIGHT" -> {
                                val sObj = json.getJSONObject("start")
                                val eObj = json.getJSONObject("end")
                                highlights.add(
                                    HighlightAnnotation(
                                        id = entity.id,
                                        start = PointF(sObj.getDouble("x").toFloat(), sObj.getDouble("y").toFloat()),
                                        end = PointF(eObj.getDouble("x").toFloat(), eObj.getDouble("y").toFloat()),
                                        color = json.getInt("color"),
                                        strokeWidth = json.getDouble("width").toFloat(),
                                        type = runCatching { AnnotationType.valueOf(json.optString("type", "HIGHLIGHT")) }.getOrDefault(AnnotationType.HIGHLIGHT)
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore malformed entity
                    }
                }
                resultMap[pageIndex] = PageAnnotations(
                    pageIndex = pageIndex,
                    strokes = strokes,
                    textNotes = textNotes,
                    highlights = highlights
                )
            }
            resultMap
        }
    }

    override suspend fun savePageAnnotations(
        documentUri: String,
        pageIndex: Int,
        annotations: PageAnnotations
    ) {
        dao.deleteForPage(documentUri, pageIndex)

        val entities = mutableListOf<AnnotationEntity>()
        val now = System.currentTimeMillis()

        for (stroke in annotations.strokes) {
            val json = JSONObject().apply {
                put("color", stroke.color)
                put("width", stroke.strokeWidth)
                put("alpha", stroke.alpha)
                put("eraser", stroke.isEraser)
                val arr = JSONArray()
                for (p in stroke.points) {
                    arr.put(JSONObject().apply {
                        put("x", p.x)
                        put("y", p.y)
                    })
                }
                put("points", arr)
            }
            entities.add(
                AnnotationEntity(
                    id = stroke.id,
                    documentUri = documentUri,
                    pageIndex = pageIndex,
                    type = "STROKE",
                    dataJson = json.toString(),
                    timestamp = now
                )
            )
        }

        for (text in annotations.textNotes) {
            val json = JSONObject().apply {
                put("text", text.text)
                put("color", text.color)
                put("size", text.fontSize)
                put("pos", JSONObject().apply {
                    put("x", text.position.x)
                    put("y", text.position.y)
                })
            }
            entities.add(
                AnnotationEntity(
                    id = text.id,
                    documentUri = documentUri,
                    pageIndex = pageIndex,
                    type = "TEXT",
                    dataJson = json.toString(),
                    timestamp = now
                )
            )
        }

        for (hl in annotations.highlights) {
            val json = JSONObject().apply {
                put("color", hl.color)
                put("width", hl.strokeWidth)
                put("type", hl.type.name)
                put("start", JSONObject().apply {
                    put("x", hl.start.x)
                    put("y", hl.start.y)
                })
                put("end", JSONObject().apply {
                    put("x", hl.end.x)
                    put("y", hl.end.y)
                })
            }
            entities.add(
                AnnotationEntity(
                    id = hl.id,
                    documentUri = documentUri,
                    pageIndex = pageIndex,
                    type = "HIGHLIGHT",
                    dataJson = json.toString(),
                    timestamp = now
                )
            )
        }

        dao.insertAll(entities)
    }

    override suspend fun clearPageAnnotations(documentUri: String, pageIndex: Int) {
        dao.deleteForPage(documentUri, pageIndex)
    }

    override suspend fun clearDocumentAnnotations(documentUri: String) {
        dao.deleteForDocument(documentUri)
    }
}
