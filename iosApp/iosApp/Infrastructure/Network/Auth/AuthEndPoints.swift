//
//  AuthEndPoints.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import UIKit
class AuthEndpoints {
    static func setAnonymousIdentityCookie(payload: Data) -> Endpoint {
        return Endpoint(
            http: "SetAnonymousIdentityCookie",
            baseURL: URL(string: AuthConstants.baseURL)!,
            path: AuthConstants.setAnonymousIdentityCookiePath,
            method: .post,
            headers: [AuthConstants.contentTypeHeader: AuthConstants.contentTypeJSON],
            body: payload
        )
    }

    static func extractIdentity(cookie: HTTPCookie) -> Endpoint {
        return Endpoint(
            http: "ExtractIdentity",
            baseURL: URL(string: AuthConstants.baseURL)!,
            path: AuthConstants.extractIdentityPath,
            method: .post,
            headers: [
                AuthConstants.contentTypeHeader: AuthConstants.contentTypeJSON,
                AuthConstants.cookieHeader: "\(cookie.name)=\(cookie.value)"
            ],
            body: Data(AuthConstants.emptyRequestBody.utf8)
        )
    }
}
