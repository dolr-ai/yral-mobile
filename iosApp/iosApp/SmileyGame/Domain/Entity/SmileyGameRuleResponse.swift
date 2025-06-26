//
//  SmileyGameRule.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct SmileyGameRuleResponse: Identifiable {
  var id: String?
  let name: String
  let imageURL: String
  let body: [BodyElement]
}
