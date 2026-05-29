//
//  iosAppApp.swift
//  iosApp
//
//  Created by Ales Krot on 27.05.2026.
//

import SwiftUI
import CoreData

@main
struct iosAppApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
        }
    }
}
