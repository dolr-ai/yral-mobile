//
//  SwiftBindings+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 06/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

extension AgentError: Error {}
extension AgentError: LocalizedError {
  public var errorDescription: String? {
    return String(describing: self)
  }
}

extension PrincipalError: Error {}
extension PrincipalError: LocalizedError {
  public var errorDescription: String? {
    return String(describing: self)
  }
}

extension RustString: Error {
  public var localizedDescription: String? {
    return self.toString()
  }
}

extension RustString: LocalizedError {
  public var errorDescription: String? {
    return self.toString()
  }
}

extension Secp256k1Error: Error {}
extension Secp256k1Error: LocalizedError {
  public var errorDescription: String? {
    return String(describing: self)
  }
}

extension BalanceError: Error {}
extension BalanceError: LocalizedError {
  public var errorDescription: String? {
    return String(describing: self)
  }
}

extension RustVec where T == UInt8 {
  public convenience init(bytes: UnsafeRawBufferPointer) {
    self.init()
    let bound = bytes.bindMemory(to: UInt8.self)
    for item in bound {
      self.push(value: item)
    }
  }
}

extension FFIError: Error {}
extension FFIError: LocalizedError {
  public var errorDescription: String? {
    return self.to_string().toString()
  }
}
