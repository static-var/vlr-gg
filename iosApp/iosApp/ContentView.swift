import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()
            Text("Hello World")
                .font(.largeTitle)
                .fontWeight(.bold)
        }
    }
}
