package com.bdcalorietracker.domain

import com.bdcalorietracker.data.local.FoodEntity
import kotlin.math.max
import kotlin.math.min

data class FuzzyMatchResult(
    val matchedFood: FoodEntity?,
    val score: Double,
    val isApproximate: Boolean
)

object FuzzyMatcher {

    fun findBestMatch(
        queryEn: String,
        queryBn: String,
        catalog: List<FoodEntity>,
        threshold: Double = 0.58
    ): FuzzyMatchResult {
        if (catalog.isEmpty()) return FuzzyMatchResult(null, 0.0, true)

        var bestFood: FoodEntity? = null
        var bestScore = 0.0

        val cleanEn = normalizeEn(queryEn)
        val cleanBn = queryBn.trim()

        for (food in catalog) {
            val foodEn = normalizeEn(food.name_en)
            val foodBn = food.name_bn.trim()

            val enSim = max(
                normalizedLevenshtein(cleanEn, foodEn),
                tokenOverlapScore(cleanEn, foodEn)
            )
            val bnSim = if (cleanBn.isNotEmpty() && foodBn.isNotEmpty()) {
                max(
                    normalizedLevenshtein(cleanBn, foodBn),
                    tokenOverlapScore(cleanBn, foodBn)
                )
            } else 0.0

            val combinedScore = max(enSim, bnSim)
            if (combinedScore > bestScore) {
                bestScore = combinedScore
                bestFood = food
            }
        }

        return if (bestScore >= threshold && bestFood != null) {
            FuzzyMatchResult(bestFood, bestScore, isApproximate = bestScore < 0.75)
        } else {
            FuzzyMatchResult(null, bestScore, isApproximate = true)
        }
    }

    private fun normalizeEn(input: String): String =
        input.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun tokenOverlapScore(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a.contains(b) || b.contains(a)) return 0.85
        val tokensA = a.split(" ").filter { it.length > 2 }.toSet()
        val tokensB = b.split(" ").filter { it.length > 2 }.toSet()
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0
        val intersection = tokensA.intersect(tokensB).size.toDouble()
        return intersection / min(tokensA.size, tokensB.size).toDouble()
    }

    private fun normalizedLevenshtein(s1: String, s2: String): Double {
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(s1, s2)
        return 1.0 - (dist.toDouble() / maxLen.toDouble())
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
