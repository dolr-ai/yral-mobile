//
//  Data+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 07/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

extension Data {
  public func base64URLEncodedString() -> String {
      let base64String = self.base64EncodedString()
      return base64String
          .replacingOccurrences(of: "=", with: "")
          .replacingOccurrences(of: "+", with: "-")
          .replacingOccurrences(of: "/", with: "_")
  }
}
