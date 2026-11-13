package org.example.project.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.project.data.network.SupabaseProvider
import org.example.project.domain.repository.PsiRepository
import org.example.project.models.EarthApplication
import org.example.project.models.FireIncident
import org.example.project.models.HabitatProfile
import org.example.project.models.HabitatRankingItem
import org.example.project.models.PsiFilter
import org.example.project.models.PsiFilterOptions
import org.example.project.models.PsiInvestigation
import org.example.project.models.PsiPublication
import org.example.project.models.PsiTestCondition

/**
 * NASA PSI verileri — tamamen Supabase'den okunur (mock / hardcoded veri yok).
 * Katalog küçük (24 deney) olduğundan çoklu filtreler (O₂ / basınç aralık kesişimi dahil)
 * tek sorgudan sonra istemcide uygulanır.
 */
class PsiRepositoryImpl(
    private val supabaseProvider: SupabaseProvider
) : PsiRepository {

    private val db get() = supabaseProvider.client.postgrest

    private suspend fun fetchAll(): List<PsiInvestigation> =
        db["psi_investigation_overview"].select {
            order(column = "psi_id", order = Order.ASCENDING)
        }.decodeList<PsiInvestigation>()

    override suspend fun getInvestigations(filter: PsiFilter): List<PsiInvestigation> {
        if (!supabaseProvider.isReadyForQueries) return emptyList()
        return try {
            fetchAll().filter { inv ->
                val q = filter.query?.trim()?.lowercase()
                (q.isNullOrBlank() || listOfNotNull(inv.title, inv.acronym, inv.psiId, inv.objective)
                    .plus(inv.fuels)
                    .any { it.lowercase().contains(q) }) &&
                    (filter.categories.isEmpty() || inv.safetyCategory in filter.categories) &&
                    (filter.fuelStates.isEmpty() || inv.fuelState in filter.fuelStates) &&
                    (filter.hardwareGroups.isEmpty() || inv.hardwareGroup in filter.hardwareGroups) &&
                    (filter.flightOnly == null || filter.flightOnly == !inv.isGroundStudy) &&
                    (filter.o2Range == null || inv.overlapsO2(filter.o2Range.start, filter.o2Range.endInclusive)) &&
                    (filter.pressureRangeAtm == null ||
                        inv.overlapsPressure(filter.pressureRangeAtm.start, filter.pressureRangeAtm.endInclusive))
            }.sortedWith(compareBy({ it.startYear ?: Int.MAX_VALUE }, { it.psiId }))
        } catch (e: Exception) {
            println("[HATA] PSI getInvestigations başarısız: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getInvestigation(psiId: String): PsiInvestigation? {
        if (!supabaseProvider.isReadyForQueries) return null
        return try {
            db["psi_investigation_overview"].select {
                filter { eq("psi_id", psiId) }
            }.decodeList<PsiInvestigation>().firstOrNull()
        } catch (e: Exception) {
            println("[HATA] PSI getInvestigation($psiId) başarısız: ${e.message}")
            null
        }
    }

    override suspend fun getInvestigationsByIds(psiIds: List<String>): List<PsiInvestigation> {
        if (!supabaseProvider.isReadyForQueries || psiIds.isEmpty()) return emptyList()
        return try {
            val list = db["psi_investigation_overview"].select {
                filter { isIn("psi_id", psiIds) }
            }.decodeList<PsiInvestigation>()
            psiIds.mapNotNull { id -> list.firstOrNull { it.psiId == id } }
        } catch (e: Exception) {
            println("[HATA] PSI getInvestigationsByIds başarısız: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTestConditions(psiId: String): List<PsiTestCondition> {
        if (!supabaseProvider.isReadyForQueries) return emptyList()
        return try {
            db["psi_test_conditions"].select {
                filter { eq("psi_id", psiId) }
                order(column = "source_file", order = Order.ASCENDING)
                order(column = "row_index", order = Order.ASCENDING)
            }.decodeList<PsiTestCondition>()
        } catch (e: Exception) {
            println("[HATA] PSI getTestConditions($psiId) başarısız: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getPublications(psiId: String): List<PsiPublication> {
        if (!supabaseProvider.isReadyForQueries) return emptyList()
        return try {
            db["psi_publications"].select {
                filter { eq("psi_id", psiId) }
                order(column = "id", order = Order.ASCENDING)
            }.decodeList<PsiPublication>()
        } catch (e: Exception) {
            println("[HATA] PSI getPublications($psiId) başarısız: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getFilterOptions(): PsiFilterOptions {
        if (!supabaseProvider.isReadyForQueries) return PsiFilterOptions()
        return try {
            val all = fetchAll()
            val o2 = all.flatMap { listOfNotNull(it.o2Min, it.o2Max) }
            val p = all.flatMap { listOfNotNull(it.pressureMinAtm, it.pressureMaxAtm) }
            PsiFilterOptions(
                categories = all.mapNotNull { it.safetyCategory }.distinct().sortedBy { it.ordinal },
                fuelStates = all.mapNotNull { it.fuelState }.distinct().sortedBy { it.ordinal },
                hardwareGroups = all.mapNotNull { it.hardwareGroup?.takeIf { g -> g.isNotBlank() } }.distinct().sorted(),
                o2Bounds = if (o2.isEmpty()) null else o2.min()..o2.max(),
                pressureBoundsAtm = if (p.isEmpty()) null else p.min()..p.max()
            )
        } catch (e: Exception) {
            println("[HATA] PSI getFilterOptions başarısız: ${e.message}")
            PsiFilterOptions()
        }
    }

    override suspend fun getHabitatProfiles(): List<HabitatProfile> {
        if (!supabaseProvider.isReadyForQueries) return emptyList()
        return try {
            db["habitat_profiles"].select().decodeList<HabitatProfile>()
                .sortedByDescending { it.pressurePsi }
        } catch (e: Exception) {
            println("[HATA] PSI getHabitatProfiles başarısız: ${e.message}")
            emptyList()
        }
    }

    override suspend fun rankForHabitat(habitatId: String?, o2Percent: Double?, pressureAtm: Double?): List<HabitatRankingItem> {
        if (!supabaseProvider.isReadyForQueries) return emptyList()
        return try {
            db.rpc(
                function = "rank_investigations_for_habitat",
                parameters = buildJsonObject {
                    put("p_habitat_id", habitatId ?: "lunar_base")
                    o2Percent?.let { put("p_o2_percent", it) }
                    pressureAtm?.let { put("p_pressure_atm", it) }
                }
            ).decodeList<HabitatRankingItem>()
        } catch (e: Exception) {
            println("[HATA] PSI rankForHabitat başarısız: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getFireIncidents(): List<FireIncident> {
        if (!supabaseProvider.isReadyForQueries) return emptyList()
        return try {
            db["fire_incidents"].select {
                order(column = "year", order = Order.ASCENDING)
            }.decodeList<FireIncident>()
        } catch (e: Exception) {
            println("[HATA] PSI getFireIncidents başarısız: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getEarthApplications(): List<EarthApplication> {
        if (!supabaseProvider.isReadyForQueries) return emptyList()
        return try {
            db["earth_applications"].select {
                order(column = "id", order = Order.ASCENDING)
            }.decodeList<EarthApplication>()
        } catch (e: Exception) {
            println("[HATA] PSI getEarthApplications başarısız: ${e.message}")
            emptyList()
        }
    }
}
