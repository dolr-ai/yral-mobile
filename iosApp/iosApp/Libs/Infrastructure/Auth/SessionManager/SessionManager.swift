//
//  SessionManager.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Combine

@MainActor
final class SessionManager: ObservableObject {
  @Published private(set) var state: AuthState = .uninitialized
  private var bag: AnyCancellable?

  init(auth: AuthClient) {
    bag = auth.authStatePublisher
      .receive(on: RunLoop.main)
      .assign(to: \.state, on: self)
  }
}
