//
//  AppUpdateDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct AppUpdateDTO: Decodable {
  let minSupportedVersion: String
  let recommendedVersion: String

  enum CodingKeys: String, CodingKey {
    case minSupportedVersion = "min_supported_version"
    case recommendedVersion = "recommended_version"
  }
}
