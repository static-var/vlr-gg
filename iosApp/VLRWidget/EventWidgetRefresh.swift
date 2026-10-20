import Foundation

private struct EventResponse: Decodable {
    let id: String
    let title: String
    let dates: String
    let prize: String?
    let location: String
    let status: String
    let img: String?
    let teams: [Team]
    let matches: [Match]
    let standings: [Standing]
    let prizes: [Prize]

    enum CodingKeys: String, CodingKey { case id, title, dates, prize, location, status, img, teams, matches, standings, prizes }
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        id = try values.decode(String.self, forKey: .id)
        title = try values.decode(String.self, forKey: .title)
        dates = values.optional(String.self, .dates) ?? ""
        prize = values.optional(String.self, .prize)
        location = values.optional(String.self, .location) ?? ""
        status = values.optional(String.self, .status) ?? ""
        img = values.optional(String.self, .img)
        teams = values.optional([Team].self, .teams) ?? []
        matches = values.optional([Match].self, .matches) ?? []
        standings = values.optional([Standing].self, .standings) ?? []
        prizes = values.optional([Prize].self, .prizes) ?? []
    }

    struct Team: Decodable {
        let id: String?
        let name: String
        let img: String?
        enum CodingKeys: String, CodingKey { case id, name, img }
        init(from decoder: Decoder) throws {
            let values = try decoder.container(keyedBy: CodingKeys.self)
            id = values.optional(String.self, .id)
            name = values.optional(String.self, .name) ?? ""
            img = values.optional(String.self, .img)
        }
    }
    struct Match: Decodable {
        let id: String
        let date: String
        let time: String
        let status: String
        let teams: [Participant]
        let round: String
        let stage: String
        enum CodingKeys: String, CodingKey { case id, date, time, status, teams, round, stage }
        init(from decoder: Decoder) throws {
            let values = try decoder.container(keyedBy: CodingKeys.self)
            id = values.optional(String.self, .id) ?? ""
            date = values.optional(String.self, .date) ?? ""
            time = values.optional(String.self, .time) ?? ""
            status = values.optional(String.self, .status) ?? ""
            teams = values.optional([Participant].self, .teams) ?? []
            round = values.optional(String.self, .round) ?? ""
            stage = values.optional(String.self, .stage) ?? ""
        }
    }
    struct Participant: Decodable {
        let name: String
        let score: Int?
        enum CodingKeys: String, CodingKey { case name, score }
        init(from decoder: Decoder) throws {
            let values = try decoder.container(keyedBy: CodingKeys.self)
            name = values.optional(String.self, .name) ?? ""
            score = values.optional(Int.self, .score)
        }
    }
    struct Standing: Decodable {
        let logo: String?
        let team: String
        let wins: Int?
        let losses: Int?
        let ties: Int?
        let map_difference: Int?
        let group: String?
        enum CodingKeys: String, CodingKey { case logo, team, wins, losses, ties, map_difference, group }
        init(from decoder: Decoder) throws {
            let values = try decoder.container(keyedBy: CodingKeys.self)
            logo = values.optional(String.self, .logo)
            team = values.optional(String.self, .team) ?? ""
            wins = values.optional(Int.self, .wins)
            losses = values.optional(Int.self, .losses)
            ties = values.optional(Int.self, .ties)
            map_difference = values.optional(Int.self, .map_difference)
            group = values.optional(String.self, .group)
        }
    }
    struct Prize: Decodable {
        let position: String
        let prize: String
        let team: Team?
        enum CodingKeys: String, CodingKey { case position, prize, team }
        init(from decoder: Decoder) throws {
            let values = try decoder.container(keyedBy: CodingKeys.self)
            position = values.optional(String.self, .position) ?? ""
            prize = values.optional(String.self, .prize) ?? ""
            team = values.optional(Team.self, .team)
        }
    }
}

private extension KeyedDecodingContainer {
    func optional<Value: Decodable>(_ type: Value.Type, _ key: Key) -> Value? {
        try? decodeIfPresent(type, forKey: key)
    }
}

enum EventWidgetMapper {
    private static func clean(_ value: String?) -> String? {
        guard let value else { return nil }
        let text = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return text.isEmpty || text == "-" || text.caseInsensitiveCompare("TBD") == .orderedSame ? nil : text
    }

    private static func nameKey(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(with: Locale(identifier: "en_US_POSIX"))
    }

    static func decode(_ bytes: Data, expectedEventID: String, at date: Date) throws -> EventWidgetData {
        try normalize(JSONDecoder().decode(EventResponse.self, from: bytes), expectedEventID: expectedEventID, at: date)
    }

    fileprivate static func normalize(_ response: EventResponse, expectedEventID: String, at date: Date) throws -> EventWidgetData {
        guard clean(response.id) == expectedEventID, let title = clean(response.title) else { throw WidgetRefreshError.invalidResponse }
        var roster: [String: WidgetTeam] = [:]
        var conflicts = Set<String>()
        for tier in [response.teams, response.prizes.compactMap(\.team)] {
            let grouped = Dictionary(grouping: tier.filter { clean($0.name) != nil }, by: { nameKey($0.name) })
            for (key, values) in grouped where roster[key] == nil {
                let ids = Set(values.compactMap { clean($0.id) })
                if ids.count > 1 { conflicts.insert(key) }
                let first = values[0]
                roster[key] = .init(id: ids.count == 1 ? ids.first : nil, name: clean(first.name)!,
                                   logo: ids.count > 1 ? nil : values.compactMap { clean($0.img) }.first)
            }
        }
        for row in response.standings {
            guard let name = clean(row.team) else { continue }
            let key = nameKey(name)
            if conflicts.contains(key) { continue }
            if var team = roster[key] { team.logo = team.logo ?? clean(row.logo); roster[key] = team }
            else { roster[key] = .init(id: nil, name: name, logo: clean(row.logo)) }
        }
        func slot(_ name: String) -> WidgetParticipant {
            guard let name = clean(name) else { return .undecided }
            return .team(roster[nameKey(name)] ?? .init(id: nil, name: name))
        }
        var seen = Set<String>()
        let matches = response.matches.compactMap { match -> EventWidgetMatch? in
            guard let id = clean(match.id), seen.insert(id).inserted, match.teams.count <= 2 else { return nil }
            let score: WidgetScore?
            if match.teams.count == 2, let first = match.teams[0].score, let second = match.teams[1].score, first >= 0, second >= 0 {
                score = .init(team: first, opponent: second)
            } else { score = nil }
            let phase: EventMatchPhase
            switch match.status.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) {
            case "completed", "final": phase = .completed(score)
            case "live", "ongoing": phase = .live(score)
            case "upcoming", "tbd": phase = .upcoming
            default: phase = .unknown
            }
            let fixture = EventWidgetFixture(id: id,
                participants: .init(left: match.teams.first.map { slot($0.name) } ?? .undecided,
                                    right: match.teams.dropFirst().first.map { slot($0.name) } ?? .undecided),
                start: EventWidgetDates.matchStart(date: match.date, time: match.time), day: WidgetDay(match.date),
                round: match.round, stage: match.stage, format: nil)
            return .init(fixture: fixture, phase: phase)
        }
        var groups: [EventWidgetGroup] = []
        for row in response.standings {
            guard let group = clean(row.group), let wins = row.wins, let losses = row.losses,
                  wins >= 0, losses >= 0, (row.ties ?? 0) >= 0, let difference = row.map_difference,
                  case .team(let team) = slot(row.team) else { continue }
            let standing = EventWidgetStanding(team: team, wins: wins, losses: losses, ties: row.ties ?? 0, mapDifference: difference)
            if let index = groups.firstIndex(where: { nameKey($0.name) == nameKey(group) }) {
                let existing = groups[index]
                groups[index] = .init(name: existing.name, standings: existing.standings + [standing])
            } else { groups.append(.init(name: group, standings: [standing])) }
        }
        let prizes = response.prizes.compactMap { prize -> EventWidgetPrize? in
            guard let raw = prize.team, let name = clean(raw.name) else { return nil }
            let known = roster[nameKey(name)]
            let team = WidgetTeam(id: clean(raw.id) ?? known?.id, name: name, logo: clean(raw.img) ?? known?.logo)
            return .init(position: prize.position, amount: clean(prize.prize) ?? "", team: team)
        }
        let status: EventReportedStatus
        switch response.status.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) {
        case "completed", "final": status = .completed
        case "live", "ongoing": status = .ongoing
        case "paused": status = .paused
        case "upcoming", "tbd": status = .upcoming
        default: status = .unknown
        }
        let event = EventWidgetMetadata(id: expectedEventID, title: title, logo: clean(response.img), location: response.location,
            prizePool: clean(response.prize), dates: EventWidgetDates.parse(response.dates, matchDays: matches.compactMap(\.fixture.day), reference: date), reportedStatus: status)
        return .init(event: event, matches: matches, groups: groups, prizes: prizes, detail: nil)
    }

    static func enrich(_ data: EventWidgetData, matchID: String, details: WidgetMatchDetailsResponse) -> EventWidgetData {
        guard let match = data.matches.first(where: { $0.fixture.id == matchID }) else { return data }
        func enrich(_ slot: WidgetParticipant) -> WidgetParticipant {
            guard case .team(var team) = slot else { return slot }
            let identity = team.id.flatMap { id in id.isEmpty ? nil : details.teams.first { $0.id == id } }
            let detail = identity ?? details.teams.first { nameKey($0.name) == nameKey(team.name) }
            team.logo = team.logo ?? clean(detail?.img)
            team.tag = team.tag ?? clean(detail?.tag)
            return .team(team)
        }
        let pair = WidgetMatchPair(left: enrich(match.fixture.participants.left), right: enrich(match.fixture.participants.right))
        let series = WidgetSeriesMapper.make(details: details, participants: pair, perspective: .left)
        return data.adding(.init(matchID: matchID, series: series), participants: pair)
    }
}

struct EventWidgetRefreshService {
    private let api: WidgetAPIClient
    private let store: FavoriteWidgetStore<EventWidgetData>

    init(api: WidgetAPIClient = .live(authorization: GeneratedBuildConfig.authToken),
         store: FavoriteWidgetStore<EventWidgetData> = FavoriteWidgetStores.events) {
        self.api = api
        self.store = store
    }

    func refresh(eventID: String, source: UpcomingMatchesSnapshot, at date: Date, timeZone: TimeZone) async -> FavoriteWidgetRecord<EventWidgetData>? {
        let pathCharacters = CharacterSet.urlPathAllowed.subtracting(CharacterSet(charactersIn: "/?#%"))
        guard source.favoriteEvents.contains(where: { $0.id == eventID }), !source.clientId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let encoded = eventID.addingPercentEncoding(withAllowedCharacters: pathCharacters) else { return nil }
        let key = FavoriteWidgetKey(entityID: eventID, clientID: source.clientId)
        let favorites = Set(source.favorites.teamIds)
        let hidden = source.spoilersHidden
        let api = api
        return await store.refresh(for: key) {
            let response: EventResponse = try await api.get("/api/v1/events/\(encoded)")
            var data = try EventWidgetMapper.normalize(response, expectedEventID: eventID, at: date)
            let selected = EventWidgetEngine.selection(in: data, fetchedAt: date, favoriteTeamIDs: favorites,
                spoilersHidden: hidden, at: date, timeZone: timeZone)
            if let id = selected.detailMatchID?.addingPercentEncoding(withAllowedCharacters: pathCharacters),
               let details: WidgetMatchDetailsResponse = try? await api.get("/api/v1/matches/\(id)"), let matchID = selected.detailMatchID {
                data = EventWidgetMapper.enrich(data, matchID: matchID, details: details)
            }
            return data
        }
    }
}
