/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data

// Centralized typealiases for SQLDelight generated tables with underscore names or to provide
// a single import surface for data layer mappers. Expand as new entities are mapped.

// Simple tables
typealias News = dev.staticvar.vlr.localsource.database.News
typealias Rankings = dev.staticvar.vlr.localsource.database.Rankings
typealias Standings = dev.staticvar.vlr.localsource.database.Standings
typealias Events = dev.staticvar.vlr.localsource.database.Events
typealias Players = dev.staticvar.vlr.localsource.database.Players
typealias Teams = dev.staticvar.vlr.localsource.database.Teams

// Matches (complex) - will be used in manual mappers
typealias Matches = dev.staticvar.vlr.localsource.database.Matches
typealias MatchMaps = dev.staticvar.vlr.localsource.database.Match_maps
typealias MatchMapRounds = dev.staticvar.vlr.localsource.database.Match_map_rounds
typealias MatchMapPlayerStats = dev.staticvar.vlr.localsource.database.Match_map_player_stats
typealias MatchBans = dev.staticvar.vlr.localsource.database.Match_bans
typealias MatchVideos = dev.staticvar.vlr.localsource.database.Match_videos
typealias MatchPreviousEncounters = dev.staticvar.vlr.localsource.database.Match_previous_encounters

// Events
typealias EventPrizes = dev.staticvar.vlr.localsource.database.Event_prizes
typealias EventTeams = dev.staticvar.vlr.localsource.database.Event_teams
typealias EventStandings = dev.staticvar.vlr.localsource.database.Event_standings
typealias EventMatches = dev.staticvar.vlr.localsource.database.Event_matches

// Teams
typealias TeamRoster = dev.staticvar.vlr.localsource.database.Team_roster
typealias TeamUpcomingMatches = dev.staticvar.vlr.localsource.database.Team_upcoming_matches
typealias TeamCompletedMatches = dev.staticvar.vlr.localsource.database.Team_completed_matches

// News child
typealias NewsMedia = dev.staticvar.vlr.localsource.database.News_media

// Player child tables
typealias PlayerAgentStats = dev.staticvar.vlr.localsource.database.Player_agent_stats
typealias PlayerTeamHistory = dev.staticvar.vlr.localsource.database.Player_team_history

// Placeholder for future child tables (uncomment when schema provides them)
// typealias MatchMaps = dev.staticvar.vlr.localsource.database.Match_maps
// typealias EventPrizes = dev.staticvar.vlr.localsource.database.Event_prizes
// (All active child table aliases declared above)
// typealias PlayerTeamHistory = dev.staticvar.vlr.localsource.database.Player_team_history
