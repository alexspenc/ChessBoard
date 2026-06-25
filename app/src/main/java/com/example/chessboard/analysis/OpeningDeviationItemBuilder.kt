package com.example.chessboard.analysis

/**
 * File role: builds UI-facing opening deviation items from indexed deviation results.
 * Allowed here:
 * - mapping pure deviation positions into lightweight presentation data
 * - aggregating indexed next moves into branch display records
 * Not allowed here:
 * - database queries, Compose rendering, screen navigation, or board replay loops
 * Validation date: 2026-06-25
 */
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationBranch
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationItem

class OpeningDeviationItemBuilder(
    private val finder: OpeningDeviationFinder = OpeningDeviationFinder(),
    private val indexBuilder: OpeningBookIndexBuilder = OpeningBookIndexBuilder(),
) {

    fun build(
        lines: List<LineEntity>,
        selectedSide: OpeningSide,
    ): List<OpeningDeviationItem> {
        val index = indexBuilder.build(lines)

        return finder.findDeviations(
            index = index,
            selectedSide = selectedSide,
        ).map { deviation ->
            OpeningDeviationItem(
                positionFen = deviation.positionFen,
                branches = buildBranches(
                    index = index,
                    deviation = deviation,
                ),
            )
        }
    }

    private fun buildBranches(
        index: OpeningBookIndex,
        deviation: OpeningDeviation,
    ): List<OpeningDeviationBranch> {
        val position = index.positions[deviation.positionFen] ?: return emptyList()
        val branchesByResultFen = linkedMapOf<String, BranchBucket>()

        position.nextMoves.forEach { move ->
            recordBranch(
                moveUci = move.moveUci,
                resultFen = move.resultFen,
                linesCount = move.lineRefs.size,
                branchesByResultFen = branchesByResultFen,
            )
        }

        return branchesByResultFen.values.map { bucket ->
            OpeningDeviationBranch(
                moveUci = bucket.moveUci,
                resultFen = bucket.resultFen,
                linesCount = bucket.linesCount,
            )
        }
    }

    private fun recordBranch(
        moveUci: String,
        resultFen: String,
        linesCount: Int,
        branchesByResultFen: MutableMap<String, BranchBucket>,
    ) {
        val existingBucket = branchesByResultFen[resultFen]
        if (existingBucket != null) {
            existingBucket.linesCount += linesCount
            return
        }

        branchesByResultFen[resultFen] = BranchBucket(
            moveUci = moveUci,
            resultFen = resultFen,
            linesCount = linesCount,
        )
    }

    private data class BranchBucket(
        val moveUci: String,
        val resultFen: String,
        var linesCount: Int,
    )
}
