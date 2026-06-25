package com.example.chessboard.analysis

/**
 * File role: detects opening-line deviations from an in-memory opening-book index.
 * Allowed here:
 * - pure deviation queries over loaded line records or a prepared OpeningBookIndex
 * - mapping indexed line references back to deviation result records
 * Not allowed here:
 * - database access, UI state, screen workflow code, or branch presentation models
 * Validation date: 2026-06-25
 */
import com.example.chessboard.entity.LineEntity

enum class OpeningSide {
    WHITE,
    BLACK,
}

data class OpeningDeviation(
    val positionFen: String,
    val lines: List<LineEntity>,
)

class OpeningDeviationFinder(
    private val indexBuilder: OpeningBookIndexBuilder = OpeningBookIndexBuilder(),
) {

    /**
     * Finds opening-line deviations for one selected side.
     *
     * The finder treats a position as a deviation when loaded opening lines contain more than one
     * unique next move from that position for [selectedSide]. Lines that end at the position are
     * tracked by the shared book index for other analysis features, but they do not create or count
     * as deviation branches for this existing finder contract.
     */
    fun findDeviations(
        lines: List<LineEntity>,
        selectedSide: OpeningSide,
    ): List<OpeningDeviation> {
        return findDeviations(
            index = indexBuilder.build(lines),
            selectedSide = selectedSide,
        )
    }

    internal fun findDeviations(
        index: OpeningBookIndex,
        selectedSide: OpeningSide,
    ): List<OpeningDeviation> {
        return index.positions.values
            .filter { position -> position.sideToMove == selectedSide }
            .filter { position -> position.nextMoves.size > 1 }
            .sortedWith(
                compareBy(
                    { position -> firstNextMoveRef(position).lineIndex },
                    { position -> firstNextMoveRef(position).ply },
                )
            )
            .map { position ->
                OpeningDeviation(
                    positionFen = position.positionFen,
                    lines = collectDeviationLines(
                        index = index,
                        position = position,
                    ),
                )
            }
    }

    private fun firstNextMoveRef(position: OpeningBookPosition): OpeningBookLineRef {
        val firstRef = position.nextMoves
            .flatMap { move -> move.lineRefs }
            .minWithOrNull(compareBy({ ref -> ref.lineIndex }, { ref -> ref.ply }))

        if (firstRef != null) {
            return firstRef
        }

        error("Deviation position has no indexed next-move line refs")
    }

    private fun collectDeviationLines(
        index: OpeningBookIndex,
        position: OpeningBookPosition,
    ): List<LineEntity> {
        val linesByKey = linkedMapOf<LineRefKey, LineEntity>()

        position.nextMoves
            .flatMap { move -> move.lineRefs }
            .sortedWith(compareBy({ ref -> ref.lineIndex }, { ref -> ref.ply }))
            .forEach { ref ->
                linesByKey[LineRefKey.from(ref)] = index.lineFor(ref)
            }

        return linesByKey.values.toList()
    }

    private data class LineRefKey(
        val stableLineId: Long?,
        val inputIndex: Int?,
    ) {
        companion object {
            fun from(ref: OpeningBookLineRef): LineRefKey {
                return LineRefKey(
                    stableLineId = ref.stableLineId,
                    inputIndex = ref.inputIndex,
                )
            }
        }
    }
}
