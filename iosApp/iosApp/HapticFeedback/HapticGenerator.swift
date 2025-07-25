//
//  HapticGenerator.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

public class HapticGenerator {
  private init() { }

  public static func performFeedback(_ feedback: HapticFeedback) {
    feedback.perform()
  }
}
