package com.puciec.eleicoes

/**
 * Tabela oficial dos partidos de 2026 definida pelo grupo (sigla oficial | nome oficial), na ordem por sigla.
 * É o que a aba Partidos (#8) deve mostrar com "Ordenar por Sigla".
 */
internal val TABELA_OFICIAL_2026 = listOf(
    "AGIR | Agir",
    "AVANTE | Avante",
    "CIDADANIA | Cidadania",
    "DC | Democracia Cristã",
    "DEMOCRATA | Democrata",
    "MDB | Movimento Democrático Brasileiro",
    "MISSÃO | Partido Missão",
    "MOBILIZA | Mobilização Nacional",
    "NOVO | Partido Novo",
    "PCB | Partido Comunista Brasileiro",
    "PCdoB | Partido Comunista do Brasil",
    "PCO | Partido da Causa Operária",
    "PDT | Partido Democrático Trabalhista",
    "PL | Partido Liberal",
    "PODE | Podemos",
    "PP | Progressistas",
    "PRD | Partido Renovação Democrática",
    "PRTB | Partido Renovador Trabalhista Brasileiro",
    "PSB | Partido Socialista Brasileiro",
    "PSD | Partido Social Democrático",
    "PSDB | Partido da Social Democracia Brasileira",
    "PSOL | Partido Socialismo e Liberdade",
    "PSTU | Partido Socialista dos Trabalhadores Unificado",
    "PT | Partido dos Trabalhadores",
    "PV | Partido Verde",
    "REDE | Rede Sustentabilidade",
    "REPUBLICANOS | Republicanos",
    "SOLIDARIEDADE | Solidariedade",
    "UNIÃO | União Brasil",
    "UP | Unidade Popular",
)

/**
 * Resposta real da tool `list_parties` (limit 50) em 07/11/2026: 38 partidos, só os campos que o app lê.
 * [extra] acrescenta partidos ao fim do `data`, para simular a API devolvendo um partido novo no futuro.
 */
internal fun listPartiesJson(vararg extra: String): String {
    val items = listOf(
        """{"id": "3f7a519d-641b-4b56-95df-dcb4cc51cad1", "number": 10, "acronym": "REPUBLICANOS", "name": "REPUBLICANOS"}""",
        """{"id": "708310c7-1f6c-4b94-bfff-0c054f8cd23c", "number": 11, "acronym": "PP", "name": "PROGRESSISTAS"}""",
        """{"id": "bd4e73ca-5d10-412d-b8e5-0c7c60be9129", "number": 12, "acronym": "PDT", "name": "PARTIDO DEMOCRÁTICO TRABALHISTA"}""",
        """{"id": "2c7ac784-e2d0-4f39-a9bc-7b304cef80ba", "number": 13, "acronym": "PT", "name": "PARTIDO DOS TRABALHADORES"}""",
        """{"id": "e4073f74-db47-4fc0-b753-76578da1b984", "number": 14, "acronym": "MISSÃO", "name": "PARTIDO MISSÃO"}""",
        """{"id": "78dc85ea-eeca-4b75-b4e9-df5dfa458e59", "number": 15, "acronym": "MDB", "name": "MOVIMENTO DEMOCRÁTICO BRASILEIRO"}""",
        """{"id": "36be010b-0aca-4e17-96ad-7e07444b5096", "number": 16, "acronym": "PSTU", "name": "PARTIDO SOCIALISTA  DOS TRABALHADORES UNIFICADO"}""",
        """{"id": "49b454c3-0aab-4c19-98c5-ca7759fd0c8f", "number": 17, "acronym": "PSL", "name": "PARTIDO SOCIAL LIBERAL"}""",
        """{"id": "8697f453-2322-44b6-9e12-ea0b20e55458", "number": 18, "acronym": "REDE", "name": "REDE SUSTENTABILIDADE"}""",
        """{"id": "518820fa-1661-4e89-ad8b-212b335a2217", "number": 19, "acronym": "PTN", "name": "PARTIDO TRABALHISTA NACIONAL"}""",
        """{"id": "afa9277d-2976-455e-a29f-c13972eba793", "number": 20, "acronym": "PODE", "name": "PODEMOS"}""",
        """{"id": "33523bb5-8417-4e69-8a44-f0830f6ac205", "number": 21, "acronym": "PCB", "name": "PARTIDO COMUNISTA BRASILEIRO"}""",
        """{"id": "3cd8d415-938d-4ec3-9380-953d393cc334", "number": 22, "acronym": "PL", "name": "PARTIDO LIBERAL"}""",
        """{"id": "f93753ed-2216-4a43-b9c8-a8e113d9c9a5", "number": 23, "acronym": "CIDADANIA", "name": "CIDADANIA"}""",
        """{"id": "99114c4e-43b2-40a2-810d-94c159f8822d", "number": 25, "acronym": "PRD", "name": "PARTIDO RENOVACAO DEMOCRATICA"}""",
        """{"id": "59886fb8-bc5d-485f-89ca-73253eca00bf", "number": 26, "acronym": "PAN", "name": "PARTIDO DOS APOSENTADOS DA NAÇÃO"}""",
        """{"id": "2f016de3-b59e-480b-bac8-6569e5964bbf", "number": 27, "acronym": "DC", "name": "DEMOCRACIA CRISTÃ"}""",
        """{"id": "33c86dbc-f744-471a-9c9c-c5789c8cb929", "number": 28, "acronym": "PRTB", "name": "PARTIDO RENOVADOR TRABALHISTA BRASILEIRO"}""",
        """{"id": "4c7c6b3a-f1ee-4d56-bc1a-05c6f27723ad", "number": 29, "acronym": "PCO", "name": "PARTIDO DA CAUSA OPERÁRIA"}""",
        """{"id": "0284074c-12c9-4075-9852-f661fefe5b10", "number": 30, "acronym": "NOVO", "name": "PARTIDO NOVO"}""",
        """{"id": "f8ee10c8-e85f-410d-9746-da273302a32d", "number": 31, "acronym": "PHS", "name": "PARTIDO HUMANISTA DA SOLIDARIEDADE"}""",
        """{"id": "320fa8c4-a3a8-442e-a071-343434b4775e", "number": 33, "acronym": "MOBILIZA", "name": "MOBILIZAÇÃO NACIONAL"}""",
        """{"id": "d40b9e69-3c17-427a-ae36-cf35149740d3", "number": 35, "acronym": "DEMOCRATA", "name": "DEMOCRATA"}""",
        """{"id": "b27fd6a1-3f0f-49e4-90fc-eba13a5abda0", "number": 36, "acronym": "AGIR", "name": "AGIR"}""",
        """{"id": "6d30dafa-5c4f-4b51-94c3-e224b6b7fc41", "number": 40, "acronym": "PSB", "name": "PARTIDO SOCIALISTA BRASILEIRO"}""",
        """{"id": "7d8418ea-cfa5-4bb8-9f5d-9bdd857e521d", "number": 43, "acronym": "PV", "name": "PARTIDO VERDE"}""",
        """{"id": "821fa0bf-01d9-4a28-9d9a-549cf9404e9f", "number": 44, "acronym": "UNIÃO", "name": "UNIÃO BRASIL"}""",
        """{"id": "679fbefd-be34-4302-b144-282bd8dedeb1", "number": 45, "acronym": "PSDB", "name": "PARTIDO DA SOCIAL DEMOCRACIA BRASILEIRA"}""",
        """{"id": "ca1a706f-58e3-4ebd-9958-007e087766a0", "number": 50, "acronym": "PSOL", "name": "PARTIDO SOCIALISMO E LIBERDADE"}""",
        """{"id": "f93319ce-6fc6-4dee-8362-4b009f2f48cb", "number": 51, "acronym": "PATRIOTA", "name": "PARTIDO ECOLÓGICO NACIONAL"}""",
        """{"id": "7e22ebe0-6d69-4098-b2ea-95cd5989ed23", "number": 54, "acronym": "PPL", "name": "PARTIDO PÁTRIA LIVRE"}""",
        """{"id": "58333878-307a-4685-af05-498e3551c5a5", "number": 55, "acronym": "PSD", "name": "PARTIDO SOCIAL DEMOCRÁTICO"}""",
        """{"id": "991791fc-a88b-460e-9fcf-448e67dc23af", "number": 56, "acronym": "PRONA", "name": "PARTIDO DE REEDIFICAÇÃO DA ORDEM NACIONAL"}""",
        """{"id": "f03e1dd1-9d68-44b6-b292-b5c9e6750671", "number": 65, "acronym": "PC do B", "name": "PARTIDO COMUNISTA DO BRASIL"}""",
        """{"id": "8f607ae4-b877-437e-9fa2-62832275f79b", "number": 70, "acronym": "AVANTE", "name": "AVANTE"}""",
        """{"id": "c3293632-81f2-48f1-82c2-3ad8892ba991", "number": 77, "acronym": "SD", "name": "SOLIDARIEDADE"}""",
        """{"id": "860b1a2d-d0c7-4ca0-9c3c-7079696fd732", "number": 80, "acronym": "UP", "name": "UNIDADE POPULAR"}""",
        """{"id": "af425e4d-d2e9-414e-bbff-8d06bfb2e046", "number": 90, "acronym": "PROS", "name": "PARTIDO REPUBLICANO DA ORDEM SOCIAL"}""",
    ) + extra
    return """{"data":[${items.joinToString(",")}],"pagination":{"page":1,"limit":50,"total":${items.size},"totalPages":1}}"""
}
