package com.example.chessboard.ui.screen

// Holds stable profile localization ids and Android string resource mappings.
// Keep profile text identifiers here; keep UI layout and persistence logic out of this file.
// Validated 2026-06-05.

import androidx.annotation.StringRes
import com.example.chessboard.R

enum class PlayerTier(
    val symbol: String,
    val titleIds: List<ProfileRankTitleId>,
) {
    Pawn(
        symbol = "♟",
        titleIds = listOf(
            ProfileRankTitleId.WalkingBlunder,
            ProfileRankTitleId.FreeMaterial,
            ProfileRankTitleId.PawnWithDreams,
            ProfileRankTitleId.RageQuitter,
            ProfileRankTitleId.CenterFeeder,
            ProfileRankTitleId.OneMoveGenius,
            ProfileRankTitleId.HopeChessBeginner,
            ProfileRankTitleId.SacrificeWithoutReason,
            ProfileRankTitleId.StillLearningRules,
        ),
    ),
    Knight(
        symbol = "♞",
        titleIds = listOf(
            ProfileRankTitleId.ForkVictim,
            ProfileRankTitleId.LMoveConfusion,
            ProfileRankTitleId.RandomJumper,
            ProfileRankTitleId.ThatWasCalculated,
            ProfileRankTitleId.HopeChessPlayer,
            ProfileRankTitleId.MissedTheFork,
            ProfileRankTitleId.TacticalTourist,
            ProfileRankTitleId.BlunderEnthusiast,
            ProfileRankTitleId.ChaosEnjoyer,
            ProfileRankTitleId.StillNotSeeingIt,
        ),
    ),
    Bishop(
        symbol = "♝",
        titleIds = listOf(
            ProfileRankTitleId.BadBishopOwner,
            ProfileRankTitleId.DiagonalPretender,
            ProfileRankTitleId.LongRangeMiss,
            ProfileRankTitleId.DidntSeeThat,
            ProfileRankTitleId.StillHangingPieces,
            ProfileRankTitleId.PassiveObserver,
            ProfileRankTitleId.FakeStrategist,
            ProfileRankTitleId.TrappedBishopClub,
        ),
    ),
    Rook(
        symbol = "♜",
        titleIds = listOf(
            ProfileRankTitleId.OpenFileTourist,
            ProfileRankTitleId.RookHanger,
            ProfileRankTitleId.SacrificeTheRook,
            ProfileRankTitleId.IHadThisWon,
            ProfileRankTitleId.BackRankVictim,
            ProfileRankTitleId.LateLineBlunderer,
            ProfileRankTitleId.PanicDefender,
            ProfileRankTitleId.SomehowWinning,
        ),
    ),
    Queen(
        symbol = "♛",
        titleIds = listOf(
            ProfileRankTitleId.QueenBlunderSpecialist,
            ProfileRankTitleId.ItWasASacrifice,
            ProfileRankTitleId.TiltManager,
            ProfileRankTitleId.CalculationOptional,
            ProfileRankTitleId.BotezGambit,
            ProfileRankTitleId.OverconfidentPlayer,
            ProfileRankTitleId.AttackWithoutPlan,
            ProfileRankTitleId.FakeTactician,
            ProfileRankTitleId.BlunderRecoveryExpert,
            ProfileRankTitleId.ChaosCreator,
        ),
    ),
    King(
        symbol = "♚",
        titleIds = listOf(
            ProfileRankTitleId.BlundersMateInOne,
            ProfileRankTitleId.SelfCheckArtist,
            ProfileRankTitleId.IDidntSeeThat,
            ProfileRankTitleId.StillNotGm,
            ProfileRankTitleId.KingInDanger,
            ProfileRankTitleId.PanicModeActivated,
            ProfileRankTitleId.LastHopeDefender,
            ProfileRankTitleId.BarelySurviving,
            ProfileRankTitleId.LuckySurvivor,
        ),
    ),
}

enum class ProfileRankTitleId(val storageKey: String, val legacyText: String) {
    WalkingBlunder("walking_blunder", "Walking Blunder"),
    FreeMaterial("free_material", "Free Material"),
    PawnWithDreams("pawn_with_dreams", "Pawn With Dreams"),
    RageQuitter("rage_quitter", "Rage Quitter"),
    CenterFeeder("center_feeder", "Center Feeder"),
    OneMoveGenius("one_move_genius", "One-Move Genius"),
    HopeChessBeginner("hope_chess_beginner", "Hope Chess Beginner"),
    SacrificeWithoutReason("sacrifice_without_reason", "Sacrifice Without Reason"),
    StillLearningRules("still_learning_rules", "Still Learning Rules"),
    ForkVictim("fork_victim", "Fork Victim"),
    LMoveConfusion("l_move_confusion", "L-Move Confusion"),
    RandomJumper("random_jumper", "Random Jumper"),
    ThatWasCalculated("that_was_calculated", "\"That was calculated\""),
    HopeChessPlayer("hope_chess_player", "Hope Chess Player"),
    MissedTheFork("missed_the_fork", "Missed the Fork"),
    TacticalTourist("tactical_tourist", "Tactical Tourist"),
    BlunderEnthusiast("blunder_enthusiast", "Blunder Enthusiast"),
    ChaosEnjoyer("chaos_enjoyer", "Chaos Enjoyer"),
    StillNotSeeingIt("still_not_seeing_it", "Still Not Seeing It"),
    BadBishopOwner("bad_bishop_owner", "Bad Bishop Owner"),
    DiagonalPretender("diagonal_pretender", "Diagonal Pretender"),
    LongRangeMiss("long_range_miss", "Long-Range Miss"),
    DidntSeeThat("didnt_see_that", "\"Didn't see that\""),
    StillHangingPieces("still_hanging_pieces", "Still Hanging Pieces"),
    PassiveObserver("passive_observer", "Passive Observer"),
    FakeStrategist("fake_strategist", "Fake Strategist"),
    TrappedBishopClub("trapped_bishop_club", "Trapped Bishop Club"),
    OpenFileTourist("open_file_tourist", "Open File Tourist"),
    RookHanger("rook_hanger", "Rook Hanger"),
    SacrificeTheRook("sacrifice_the_rook", "Sacrifice the ROOOOOOOK"),
    IHadThisWon("i_had_this_won", "\"I Had This Won\""),
    BackRankVictim("back_rank_victim", "Back Rank Victim"),
    LateLineBlunderer("late_line_blunderer", "Late Line Blunderer"),
    PanicDefender("panic_defender", "Panic Defender"),
    SomehowWinning("somehow_winning", "Somehow Winning"),
    QueenBlunderSpecialist("queen_blunder_specialist", "Queen Blunder Specialist"),
    ItWasASacrifice("it_was_a_sacrifice", "\"It Was a Sacrifice\""),
    TiltManager("tilt_manager", "Tilt Manager"),
    CalculationOptional("calculation_optional", "Calculation Optional"),
    BotezGambit("botez_gambit", "Botez Gambit"),
    OverconfidentPlayer("overconfident_player", "Overconfident Player"),
    AttackWithoutPlan("attack_without_plan", "Attack Without Plan"),
    FakeTactician("fake_tactician", "Fake Tactician"),
    BlunderRecoveryExpert("blunder_recovery_expert", "Blunder Recovery Expert"),
    ChaosCreator("chaos_creator", "Chaos Creator"),
    BlundersMateInOne("blunders_mate_in_one", "Blunders Mate in 1"),
    SelfCheckArtist("self_check_artist", "Self-Check Artist"),
    IDidntSeeThat("i_didnt_see_that", "\"I Didn't See That\""),
    StillNotGm("still_not_gm", "Still Not GM"),
    KingInDanger("king_in_danger", "King in Danger"),
    PanicModeActivated("panic_mode_activated", "Panic Mode Activated"),
    LastHopeDefender("last_hope_defender", "Last Hope Defender"),
    BarelySurviving("barely_surviving", "Barely Surviving"),
    LuckySurvivor("lucky_survivor", "Lucky Survivor"),
}

enum class ProfileAchievementId {
    FirstSteps,
    Dedication,
    OpeningMaster,
    StreakKing,
}

internal object ProfileLocalization {
    fun rankTitleIdFromStorage(storageValue: String): ProfileRankTitleId? {
        if (storageValue.isBlank()) {
            return null
        }

        return ProfileRankTitleId.entries.firstOrNull { titleId ->
            titleId.storageKey == storageValue || titleId.legacyText == storageValue
        }
    }

    @StringRes
    fun tierLabelResId(tier: PlayerTier): Int {
        return when (tier) {
            PlayerTier.Pawn -> R.string.profile_tier_pawn
            PlayerTier.Knight -> R.string.profile_tier_knight
            PlayerTier.Bishop -> R.string.profile_tier_bishop
            PlayerTier.Rook -> R.string.profile_tier_rook
            PlayerTier.Queen -> R.string.profile_tier_queen
            PlayerTier.King -> R.string.profile_tier_king
        }
    }

    @StringRes
    fun rankTitleResId(titleId: ProfileRankTitleId): Int {
        return when (titleId) {
            ProfileRankTitleId.WalkingBlunder -> R.string.profile_rank_title_walking_blunder
            ProfileRankTitleId.FreeMaterial -> R.string.profile_rank_title_free_material
            ProfileRankTitleId.PawnWithDreams -> R.string.profile_rank_title_pawn_with_dreams
            ProfileRankTitleId.RageQuitter -> R.string.profile_rank_title_rage_quitter
            ProfileRankTitleId.CenterFeeder -> R.string.profile_rank_title_center_feeder
            ProfileRankTitleId.OneMoveGenius -> R.string.profile_rank_title_one_move_genius
            ProfileRankTitleId.HopeChessBeginner -> R.string.profile_rank_title_hope_chess_beginner
            ProfileRankTitleId.SacrificeWithoutReason -> R.string.profile_rank_title_sacrifice_without_reason
            ProfileRankTitleId.StillLearningRules -> R.string.profile_rank_title_still_learning_rules
            ProfileRankTitleId.ForkVictim -> R.string.profile_rank_title_fork_victim
            ProfileRankTitleId.LMoveConfusion -> R.string.profile_rank_title_l_move_confusion
            ProfileRankTitleId.RandomJumper -> R.string.profile_rank_title_random_jumper
            ProfileRankTitleId.ThatWasCalculated -> R.string.profile_rank_title_that_was_calculated
            ProfileRankTitleId.HopeChessPlayer -> R.string.profile_rank_title_hope_chess_player
            ProfileRankTitleId.MissedTheFork -> R.string.profile_rank_title_missed_the_fork
            ProfileRankTitleId.TacticalTourist -> R.string.profile_rank_title_tactical_tourist
            ProfileRankTitleId.BlunderEnthusiast -> R.string.profile_rank_title_blunder_enthusiast
            ProfileRankTitleId.ChaosEnjoyer -> R.string.profile_rank_title_chaos_enjoyer
            ProfileRankTitleId.StillNotSeeingIt -> R.string.profile_rank_title_still_not_seeing_it
            ProfileRankTitleId.BadBishopOwner -> R.string.profile_rank_title_bad_bishop_owner
            ProfileRankTitleId.DiagonalPretender -> R.string.profile_rank_title_diagonal_pretender
            ProfileRankTitleId.LongRangeMiss -> R.string.profile_rank_title_long_range_miss
            ProfileRankTitleId.DidntSeeThat -> R.string.profile_rank_title_didnt_see_that
            ProfileRankTitleId.StillHangingPieces -> R.string.profile_rank_title_still_hanging_pieces
            ProfileRankTitleId.PassiveObserver -> R.string.profile_rank_title_passive_observer
            ProfileRankTitleId.FakeStrategist -> R.string.profile_rank_title_fake_strategist
            ProfileRankTitleId.TrappedBishopClub -> R.string.profile_rank_title_trapped_bishop_club
            ProfileRankTitleId.OpenFileTourist -> R.string.profile_rank_title_open_file_tourist
            ProfileRankTitleId.RookHanger -> R.string.profile_rank_title_rook_hanger
            ProfileRankTitleId.SacrificeTheRook -> R.string.profile_rank_title_sacrifice_the_rook
            ProfileRankTitleId.IHadThisWon -> R.string.profile_rank_title_i_had_this_won
            ProfileRankTitleId.BackRankVictim -> R.string.profile_rank_title_back_rank_victim
            ProfileRankTitleId.LateLineBlunderer -> R.string.profile_rank_title_late_line_blunderer
            ProfileRankTitleId.PanicDefender -> R.string.profile_rank_title_panic_defender
            ProfileRankTitleId.SomehowWinning -> R.string.profile_rank_title_somehow_winning
            ProfileRankTitleId.QueenBlunderSpecialist -> R.string.profile_rank_title_queen_blunder_specialist
            ProfileRankTitleId.ItWasASacrifice -> R.string.profile_rank_title_it_was_a_sacrifice
            ProfileRankTitleId.TiltManager -> R.string.profile_rank_title_tilt_manager
            ProfileRankTitleId.CalculationOptional -> R.string.profile_rank_title_calculation_optional
            ProfileRankTitleId.BotezGambit -> R.string.profile_rank_title_botez_gambit
            ProfileRankTitleId.OverconfidentPlayer -> R.string.profile_rank_title_overconfident_player
            ProfileRankTitleId.AttackWithoutPlan -> R.string.profile_rank_title_attack_without_plan
            ProfileRankTitleId.FakeTactician -> R.string.profile_rank_title_fake_tactician
            ProfileRankTitleId.BlunderRecoveryExpert -> R.string.profile_rank_title_blunder_recovery_expert
            ProfileRankTitleId.ChaosCreator -> R.string.profile_rank_title_chaos_creator
            ProfileRankTitleId.BlundersMateInOne -> R.string.profile_rank_title_blunders_mate_in_one
            ProfileRankTitleId.SelfCheckArtist -> R.string.profile_rank_title_self_check_artist
            ProfileRankTitleId.IDidntSeeThat -> R.string.profile_rank_title_i_didnt_see_that
            ProfileRankTitleId.StillNotGm -> R.string.profile_rank_title_still_not_gm
            ProfileRankTitleId.KingInDanger -> R.string.profile_rank_title_king_in_danger
            ProfileRankTitleId.PanicModeActivated -> R.string.profile_rank_title_panic_mode_activated
            ProfileRankTitleId.LastHopeDefender -> R.string.profile_rank_title_last_hope_defender
            ProfileRankTitleId.BarelySurviving -> R.string.profile_rank_title_barely_surviving
            ProfileRankTitleId.LuckySurvivor -> R.string.profile_rank_title_lucky_survivor
        }
    }

    @StringRes
    fun achievementTitleResId(achievementId: ProfileAchievementId): Int {
        return when (achievementId) {
            ProfileAchievementId.FirstSteps -> R.string.profile_achievement_first_steps_title
            ProfileAchievementId.Dedication -> R.string.profile_achievement_dedication_title
            ProfileAchievementId.OpeningMaster -> R.string.profile_achievement_opening_master_title
            ProfileAchievementId.StreakKing -> R.string.profile_achievement_streak_king_title
        }
    }

    @StringRes
    fun achievementDescriptionResId(achievementId: ProfileAchievementId): Int {
        return when (achievementId) {
            ProfileAchievementId.FirstSteps -> R.string.profile_achievement_first_steps_description
            ProfileAchievementId.Dedication -> R.string.profile_achievement_dedication_description
            ProfileAchievementId.OpeningMaster -> R.string.profile_achievement_opening_master_description
            ProfileAchievementId.StreakKing -> R.string.profile_achievement_streak_king_description
        }
    }
}
