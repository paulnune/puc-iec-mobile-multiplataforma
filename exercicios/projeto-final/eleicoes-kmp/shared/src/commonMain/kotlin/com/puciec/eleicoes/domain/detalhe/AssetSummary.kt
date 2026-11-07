package com.puciec.eleicoes.domain.detalhe

import com.puciec.eleicoes.domain.model.Asset

/** Bens de um mesmo tipo (ex.: "Terreno"): quantos e quanto somam. */
data class AssetGroup(val typeLabel: String, val count: Int, val total: Double)

/**
 * Bens declarados na ordem da declaração, com o total e o resumo por tipo.
 * [declaredCount] é quantos bens a API diz existir; maior que `assets.size` quando a lista foi cortada.
 */
data class AssetSummary(
    val assets: List<Asset>,
    val total: Double,
    val groups: List<AssetGroup>,
    val declaredCount: Int = assets.size,
) {
    val isTruncated: Boolean get() = declaredCount > assets.size
}

const val OTHER_ASSET_TYPE = "Outros"

/**
 * Rótulo curto do tipo: o texto antes do primeiro ":" ("Veículo automotor terrestre: caminhão, ...").
 * Alguns tipos vêm todo em maiúsculas ("OUTROS BENS E DIREITOS"): esses viram "Outros bens e direitos".
 */
fun shortAssetType(typeLabel: String): String {
    val short = typeLabel.substringBefore(':').trim().ifBlank { return OTHER_ASSET_TYPE }
    val allCaps = short.any { it.isLetter() } && short == short.uppercase()
    return if (allCaps) short.lowercase().replaceFirstChar { it.uppercaseChar() } else short
}

/** Soma os bens e agrupa pelo tipo curto; grupos do maior para o menor total (empate: ordem alfabética). */
fun List<Asset>.summarize(declaredCount: Int? = null): AssetSummary {
    val ordered = sortedBy { it.order }
    val groups = ordered
        .groupBy { shortAssetType(it.typeLabel) }
        .map { (type, items) -> AssetGroup(type, items.size, items.sumOf { it.value }) }
        .sortedWith(compareByDescending<AssetGroup> { it.total }.thenBy { it.typeLabel })
    return AssetSummary(
        assets = ordered,
        total = ordered.sumOf { it.value },
        groups = groups,
        declaredCount = maxOf(declaredCount ?: ordered.size, ordered.size),
    )
}
