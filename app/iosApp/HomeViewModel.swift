import Foundation
import sharedLogic
import Combine

@MainActor
class HomeViewModel: ObservableObject {
    @Published var users: [User] = []
    @Published var isLoading = false
    @Published var isError = false
    @Published var errorMessage: String?
    @Published var allItemLoaded = false

    private let registerUsecase: RegisterUsecase
    private let deleteUserUsecase: DeleteUserUsecase
    private let loadUsersEventHandler: LoadUsersEvenHandler

    init() {
        let bridge = ShareModuleBridge.shared
        self.registerUsecase = bridge.getRegisterUsecase()
        self.deleteUserUsecase = bridge.getDeleteUserUsecase()
        self.loadUsersEventHandler = LoadUsersEvenHandler(pager: bridge.getUserPager())
    }

    func loadData() {
        Task {
            do {
                let flow = loadUsersEventHandler.handleEvent(
                    event: HomePageEvents.LoadData(scrollDirection: nil),
                    stateProvider: { [weak self] in
                        self?.toHomePageState() ?? HomePageState()
                    }
                )
                for try await result in flow {
                    switch result {
                    case let transformState as EvenResultTransformState<HomePageState>:
                        applyTransform(transformState.transform)
                    case let sideEffectEvent as EvenResultSideEffectEvent<HomePageSideEffects>:
                        handleSideEffect(sideEffectEvent.sideEffect)
                    default:
                        break
                    }
                }
            } catch {
                isError = true
                errorMessage = error.localizedDescription
            }
        }
    }

    func addUser() {
        Task {
            do {
                let result = try registerUsecase.execute(
                    email: "\(Int(Date().timeIntervalSince1970 * 1000))@gmail.com",
                    password: "123"
                )
                if result.isSuccess {
                    if let user = result.getOrNull() {
                        users.append(User(username: user.username, userId: user.userId))
                    }
                } else {
                    errorMessage = "Could not add User"
                    isError = true
                }
            } catch {
                errorMessage = error.localizedDescription
                isError = true
            }
        }
    }

    func removeUser(_ user: User) {
        Task {
            do {
                let result = try deleteUserUsecase.execute(userId: user.userId)
                if result.isSuccess {
                    users.removeAll { $0.userId == user.userId }
                } else {
                    errorMessage = "Could not delete User"
                    isError = true
                }
            } catch {
                errorMessage = error.localizedDescription
                isError = true
            }
        }
    }

    private func applyTransform(_ transform: (HomePageState) -> HomePageState) {
        let newState = transform(toHomePageState())
        users = newState.users.map { User(username: $0.username, userId: $0.userId) }
        isLoading = newState.isLoading
        isError = newState.isError
        errorMessage = newState.errorMessage
        allItemLoaded = newState.allItemLoaded
    }

    private func handleSideEffect(_ sideEffect: HomePageSideEffects) {
        if let showError = sideEffect as? HomePageSideEffectsShowErrorToast {
            errorMessage = showError.message
            isError = true
        }
    }

    private func toHomePageState() -> HomePageState {
        return HomePageState(
            users: users.map { UserDomainModel(username: $0.username, userId: $0.userId) },
            allItemLoaded: allItemLoaded,
            isLoadingBefore: false,
            isLoadingAfter: false,
            isLoading: isLoading,
            isError: isError,
            errorMessage: errorMessage
        )
    }
}
