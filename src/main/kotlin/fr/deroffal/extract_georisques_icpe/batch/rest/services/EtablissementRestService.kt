package fr.deroffal.extract_georisques_icpe.batch.rest.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.deroffal.extract_georisques_icpe.batch.rest.HttpBuilder
import fr.deroffal.extract_georisques_icpe.batch.rest.beans.EtablissementDto
import fr.deroffal.extract_georisques_icpe.service.DateService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class EtablissementRestService(
    private val dateService: DateService,
    private val httpBuilder: HttpBuilder,
    private val mapper: ObjectMapper
) {

    fun listerIdInstallation(parametreExport: ParametreExport): List<String> {
        return httpBuilder.getAsString(parametreExport.buildUri(dateService.today()))
            .split("\n").filterNot { it.isEmpty() }.drop(1)
            .map { it.split(";") }
            .map { it[0].replace('.', '-') }
    }

    fun recupererEtablissement(numeroEtablissement: String): EtablissementDto {
        val etablissementStr = httpBuilder.getAsString("/etablissement/$numeroEtablissement")
        return mapper.readValue(etablissementStr)
    }
}


class ParametreExport private constructor(
    val parametreGeographiqueExport: ParametreGeographiqueExport?,
    val exclureSeveso: Boolean = false
) {

    private fun hasParametreGeographique() = parametreGeographiqueExport != null
    private fun getParametreGeographique() =
        if (hasParametreGeographique()) parametreGeographiqueExport!!.asUrlParam() else ""

    fun buildUri(date: LocalDate): String {
        //URI minimale pour avoir un export
        var baseUri = "/sitesdetails/detailsites_${
            date.format(DateTimeFormatter.ISO_DATE)
        }.csv?etablissement=&isExport=true"
        if (hasParametreGeographique()) {
            baseUri += "&${getParametreGeographique()}"
        }
        if (exclureSeveso) {
            baseUri += "&statut=NS"
        }
        return baseUri
    }

    data class Builder(
        var parametreGeographiqueExport: ParametreGeographiqueExport? = null,
        var exclureSeveso: Boolean = true
    ) {

        fun parametreGeographiqueExport(parametreGeographiqueExport: String?) = apply {
            this.parametreGeographiqueExport =
                if (parametreGeographiqueExport != null) ParametreGeographiqueExport.valueOf(parametreGeographiqueExport) else ParametreGeographiqueExport.PAYS_DE_LA_LOIRE
        }

        fun exclureSeveso(exclureSeveso: String?) = apply { this.exclureSeveso = (exclureSeveso ?: "true").toBoolean() }
        fun build() = ParametreExport(parametreGeographiqueExport, exclureSeveso)
    }
}

enum class ParametreGeographiqueExport(val clef: String, val code: Int) {

    NANTES("commune", 44109),
    LOIRE_ATLANTIQUE("departement", 44),
    PAYS_DE_LA_LOIRE("region", 52);

    fun asUrlParam() = "${this.clef}=${this.code}"
}
