//
//  UploadVideoRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct UploadVideoRequest: Codable {
  let fileURL: URL
  let uploadURLString: String
}
