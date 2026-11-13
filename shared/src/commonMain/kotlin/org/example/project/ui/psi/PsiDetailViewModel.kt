package org.example.project.ui.psi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.example.project.domain.usecase.GetPsiInvestigationUseCase
import org.example.project.domain.usecase.GetPsiPublicationsUseCase
import org.example.project.domain.usecase.GetPsiTestConditionsUseCase
import org.example.project.models.PsiInvestigation
import org.example.project.models.PsiPublication
import org.example.project.models.PsiTestCondition

/** Bir PSI "Experimental table" CSV dosyasının tablo görünümü (kolonlar JSON anahtarlarından dinamik). */
data class TestTable(
    val sourceFile: String,
    val columns: List<String>,
    val rows: List<List<String>>
) {
    val shortName: String
        get() = sourceFile.substringAfter("Experimental table_").removeSuffix(".csv").ifBlank { sourceFile }

    fun toCsv(): String {
        fun esc(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        return buildString {
            appendLine(columns.joinToString(",") { esc(it) })
            rows.forEach { r -> appendLine(r.joinToString(",") { esc(it) }) }
        }
    }

    fun toJson(): String {
        fun q(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
        return rows.joinToString(",\n", prefix = "[\n", postfix = "\n]") { r ->
            columns.indices.filter { r[it].isNotEmpty() }
                .joinToString(", ", prefix = "  {", postfix = "}") { i -> "${q(columns[i])}: ${q(r[i])}" }
        }
    }
}

data class PsiDetailUiState(
    val isLoading: Boolean = true,
    val investigation: PsiInvestigation? = null,
    val tables: List<TestTable> = emptyList(),
    val publications: List<PsiPublication> = emptyList(),
    val notFound: Boolean = false
)

class PsiDetailViewModel(
    private val getInvestigation: GetPsiInvestigationUseCase,
    private val getTestConditions: GetPsiTestConditionsUseCase,
    private val getPublications: GetPsiPublicationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PsiDetailUiState())
    val uiState: StateFlow<PsiDetailUiState> = _uiState.asStateFlow()
    private var loadedId: String? = null

    fun load(psiId: String) {
        if (loadedId == psiId) return
        loadedId = psiId
        _uiState.value = PsiDetailUiState(isLoading = true)
        viewModelScope.launch {
            val inv = async { getInvestigation(psiId) }
            val tc = async { getTestConditions(psiId) }
            val pubs = async { getPublications(psiId) }
            val investigation = inv.await()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    investigation = investigation,
                    tables = buildTables(tc.await()),
                    publications = pubs.await(),
                    notFound = investigation == null
                )
            }
        }
    }

    private fun cell(v: Any?): String = when (v) {
        is JsonPrimitive -> v.contentOrNull ?: ""
        is JsonArray -> v.joinToString("; ") { cell(it) }
        is JsonObject -> v.toString()
        else -> ""
    }

    private fun buildTables(rows: List<PsiTestCondition>): List<TestTable> =
        rows.groupBy { it.sourceFile }.map { (file, list) ->
            val columns = LinkedHashSet<String>()
            list.sortedBy { it.rowIndex }.forEach { columns.addAll(it.data.keys) }
            val cols = columns.toList()
            TestTable(
                sourceFile = file,
                columns = cols,
                rows = list.sortedBy { it.rowIndex }.map { r -> cols.map { c -> cell(r.data[c]) } }
            )
        }
}
