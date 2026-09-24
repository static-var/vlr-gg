-- Historical unversioned SQLDelight schema at 369e2497 (user_version = 1).
CREATE TABLE events (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    subtitle TEXT NOT NULL DEFAULT '',
    status TEXT,
    prizes TEXT NOT NULL,
    dates TEXT NOT NULL,
    region TEXT,
    logo_url TEXT NOT NULL,
    last_updated INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_event_status ON events(status);

CREATE INDEX idx_event_dates ON events(dates);

CREATE INDEX idx_event_region ON events(region);

CREATE TABLE event_prizes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id TEXT NOT NULL,
    position TEXT NOT NULL,
    prize TEXT NOT NULL,
    team_id TEXT,
    team_name TEXT NOT NULL,
    team_logo_url TEXT NOT NULL,
    team_country TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_event_prizes_event_id ON event_prizes(event_id);

CREATE TABLE event_teams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id TEXT NOT NULL,
    team_id TEXT,
    team_name TEXT NOT NULL,
    team_logo_url TEXT NOT NULL,
    seed TEXT,
    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_event_teams_event_id ON event_teams(event_id);

CREATE INDEX idx_event_teams_team_id ON event_teams(team_id);

CREATE TABLE event_standings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id TEXT NOT NULL,
    team_name TEXT NOT NULL,
    team_logo_url TEXT NOT NULL,
    team_country TEXT NOT NULL,
    group_name TEXT,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    ties INTEGER NOT NULL DEFAULT 0,
    map_difference INTEGER NOT NULL DEFAULT 0,
    round_difference INTEGER NOT NULL DEFAULT 0,
    round_delta INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_event_standings_event_id ON event_standings(event_id);

CREATE INDEX idx_event_standings_group ON event_standings(event_id, group_name);

CREATE TABLE event_matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id TEXT NOT NULL,
    match_id TEXT NOT NULL,
    time TEXT NOT NULL DEFAULT '',
    date TEXT NOT NULL DEFAULT '',
    eta TEXT,
    status TEXT NOT NULL DEFAULT '',
    team1_name TEXT NOT NULL DEFAULT '',
    team1_region TEXT NOT NULL DEFAULT '',
    team1_score INTEGER,
    team2_name TEXT NOT NULL DEFAULT '',
    team2_region TEXT NOT NULL DEFAULT '',
    team2_score INTEGER,
    round TEXT NOT NULL DEFAULT '',
    stage TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE(event_id, match_id)
);

CREATE INDEX idx_event_matches_event_id ON event_matches(event_id);

CREATE INDEX idx_event_matches_match_id ON event_matches(match_id);

CREATE TABLE favorite_events (
    event_id TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE matches (
    id TEXT NOT NULL PRIMARY KEY,
    event_id TEXT,
    event_name TEXT NOT NULL,
    event_logo_url TEXT NOT NULL,
    series TEXT NOT NULL DEFAULT '',
    stage TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL,
    time TEXT NOT NULL,
    eta TEXT,
    note TEXT NOT NULL DEFAULT '',
    patch TEXT,
    team1_id TEXT NOT NULL,
    team1_name TEXT NOT NULL,
    team1_logo_url TEXT NOT NULL,
    team1_score INTEGER,
    team2_id TEXT NOT NULL,
    team2_name TEXT NOT NULL,
    team2_logo_url TEXT NOT NULL,
    team2_score INTEGER,
    map_count INTEGER NOT NULL DEFAULT 0,
    last_updated INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_match_event_id ON matches(event_id);

CREATE INDEX idx_match_status_time ON matches(status, time);

CREATE INDEX idx_match_team1_time ON matches(team1_id, time);

CREATE INDEX idx_match_team2_time ON matches(team2_id, time);

CREATE TABLE match_maps (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id TEXT NOT NULL,
    map_name TEXT NOT NULL,
    team1_score INTEGER,
    team2_score INTEGER,
    duration TEXT,
    stats_url TEXT,
    FOREIGN KEY(match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_match_maps_match_id ON match_maps(match_id);

CREATE TABLE match_map_rounds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id TEXT NOT NULL,
    map_name TEXT NOT NULL,
    round_number INTEGER NOT NULL,
    round_score TEXT NOT NULL,
    winner TEXT NOT NULL,
    side TEXT NOT NULL,
    win_type TEXT NOT NULL,
    FOREIGN KEY(match_id) REFERENCES matches(id) ON DELETE CASCADE,
    UNIQUE(match_id, map_name, round_number)
);

CREATE INDEX idx_match_rounds_match_map ON match_map_rounds(match_id, map_name);

CREATE TABLE match_map_player_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id TEXT NOT NULL,
    map_name TEXT NOT NULL,
    player_id TEXT NOT NULL,
    player_name TEXT NOT NULL,
    team_id TEXT NOT NULL,
    agent_name TEXT NOT NULL,
    agent_image_url TEXT NOT NULL DEFAULT '',
    rating REAL NOT NULL DEFAULT 0.0,
    acs INTEGER NOT NULL DEFAULT 0,
    kills INTEGER NOT NULL DEFAULT 0,
    deaths INTEGER NOT NULL DEFAULT 0,
    assists INTEGER NOT NULL DEFAULT 0,
    kast_percent REAL,
    adr REAL,
    hs_percent REAL,
    first_kills INTEGER NOT NULL DEFAULT 0,
    first_deaths INTEGER NOT NULL DEFAULT 0,
    first_kills_diff INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_mmps_match_id ON match_map_player_stats(match_id);

CREATE INDEX idx_mmps_player_id ON match_map_player_stats(player_id);

CREATE INDEX idx_mmps_map ON match_map_player_stats(match_id, map_name);

CREATE TABLE match_bans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id TEXT NOT NULL,
    ban_type TEXT NOT NULL,
    ban_value TEXT NOT NULL,
    FOREIGN KEY(match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_match_bans_match_id ON match_bans(match_id);

CREATE TABLE match_videos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id TEXT NOT NULL,
    video_type TEXT NOT NULL,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    FOREIGN KEY(match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_match_videos_match_id ON match_videos(match_id);

CREATE INDEX idx_match_videos_type ON match_videos(match_id, video_type);

CREATE TABLE match_previous_encounters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id TEXT NOT NULL,
    previous_match_id TEXT NOT NULL,
    team1_name TEXT NOT NULL,
    team1_score INTEGER,
    team2_name TEXT NOT NULL,
    team2_score INTEGER,
    FOREIGN KEY(match_id) REFERENCES matches(id) ON DELETE CASCADE,
    FOREIGN KEY(previous_match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_match_prev_encounters_match_id ON match_previous_encounters(match_id);

CREATE TABLE favorite_matches (
    match_id TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE sync_metadata (
    entity_type TEXT NOT NULL PRIMARY KEY,
    last_sync_timestamp INTEGER NOT NULL,
    version_hash TEXT,
    record_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE news (
    id TEXT NOT NULL PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    date TEXT NOT NULL,
    cover_url TEXT NOT NULL,
    description TEXT,
    content_html TEXT,
    last_updated INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_news_date ON news(date);

CREATE INDEX idx_news_author ON news(author);

CREATE TABLE news_media (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    news_id TEXT NOT NULL,
    media_type TEXT NOT NULL,
    media_value TEXT NOT NULL,
    media_text TEXT,
    FOREIGN KEY(news_id) REFERENCES news(id) ON DELETE CASCADE
);

CREATE INDEX idx_news_media_news_id ON news_media(news_id);

CREATE INDEX idx_news_media_type ON news_media(news_id, media_type);

CREATE TABLE players (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    alias TEXT NOT NULL DEFAULT '',
    real_name TEXT,
    country TEXT NOT NULL,
    current_team_id TEXT,
    image_url TEXT,
    twitter_url TEXT,
    twitch_url TEXT,
    total_winnings REAL NOT NULL DEFAULT 0.0,
    last_updated INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_player_country ON players(country);

CREATE INDEX idx_player_current_team ON players(current_team_id);

CREATE TABLE player_agent_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id TEXT NOT NULL,
    agent_name TEXT NOT NULL,
    agent_image_url TEXT NOT NULL DEFAULT '',
    usage_count INTEGER NOT NULL DEFAULT 0,
    usage_percent REAL NOT NULL DEFAULT 0.0,
    rounds_played INTEGER NOT NULL DEFAULT 0,
    rating REAL NOT NULL DEFAULT 0.0,
    acs REAL NOT NULL DEFAULT 0.0,
    kd_ratio REAL NOT NULL DEFAULT 0.0,
    adr REAL NOT NULL DEFAULT 0.0,
    kast REAL NOT NULL DEFAULT 0.0,
    kpr REAL NOT NULL DEFAULT 0.0,
    apr REAL NOT NULL DEFAULT 0.0,
    fkpr REAL NOT NULL DEFAULT 0.0,
    fdpr REAL NOT NULL DEFAULT 0.0,
    kills INTEGER NOT NULL DEFAULT 0,
    deaths INTEGER NOT NULL DEFAULT 0,
    assists INTEGER NOT NULL DEFAULT 0,
    first_kills INTEGER NOT NULL DEFAULT 0,
    first_deaths INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
    UNIQUE(player_id, agent_name)
);

CREATE INDEX idx_player_agent_stats_player_id ON player_agent_stats(player_id);

CREATE INDEX idx_player_agent_stats_usage ON player_agent_stats(player_id, usage_count);

CREATE TABLE player_team_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id TEXT NOT NULL,
    team_id TEXT,
    team_name TEXT NOT NULL,
    team_logo_url TEXT NOT NULL DEFAULT '',
    is_current INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
);

CREATE INDEX idx_player_team_history_player_id ON player_team_history(player_id);

CREATE INDEX idx_player_team_history_current ON player_team_history(player_id, is_current);

CREATE TABLE favorite_players (
    player_id TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE rankings (
    team_id TEXT NOT NULL,
    region TEXT NOT NULL,
    team_name TEXT NOT NULL DEFAULT '',
    team_logo TEXT NOT NULL DEFAULT '',
    country TEXT NOT NULL DEFAULT '',
    rank INTEGER NOT NULL,
    points TEXT NOT NULL,
    last_updated INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (team_id, region)
);

CREATE INDEX idx_rankings_region ON rankings(region);

CREATE INDEX idx_rankings_rank ON rankings(region, rank);

CREATE TABLE standings (
    team_id TEXT NOT NULL,
    year INTEGER NOT NULL,
    circuit TEXT NOT NULL,
    region TEXT NOT NULL,
    team_name TEXT NOT NULL DEFAULT '',
    team_logo TEXT NOT NULL DEFAULT '',
    country TEXT NOT NULL DEFAULT '',
    rank INTEGER NOT NULL,
    points TEXT NOT NULL,
    last_updated INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (team_id, year, circuit)
);

CREATE INDEX idx_standings_year_region ON standings(year, region);

CREATE INDEX idx_standings_circuit ON standings(circuit);

CREATE TABLE search_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    tags TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_search_entity_type ON search_index(entity_type);

CREATE INDEX idx_search_entity_id ON search_index(entity_type, entity_id);

CREATE INDEX idx_search_name ON search_index(name);

CREATE TABLE teams (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    tag TEXT NOT NULL DEFAULT '',
    logo_url TEXT NOT NULL,
    region TEXT,
    country TEXT NOT NULL,
    roster_url TEXT,
    earnings TEXT,
    rank INTEGER NOT NULL DEFAULT 0,
    website TEXT,
    twitter TEXT,
    last_updated INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_team_region ON teams(region);

CREATE INDEX idx_team_country ON teams(country);

CREATE INDEX idx_team_rank ON teams(rank);

CREATE TABLE team_roster (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    player_name TEXT NOT NULL,
    player_alias TEXT NOT NULL DEFAULT '',
    player_image_url TEXT NOT NULL DEFAULT '',
    player_country TEXT NOT NULL DEFAULT '',
    is_stand_in INTEGER NOT NULL DEFAULT 0,
    is_coach INTEGER NOT NULL DEFAULT 0,
    is_current INTEGER NOT NULL DEFAULT 1,
    role TEXT,
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE,
    UNIQUE(team_id, player_id)
);

CREATE INDEX idx_team_roster_team_id ON team_roster(team_id);

CREATE INDEX idx_team_roster_player_id ON team_roster(player_id);

CREATE TABLE team_upcoming_matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team_id TEXT NOT NULL,
    match_id TEXT NOT NULL,
    opponent_team_id TEXT,
    opponent_team_name TEXT NOT NULL,
    opponent_team_logo_url TEXT NOT NULL DEFAULT '',
    date TEXT NOT NULL,
    eta TEXT,
    event_name TEXT NOT NULL,
    event_logo_url TEXT NOT NULL,
    event_id TEXT,
    stage TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE,
    UNIQUE(team_id, match_id)
);

CREATE INDEX idx_team_upcoming_team_id ON team_upcoming_matches(team_id);

CREATE INDEX idx_team_upcoming_date ON team_upcoming_matches(team_id, date);

CREATE TABLE team_completed_matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team_id TEXT NOT NULL,
    match_id TEXT NOT NULL,
    opponent_team_id TEXT,
    opponent_team_name TEXT NOT NULL,
    opponent_team_logo_url TEXT NOT NULL DEFAULT '',
    date TEXT NOT NULL,
    event_name TEXT NOT NULL,
    event_logo_url TEXT NOT NULL,
    event_id TEXT,
    stage TEXT NOT NULL DEFAULT '',
    result TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE,
    UNIQUE(team_id, match_id)
);

CREATE INDEX idx_team_completed_team_id ON team_completed_matches(team_id);

CREATE INDEX idx_team_completed_date ON team_completed_matches(team_id, date);

CREATE TABLE favorite_teams (
    team_id TEXT NOT NULL PRIMARY KEY
);
