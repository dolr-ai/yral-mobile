//
//  VideoUploadError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

enum VideoUploadError: Error {
  case invalidFileURL(String)
  case network(NetworkError)
  case auth(AuthError)
  case unknown
}
