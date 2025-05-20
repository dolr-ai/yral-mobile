//
//  NetworkMonitor.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Network

protocol NetworkMonitorProtocol: AnyObject {
  var isGoodForPrefetch: Bool { get }
  var isNetworkAvailable: Bool { get set }
  func startMonitoring()
}

final class DefaultNetworkMonitor: NetworkMonitorProtocol {
  private let monitor = NWPathMonitor()
  private let monitorQueue = DispatchQueue.global(qos: .background)
  var isNetworkAvailable: Bool = true
  var isGoodForPrefetch: Bool = true

  func startMonitoring() {
    monitor.pathUpdateHandler = { [weak self] path in
      let constrained = path.isConstrained
      Task { @MainActor in
        self?.isGoodForPrefetch = (!constrained && path.status == .satisfied)
      }
    }
    monitor.start(queue: monitorQueue)
  }
}
