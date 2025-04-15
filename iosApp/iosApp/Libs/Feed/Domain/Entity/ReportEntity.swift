//
//  ReportEntity.swift
//  Yral
//
//  Created by Sarvesh Sharma on 28/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct ReportRequest: Codable {
  let postId: UInt64
  let videoId: String
  let reason: String
  let canisterID: String
}
