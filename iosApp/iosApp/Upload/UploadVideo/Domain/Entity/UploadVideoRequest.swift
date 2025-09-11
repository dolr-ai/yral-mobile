//
//  UploadVideoRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct UploadVideoRequest {
  let fileURL: URL
  let videoUID: String
  let uploadURLString: String
  let caption: String
  let hashtags: [String]
  let isNSFW: Bool = false
  let creatorConsentForInclusionInHotOrNot: Bool = true
}
