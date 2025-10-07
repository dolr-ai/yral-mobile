//
//  LeaderboardQuery.swift
//  iosApp
//
//  Created by Samarth Paboowal on 08/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct LeaderboardQuery: Encodable {
  let mode: String
  let countryCode: String
  var principalID: String?

  enum CodingKeys: String, CodingKey {
    case mode
    case countryCode = "country_code"
    case principalID = "principal_id"
  }
}

extension LeaderboardQuery {
  func addingPrincipal(_ id: String) -> LeaderboardQuery {
    var copy = self
    copy.principalID = id
    return copy
  }
}
