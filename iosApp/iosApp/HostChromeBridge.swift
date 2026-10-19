import UIKit
import shared

final class HostChromeBridge: UIView {
    private struct Metrics: Equatable {
        var edge: Int32 = 0
        var barTopReserved: CGFloat = 0
        var hinge: CGRect?
    }

    private let store: IosHostChromeStore
    private var updateScheduled = false
    private var lastMetrics: Metrics?

    init(store: IosHostChromeStore) {
        self.store = store
        super.init(frame: .zero)
        isUserInteractionEnabled = false
        backgroundColor = .clear
        autoresizingMask = [.flexibleWidth, .flexibleHeight]

        if #available(iOS 27.1, *) {
            registerForTraitChanges(UITraitCollection.systemTraitsAffectingVerticalBarEdge) {
                (view: HostChromeBridge, _: UITraitCollection) in
                view.scheduleUpdate()
            }
            addInteraction(UIHingeInteraction { [weak self] _, _ in
                self?.scheduleUpdate()
            })
        }
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        scheduleUpdate()
    }

    override func safeAreaInsetsDidChange() {
        super.safeAreaInsetsDidChange()
        scheduleUpdate()
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        scheduleUpdate()
    }

    private func scheduleUpdate() {
        guard !updateScheduled else { return }
        updateScheduled = true
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.updateScheduled = false
            self.pushUpdate()
        }
    }

    private func pushUpdate() {
        guard let host = superview, let window = host.window else { return }
        var metrics = Metrics()
        if #available(iOS 27.1, *) {
            switch host.traitCollection.verticalBarEdge {
            case .leading: metrics.edge = 1
            case .trailing: metrics.edge = 2
            default: break
            }

            let bounds = host.bounds
            if metrics.edge != 0 {
                let isRight = (metrics.edge == 2) != (host.effectiveUserInterfaceLayoutDirection == .rightToLeft)
                let width = isRight ? host.safeAreaInsets.right : host.safeAreaInsets.left
                let strip = CGRect(
                    x: isRight ? bounds.maxX - width : bounds.minX,
                    y: bounds.minY,
                    width: width,
                    height: bounds.height
                )
                metrics.barTopReserved = host.reservedRegions(kind: .occlusion)
                    .filter { $0.isActive && bounds.contains($0.frame) && $0.frame.intersects(strip) }
                    .map { $0.frame.maxY - bounds.minY }
                    .max() ?? 0
            }

            if let division = host.reservedRegions(kind: .division).first(where: {
                $0.isActive && bounds.contains($0.frame) && $0.frame.height > $0.frame.width
                    && abs($0.frame.minY - bounds.minY) < 1
                    && abs($0.frame.maxY - bounds.maxY) < 1
            }) {
                let frame = division.frame
                let margins = division.margins
                let expanded = CGRect(
                    x: frame.minX - margins.left,
                    y: frame.minY - margins.top,
                    width: frame.width + margins.left + margins.right,
                    height: frame.height + margins.top + margins.bottom
                )
                metrics.hinge = host.convert(expanded, to: window)
            }
        }
        guard metrics != lastMetrics else { return }
        lastMetrics = metrics
        let hinge = metrics.hinge ?? .zero
        store.update(
            edge: metrics.edge,
            barTopReserved: Double(metrics.barTopReserved),
            hingeX: Double(hinge.minX),
            hingeY: Double(hinge.minY),
            hingeWidth: Double(hinge.width),
            hingeHeight: Double(hinge.height)
        )
    }
}
