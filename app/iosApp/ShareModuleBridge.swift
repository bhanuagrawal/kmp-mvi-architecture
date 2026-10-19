import Foundation
import sharedLogic

class ShareModuleBridge {
    static let shared = ShareModuleBridge()

    private let shareModule: ShareModule

    private init() {
        shareModule = SharedModuleKt.buildSharedModule()
    }

    func getRegisterUsecase() -> RegisterUsecase {
        return shareModule.getRegisterUsecase()
    }

    func getDeleteUserUsecase() -> DeleteUserUsecase {
        return shareModule.getDeleteUserUsecase()
    }

    func getUserPager() -> Pager {
        return shareModule.getUserPager()
    }
}
