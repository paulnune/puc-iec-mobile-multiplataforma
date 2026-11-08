package com.puciec.eleicoes.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Estrela vazada (star_border do Material). O `Icons.Outlined.Star` do material-icons-core desenha a
 * estrela cheia, então "favoritado" e "não favoritado" ficavam iguais (#11). Desenhada aqui para não
 * trazer o material-icons-extended inteiro por um ícone.
 */
val StarBorder: ImageVector by lazy {
    ImageVector.Builder(name = "StarBorder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = addPathNodes(
                "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24z" +
                    "M12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z",
            ),
            fill = SolidColor(Color.Black),
        )
        .build()
}
