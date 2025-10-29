//
//  ProfileInfo.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct AccountInfo: Equatable {
  let imageURL: URL?
  let canisterID: String
  let username: String
  let followers: Int?
  let following: Int?
  let gamesPlayed: Int?
  let bio: String?
}
