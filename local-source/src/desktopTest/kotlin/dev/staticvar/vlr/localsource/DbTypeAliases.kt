/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

// Type aliases to provide Kotlin-style names for underscored SQLDelight generated classes
// Avoids invasive schema renames and migrations.

// Matches module
typealias MatchMaps = dev.staticvar.vlr.localsource.database.Match_maps
typealias MatchMapRounds = dev.staticvar.vlr.localsource.database.Match_map_rounds
typealias MatchMapPlayerStats = dev.staticvar.vlr.localsource.database.Match_map_player_stats
typealias MatchBans = dev.staticvar.vlr.localsource.database.Match_bans
typealias MatchVideos = dev.staticvar.vlr.localsource.database.Match_videos
typealias MatchPreviousEncounters = dev.staticvar.vlr.localsource.database.Match_previous_encounters
typealias FavoriteMatches = dev.staticvar.vlr.localsource.database.Favorite_matches

// Events module
typealias EventPrizes = dev.staticvar.vlr.localsource.database.Event_prizes
typealias EventTeams = dev.staticvar.vlr.localsource.database.Event_teams
typealias EventStandings = dev.staticvar.vlr.localsource.database.Event_standings
typealias EventMatches = dev.staticvar.vlr.localsource.database.Event_matches
typealias FavoriteEvents = dev.staticvar.vlr.localsource.database.Favorite_events

// Players module
typealias PlayerAgentStats = dev.staticvar.vlr.localsource.database.Player_agent_stats
typealias PlayerTeamHistory = dev.staticvar.vlr.localsource.database.Player_team_history
typealias FavoritePlayers = dev.staticvar.vlr.localsource.database.Favorite_players

// News module
typealias NewsMedia = dev.staticvar.vlr.localsource.database.News_media

// Rankings (already fine: Rankings, Standings)

// Metadata
typealias SyncMetadata = dev.staticvar.vlr.localsource.database.Sync_metadata

// Search (FTS5)
typealias SearchIndex = dev.staticvar.vlr.localsource.database.Search_index

// Teams module
typealias TeamRoster = dev.staticvar.vlr.localsource.database.Team_roster
typealias TeamUpcomingMatches = dev.staticvar.vlr.localsource.database.Team_upcoming_matches
typealias TeamCompletedMatches = dev.staticvar.vlr.localsource.database.Team_completed_matches
typealias FavoriteTeams = dev.staticvar.vlr.localsource.database.Favorite_teams
