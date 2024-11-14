public func get_secp256k1_identity(_ jwk_key: JwkEcKey) -> Optional<Secp256k1Identity> {
    { let val = __swift_bridge__$get_secp256k1_identity({jwk_key.isOwned = false; return jwk_key.ptr;}()); if val != nil { return Secp256k1Identity(ptr: val!) } else { return nil } }()
}
public func get_jwk_ec_key<GenericIntoRustString: IntoRustString>(_ json_string: GenericIntoRustString) -> Optional<JwkEcKey> {
    { let val = __swift_bridge__$get_jwk_ec_key({ let rustString = json_string.intoRustString(); rustString.isOwned = false; return rustString.ptr }()); if val != nil { return JwkEcKey(ptr: val!) } else { return nil } }()
}
public func delegated_identity_from_bytes(_ data: UnsafeBufferPointer<UInt8>) throws -> DelegatedIdentity {
    try { let val = __swift_bridge__$delegated_identity_from_bytes(data.toFfiSlice()); if val.is_ok { return DelegatedIdentity(ptr: val.ok_or_err!) } else { throw RustString(ptr: val.ok_or_err!) } }()
}

public class DelegatedIdentity: DelegatedIdentityRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$DelegatedIdentity$_free(ptr)
        }
    }
}
public class DelegatedIdentityRefMut: DelegatedIdentityRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class DelegatedIdentityRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension DelegatedIdentity: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_DelegatedIdentity$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_DelegatedIdentity$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: DelegatedIdentity) {
        __swift_bridge__$Vec_DelegatedIdentity$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_DelegatedIdentity$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (DelegatedIdentity(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DelegatedIdentityRef> {
        let pointer = __swift_bridge__$Vec_DelegatedIdentity$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DelegatedIdentityRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DelegatedIdentityRefMut> {
        let pointer = __swift_bridge__$Vec_DelegatedIdentity$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DelegatedIdentityRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<DelegatedIdentityRef> {
        UnsafePointer<DelegatedIdentityRef>(OpaquePointer(__swift_bridge__$Vec_DelegatedIdentity$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_DelegatedIdentity$len(vecPtr)
    }
}


public class Secp256k1Identity: Secp256k1IdentityRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Secp256k1Identity$_free(ptr)
        }
    }
}
public class Secp256k1IdentityRefMut: Secp256k1IdentityRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Secp256k1IdentityRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Secp256k1Identity: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Secp256k1Identity$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Secp256k1Identity$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Secp256k1Identity) {
        __swift_bridge__$Vec_Secp256k1Identity$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Secp256k1Identity$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Secp256k1Identity(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Secp256k1IdentityRef> {
        let pointer = __swift_bridge__$Vec_Secp256k1Identity$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Secp256k1IdentityRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Secp256k1IdentityRefMut> {
        let pointer = __swift_bridge__$Vec_Secp256k1Identity$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Secp256k1IdentityRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Secp256k1IdentityRef> {
        UnsafePointer<Secp256k1IdentityRef>(OpaquePointer(__swift_bridge__$Vec_Secp256k1Identity$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Secp256k1Identity$len(vecPtr)
    }
}


public class JwkEcKey: JwkEcKeyRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$JwkEcKey$_free(ptr)
        }
    }
}
public class JwkEcKeyRefMut: JwkEcKeyRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class JwkEcKeyRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension JwkEcKey: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_JwkEcKey$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_JwkEcKey$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: JwkEcKey) {
        __swift_bridge__$Vec_JwkEcKey$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_JwkEcKey$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (JwkEcKey(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<JwkEcKeyRef> {
        let pointer = __swift_bridge__$Vec_JwkEcKey$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return JwkEcKeyRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<JwkEcKeyRefMut> {
        let pointer = __swift_bridge__$Vec_JwkEcKey$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return JwkEcKeyRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<JwkEcKeyRef> {
        UnsafePointer<JwkEcKeyRef>(OpaquePointer(__swift_bridge__$Vec_JwkEcKey$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_JwkEcKey$len(vecPtr)
    }
}


public class Nat: NatRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Nat$_free(ptr)
        }
    }
}
public class NatRefMut: NatRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NatRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Nat: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Nat$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Nat$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Nat) {
        __swift_bridge__$Vec_Nat$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Nat$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Nat(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NatRef> {
        let pointer = __swift_bridge__$Vec_Nat$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NatRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NatRefMut> {
        let pointer = __swift_bridge__$Vec_Nat$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NatRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NatRef> {
        UnsafePointer<NatRef>(OpaquePointer(__swift_bridge__$Vec_Nat$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Nat$len(vecPtr)
    }
}


public class ByteBuf: ByteBufRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$ByteBuf$_free(ptr)
        }
    }
}
public class ByteBufRefMut: ByteBufRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class ByteBufRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension ByteBuf: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_ByteBuf$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_ByteBuf$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: ByteBuf) {
        __swift_bridge__$Vec_ByteBuf$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_ByteBuf$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (ByteBuf(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<ByteBufRef> {
        let pointer = __swift_bridge__$Vec_ByteBuf$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return ByteBufRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<ByteBufRefMut> {
        let pointer = __swift_bridge__$Vec_ByteBuf$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return ByteBufRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<ByteBufRef> {
        UnsafePointer<ByteBufRef>(OpaquePointer(__swift_bridge__$Vec_ByteBuf$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_ByteBuf$len(vecPtr)
    }
}


public class Principal: PrincipalRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Principal$_free(ptr)
        }
    }
}
public class PrincipalRefMut: PrincipalRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PrincipalRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Principal: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Principal$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Principal$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Principal) {
        __swift_bridge__$Vec_Principal$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Principal$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Principal(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PrincipalRef> {
        let pointer = __swift_bridge__$Vec_Principal$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PrincipalRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PrincipalRefMut> {
        let pointer = __swift_bridge__$Vec_Principal$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PrincipalRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PrincipalRef> {
        UnsafePointer<PrincipalRef>(OpaquePointer(__swift_bridge__$Vec_Principal$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Principal$len(vecPtr)
    }
}


public class PrincipalError: PrincipalErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PrincipalError$_free(ptr)
        }
    }
}
public class PrincipalErrorRefMut: PrincipalErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PrincipalErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PrincipalError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PrincipalError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PrincipalError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PrincipalError) {
        __swift_bridge__$Vec_PrincipalError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PrincipalError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PrincipalError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PrincipalErrorRef> {
        let pointer = __swift_bridge__$Vec_PrincipalError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PrincipalErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PrincipalErrorRefMut> {
        let pointer = __swift_bridge__$Vec_PrincipalError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PrincipalErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PrincipalErrorRef> {
        UnsafePointer<PrincipalErrorRef>(OpaquePointer(__swift_bridge__$Vec_PrincipalError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PrincipalError$len(vecPtr)
    }
}


public class AgentError: AgentErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$AgentError$_free(ptr)
        }
    }
}
public class AgentErrorRefMut: AgentErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class AgentErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension AgentError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_AgentError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_AgentError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: AgentError) {
        __swift_bridge__$Vec_AgentError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_AgentError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (AgentError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<AgentErrorRef> {
        let pointer = __swift_bridge__$Vec_AgentError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return AgentErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<AgentErrorRefMut> {
        let pointer = __swift_bridge__$Vec_AgentError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return AgentErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<AgentErrorRef> {
        UnsafePointer<AgentErrorRef>(OpaquePointer(__swift_bridge__$Vec_AgentError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_AgentError$len(vecPtr)
    }
}


public class FollowerArg: FollowerArgRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$FollowerArg$_free(ptr)
        }
    }
}
public class FollowerArgRefMut: FollowerArgRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class FollowerArgRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension FollowerArg: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_FollowerArg$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_FollowerArg$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: FollowerArg) {
        __swift_bridge__$Vec_FollowerArg$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_FollowerArg$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (FollowerArg(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowerArgRef> {
        let pointer = __swift_bridge__$Vec_FollowerArg$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowerArgRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowerArgRefMut> {
        let pointer = __swift_bridge__$Vec_FollowerArg$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowerArgRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<FollowerArgRef> {
        UnsafePointer<FollowerArgRef>(OpaquePointer(__swift_bridge__$Vec_FollowerArg$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_FollowerArg$len(vecPtr)
    }
}


public class Result27: Result27RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result27$_free(ptr)
        }
    }
}
public class Result27RefMut: Result27Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result27Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result27: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result27$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result27$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result27) {
        __swift_bridge__$Vec_Result27$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result27$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result27(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result27Ref> {
        let pointer = __swift_bridge__$Vec_Result27$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result27Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result27RefMut> {
        let pointer = __swift_bridge__$Vec_Result27$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result27RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result27Ref> {
        UnsafePointer<Result27Ref>(OpaquePointer(__swift_bridge__$Vec_Result27$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result27$len(vecPtr)
    }
}


public class UpdateProfileSetUniqueUsernameError: UpdateProfileSetUniqueUsernameErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UpdateProfileSetUniqueUsernameError$_free(ptr)
        }
    }
}
public class UpdateProfileSetUniqueUsernameErrorRefMut: UpdateProfileSetUniqueUsernameErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UpdateProfileSetUniqueUsernameErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UpdateProfileSetUniqueUsernameError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UpdateProfileSetUniqueUsernameError) {
        __swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UpdateProfileSetUniqueUsernameError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UpdateProfileSetUniqueUsernameErrorRef> {
        let pointer = __swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UpdateProfileSetUniqueUsernameErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UpdateProfileSetUniqueUsernameErrorRefMut> {
        let pointer = __swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UpdateProfileSetUniqueUsernameErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UpdateProfileSetUniqueUsernameErrorRef> {
        UnsafePointer<UpdateProfileSetUniqueUsernameErrorRef>(OpaquePointer(__swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UpdateProfileSetUniqueUsernameError$len(vecPtr)
    }
}


public class Result26: Result26RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result26$_free(ptr)
        }
    }
}
public class Result26RefMut: Result26Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result26Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result26: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result26$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result26$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result26) {
        __swift_bridge__$Vec_Result26$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result26$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result26(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result26Ref> {
        let pointer = __swift_bridge__$Vec_Result26$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result26Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result26RefMut> {
        let pointer = __swift_bridge__$Vec_Result26$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result26RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result26Ref> {
        UnsafePointer<Result26Ref>(OpaquePointer(__swift_bridge__$Vec_Result26$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result26$len(vecPtr)
    }
}


public class Result25: Result25RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result25$_free(ptr)
        }
    }
}
public class Result25RefMut: Result25Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result25Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result25: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result25$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result25$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result25) {
        __swift_bridge__$Vec_Result25$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result25$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result25(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result25Ref> {
        let pointer = __swift_bridge__$Vec_Result25$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result25Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result25RefMut> {
        let pointer = __swift_bridge__$Vec_Result25$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result25RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result25Ref> {
        UnsafePointer<Result25Ref>(OpaquePointer(__swift_bridge__$Vec_Result25$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result25$len(vecPtr)
    }
}


public class UpdateProfileDetailsError: UpdateProfileDetailsErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UpdateProfileDetailsError$_free(ptr)
        }
    }
}
public class UpdateProfileDetailsErrorRefMut: UpdateProfileDetailsErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UpdateProfileDetailsErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UpdateProfileDetailsError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UpdateProfileDetailsError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UpdateProfileDetailsError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UpdateProfileDetailsError) {
        __swift_bridge__$Vec_UpdateProfileDetailsError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UpdateProfileDetailsError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UpdateProfileDetailsError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UpdateProfileDetailsErrorRef> {
        let pointer = __swift_bridge__$Vec_UpdateProfileDetailsError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UpdateProfileDetailsErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UpdateProfileDetailsErrorRefMut> {
        let pointer = __swift_bridge__$Vec_UpdateProfileDetailsError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UpdateProfileDetailsErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UpdateProfileDetailsErrorRef> {
        UnsafePointer<UpdateProfileDetailsErrorRef>(OpaquePointer(__swift_bridge__$Vec_UpdateProfileDetailsError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UpdateProfileDetailsError$len(vecPtr)
    }
}


public class UserProfileUpdateDetailsFromFrontend: UserProfileUpdateDetailsFromFrontendRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UserProfileUpdateDetailsFromFrontend$_free(ptr)
        }
    }
}
public class UserProfileUpdateDetailsFromFrontendRefMut: UserProfileUpdateDetailsFromFrontendRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UserProfileUpdateDetailsFromFrontendRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UserProfileUpdateDetailsFromFrontend: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UserProfileUpdateDetailsFromFrontend) {
        __swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UserProfileUpdateDetailsFromFrontend(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileUpdateDetailsFromFrontendRef> {
        let pointer = __swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileUpdateDetailsFromFrontendRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileUpdateDetailsFromFrontendRefMut> {
        let pointer = __swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileUpdateDetailsFromFrontendRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UserProfileUpdateDetailsFromFrontendRef> {
        UnsafePointer<UserProfileUpdateDetailsFromFrontendRef>(OpaquePointer(__swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UserProfileUpdateDetailsFromFrontend$len(vecPtr)
    }
}


public class PostViewDetailsFromFrontend: PostViewDetailsFromFrontendRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostViewDetailsFromFrontend$_free(ptr)
        }
    }
}
public class PostViewDetailsFromFrontendRefMut: PostViewDetailsFromFrontendRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostViewDetailsFromFrontendRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostViewDetailsFromFrontend: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostViewDetailsFromFrontend$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostViewDetailsFromFrontend$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostViewDetailsFromFrontend) {
        __swift_bridge__$Vec_PostViewDetailsFromFrontend$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostViewDetailsFromFrontend$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostViewDetailsFromFrontend(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostViewDetailsFromFrontendRef> {
        let pointer = __swift_bridge__$Vec_PostViewDetailsFromFrontend$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostViewDetailsFromFrontendRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostViewDetailsFromFrontendRefMut> {
        let pointer = __swift_bridge__$Vec_PostViewDetailsFromFrontend$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostViewDetailsFromFrontendRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostViewDetailsFromFrontendRef> {
        UnsafePointer<PostViewDetailsFromFrontendRef>(OpaquePointer(__swift_bridge__$Vec_PostViewDetailsFromFrontend$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostViewDetailsFromFrontend$len(vecPtr)
    }
}


public class Result24: Result24RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result24$_free(ptr)
        }
    }
}
public class Result24RefMut: Result24Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result24Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result24: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result24$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result24$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result24) {
        __swift_bridge__$Vec_Result24$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result24$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result24(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result24Ref> {
        let pointer = __swift_bridge__$Vec_Result24$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result24Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result24RefMut> {
        let pointer = __swift_bridge__$Vec_Result24$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result24RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result24Ref> {
        UnsafePointer<Result24Ref>(OpaquePointer(__swift_bridge__$Vec_Result24$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result24$len(vecPtr)
    }
}


public class Result23: Result23RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result23$_free(ptr)
        }
    }
}
public class Result23RefMut: Result23Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result23Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result23: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result23$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result23$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result23) {
        __swift_bridge__$Vec_Result23$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result23$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result23(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result23Ref> {
        let pointer = __swift_bridge__$Vec_Result23$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result23Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result23RefMut> {
        let pointer = __swift_bridge__$Vec_Result23$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result23RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result23Ref> {
        UnsafePointer<Result23Ref>(OpaquePointer(__swift_bridge__$Vec_Result23$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result23$len(vecPtr)
    }
}


public class SettleNeuronsFundParticipationResponse: SettleNeuronsFundParticipationResponseRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SettleNeuronsFundParticipationResponse$_free(ptr)
        }
    }
}
public class SettleNeuronsFundParticipationResponseRefMut: SettleNeuronsFundParticipationResponseRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SettleNeuronsFundParticipationResponseRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SettleNeuronsFundParticipationResponse: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SettleNeuronsFundParticipationResponse) {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SettleNeuronsFundParticipationResponse(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SettleNeuronsFundParticipationResponseRef> {
        let pointer = __swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SettleNeuronsFundParticipationResponseRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SettleNeuronsFundParticipationResponseRefMut> {
        let pointer = __swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SettleNeuronsFundParticipationResponseRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SettleNeuronsFundParticipationResponseRef> {
        UnsafePointer<SettleNeuronsFundParticipationResponseRef>(OpaquePointer(__swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationResponse$len(vecPtr)
    }
}


public class Result22: Result22RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result22$_free(ptr)
        }
    }
}
public class Result22RefMut: Result22Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result22Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result22: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result22$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result22$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result22) {
        __swift_bridge__$Vec_Result22$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result22$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result22(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result22Ref> {
        let pointer = __swift_bridge__$Vec_Result22$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result22Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result22RefMut> {
        let pointer = __swift_bridge__$Vec_Result22$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result22RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result22Ref> {
        UnsafePointer<Result22Ref>(OpaquePointer(__swift_bridge__$Vec_Result22$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result22$len(vecPtr)
    }
}


public class GovernanceError: GovernanceErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$GovernanceError$_free(ptr)
        }
    }
}
public class GovernanceErrorRefMut: GovernanceErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class GovernanceErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension GovernanceError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_GovernanceError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_GovernanceError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: GovernanceError) {
        __swift_bridge__$Vec_GovernanceError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_GovernanceError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (GovernanceError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<GovernanceErrorRef> {
        let pointer = __swift_bridge__$Vec_GovernanceError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return GovernanceErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<GovernanceErrorRefMut> {
        let pointer = __swift_bridge__$Vec_GovernanceError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return GovernanceErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<GovernanceErrorRef> {
        UnsafePointer<GovernanceErrorRef>(OpaquePointer(__swift_bridge__$Vec_GovernanceError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_GovernanceError$len(vecPtr)
    }
}


public class Ok: OkRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Ok$_free(ptr)
        }
    }
}
public class OkRefMut: OkRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class OkRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Ok: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Ok$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Ok$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Ok) {
        __swift_bridge__$Vec_Ok$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Ok$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Ok(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<OkRef> {
        let pointer = __swift_bridge__$Vec_Ok$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return OkRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<OkRefMut> {
        let pointer = __swift_bridge__$Vec_Ok$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return OkRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<OkRef> {
        UnsafePointer<OkRef>(OpaquePointer(__swift_bridge__$Vec_Ok$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Ok$len(vecPtr)
    }
}


public class NeuronsFundNeuron: NeuronsFundNeuronRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NeuronsFundNeuron$_free(ptr)
        }
    }
}
public class NeuronsFundNeuronRefMut: NeuronsFundNeuronRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NeuronsFundNeuronRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NeuronsFundNeuron: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NeuronsFundNeuron$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NeuronsFundNeuron$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NeuronsFundNeuron) {
        __swift_bridge__$Vec_NeuronsFundNeuron$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NeuronsFundNeuron$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NeuronsFundNeuron(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronsFundNeuronRef> {
        let pointer = __swift_bridge__$Vec_NeuronsFundNeuron$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronsFundNeuronRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronsFundNeuronRefMut> {
        let pointer = __swift_bridge__$Vec_NeuronsFundNeuron$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronsFundNeuronRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NeuronsFundNeuronRef> {
        UnsafePointer<NeuronsFundNeuronRef>(OpaquePointer(__swift_bridge__$Vec_NeuronsFundNeuron$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NeuronsFundNeuron$len(vecPtr)
    }
}


public class SettleNeuronsFundParticipationRequest: SettleNeuronsFundParticipationRequestRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SettleNeuronsFundParticipationRequest$_free(ptr)
        }
    }
}
public class SettleNeuronsFundParticipationRequestRefMut: SettleNeuronsFundParticipationRequestRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SettleNeuronsFundParticipationRequestRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SettleNeuronsFundParticipationRequest: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SettleNeuronsFundParticipationRequest) {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SettleNeuronsFundParticipationRequest(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SettleNeuronsFundParticipationRequestRef> {
        let pointer = __swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SettleNeuronsFundParticipationRequestRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SettleNeuronsFundParticipationRequestRefMut> {
        let pointer = __swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SettleNeuronsFundParticipationRequestRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SettleNeuronsFundParticipationRequestRef> {
        UnsafePointer<SettleNeuronsFundParticipationRequestRef>(OpaquePointer(__swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SettleNeuronsFundParticipationRequest$len(vecPtr)
    }
}


public class Result21: Result21RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result21$_free(ptr)
        }
    }
}
public class Result21RefMut: Result21Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result21Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result21: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result21$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result21$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result21) {
        __swift_bridge__$Vec_Result21$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result21$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result21(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result21Ref> {
        let pointer = __swift_bridge__$Vec_Result21$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result21Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result21RefMut> {
        let pointer = __swift_bridge__$Vec_Result21$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result21RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result21Ref> {
        UnsafePointer<Result21Ref>(OpaquePointer(__swift_bridge__$Vec_Result21$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result21$len(vecPtr)
    }
}


public class Committed: CommittedRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Committed$_free(ptr)
        }
    }
}
public class CommittedRefMut: CommittedRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CommittedRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Committed: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Committed$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Committed$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Committed) {
        __swift_bridge__$Vec_Committed$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Committed$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Committed(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CommittedRef> {
        let pointer = __swift_bridge__$Vec_Committed$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CommittedRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CommittedRefMut> {
        let pointer = __swift_bridge__$Vec_Committed$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CommittedRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CommittedRef> {
        UnsafePointer<CommittedRef>(OpaquePointer(__swift_bridge__$Vec_Committed$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Committed$len(vecPtr)
    }
}


public class Result20: Result20RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result20$_free(ptr)
        }
    }
}
public class Result20RefMut: Result20Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result20Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result20: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result20$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result20$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result20) {
        __swift_bridge__$Vec_Result20$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result20$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result20(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result20Ref> {
        let pointer = __swift_bridge__$Vec_Result20$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result20Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result20RefMut> {
        let pointer = __swift_bridge__$Vec_Result20$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result20RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result20Ref> {
        UnsafePointer<Result20Ref>(OpaquePointer(__swift_bridge__$Vec_Result20$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result20$len(vecPtr)
    }
}


public class MigrationErrors: MigrationErrorsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$MigrationErrors$_free(ptr)
        }
    }
}
public class MigrationErrorsRefMut: MigrationErrorsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class MigrationErrorsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension MigrationErrors: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_MigrationErrors$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_MigrationErrors$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: MigrationErrors) {
        __swift_bridge__$Vec_MigrationErrors$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_MigrationErrors$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (MigrationErrors(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MigrationErrorsRef> {
        let pointer = __swift_bridge__$Vec_MigrationErrors$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MigrationErrorsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MigrationErrorsRefMut> {
        let pointer = __swift_bridge__$Vec_MigrationErrors$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MigrationErrorsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<MigrationErrorsRef> {
        UnsafePointer<MigrationErrorsRef>(OpaquePointer(__swift_bridge__$Vec_MigrationErrors$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_MigrationErrors$len(vecPtr)
    }
}


public class Result19: Result19RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result19$_free(ptr)
        }
    }
}
public class Result19RefMut: Result19Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result19Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result19: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result19$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result19$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result19) {
        __swift_bridge__$Vec_Result19$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result19$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result19(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result19Ref> {
        let pointer = __swift_bridge__$Vec_Result19$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result19Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result19RefMut> {
        let pointer = __swift_bridge__$Vec_Result19$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result19RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result19Ref> {
        UnsafePointer<Result19Ref>(OpaquePointer(__swift_bridge__$Vec_Result19$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result19$len(vecPtr)
    }
}


public class Result18: Result18RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result18$_free(ptr)
        }
    }
}
public class Result18RefMut: Result18Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result18Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result18: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result18$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result18$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result18) {
        __swift_bridge__$Vec_Result18$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result18$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result18(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result18Ref> {
        let pointer = __swift_bridge__$Vec_Result18$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result18Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result18RefMut> {
        let pointer = __swift_bridge__$Vec_Result18$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result18RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result18Ref> {
        UnsafePointer<Result18Ref>(OpaquePointer(__swift_bridge__$Vec_Result18$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result18$len(vecPtr)
    }
}


public class HttpResponse: HttpResponseRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$HttpResponse$_free(ptr)
        }
    }
}
public class HttpResponseRefMut: HttpResponseRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class HttpResponseRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension HttpResponse: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_HttpResponse$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_HttpResponse$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: HttpResponse) {
        __swift_bridge__$Vec_HttpResponse$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_HttpResponse$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (HttpResponse(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HttpResponseRef> {
        let pointer = __swift_bridge__$Vec_HttpResponse$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HttpResponseRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HttpResponseRefMut> {
        let pointer = __swift_bridge__$Vec_HttpResponse$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HttpResponseRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<HttpResponseRef> {
        UnsafePointer<HttpResponseRef>(OpaquePointer(__swift_bridge__$Vec_HttpResponse$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_HttpResponse$len(vecPtr)
    }
}


public class HttpRequest: HttpRequestRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$HttpRequest$_free(ptr)
        }
    }
}
public class HttpRequestRefMut: HttpRequestRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class HttpRequestRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension HttpRequest: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_HttpRequest$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_HttpRequest$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: HttpRequest) {
        __swift_bridge__$Vec_HttpRequest$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_HttpRequest$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (HttpRequest(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HttpRequestRef> {
        let pointer = __swift_bridge__$Vec_HttpRequest$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HttpRequestRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HttpRequestRefMut> {
        let pointer = __swift_bridge__$Vec_HttpRequest$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HttpRequestRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<HttpRequestRef> {
        UnsafePointer<HttpRequestRef>(OpaquePointer(__swift_bridge__$Vec_HttpRequest$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_HttpRequest$len(vecPtr)
    }
}


public class KnownPrincipalType: KnownPrincipalTypeRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$KnownPrincipalType$_free(ptr)
        }
    }
}
public class KnownPrincipalTypeRefMut: KnownPrincipalTypeRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class KnownPrincipalTypeRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension KnownPrincipalType: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_KnownPrincipalType$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_KnownPrincipalType$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: KnownPrincipalType) {
        __swift_bridge__$Vec_KnownPrincipalType$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_KnownPrincipalType$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (KnownPrincipalType(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<KnownPrincipalTypeRef> {
        let pointer = __swift_bridge__$Vec_KnownPrincipalType$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return KnownPrincipalTypeRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<KnownPrincipalTypeRefMut> {
        let pointer = __swift_bridge__$Vec_KnownPrincipalType$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return KnownPrincipalTypeRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<KnownPrincipalTypeRef> {
        UnsafePointer<KnownPrincipalTypeRef>(OpaquePointer(__swift_bridge__$Vec_KnownPrincipalType$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_KnownPrincipalType$len(vecPtr)
    }
}


public class Result17: Result17RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result17$_free(ptr)
        }
    }
}
public class Result17RefMut: Result17Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result17Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result17: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result17$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result17$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result17) {
        __swift_bridge__$Vec_Result17$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result17$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result17(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result17Ref> {
        let pointer = __swift_bridge__$Vec_Result17$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result17Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result17RefMut> {
        let pointer = __swift_bridge__$Vec_Result17$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result17RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result17Ref> {
        UnsafePointer<Result17Ref>(OpaquePointer(__swift_bridge__$Vec_Result17$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result17$len(vecPtr)
    }
}


public class WatchHistoryItem: WatchHistoryItemRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$WatchHistoryItem$_free(ptr)
        }
    }
}
public class WatchHistoryItemRefMut: WatchHistoryItemRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class WatchHistoryItemRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension WatchHistoryItem: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_WatchHistoryItem$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_WatchHistoryItem$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: WatchHistoryItem) {
        __swift_bridge__$Vec_WatchHistoryItem$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_WatchHistoryItem$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (WatchHistoryItem(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<WatchHistoryItemRef> {
        let pointer = __swift_bridge__$Vec_WatchHistoryItem$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return WatchHistoryItemRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<WatchHistoryItemRefMut> {
        let pointer = __swift_bridge__$Vec_WatchHistoryItem$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return WatchHistoryItemRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<WatchHistoryItemRef> {
        UnsafePointer<WatchHistoryItemRef>(OpaquePointer(__swift_bridge__$Vec_WatchHistoryItem$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_WatchHistoryItem$len(vecPtr)
    }
}


public class Result16: Result16RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result16$_free(ptr)
        }
    }
}
public class Result16RefMut: Result16Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result16Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result16: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result16$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result16$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result16) {
        __swift_bridge__$Vec_Result16$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result16$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result16(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result16Ref> {
        let pointer = __swift_bridge__$Vec_Result16$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result16Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result16RefMut> {
        let pointer = __swift_bridge__$Vec_Result16$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result16RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result16Ref> {
        UnsafePointer<Result16Ref>(OpaquePointer(__swift_bridge__$Vec_Result16$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result16$len(vecPtr)
    }
}


public class TokenEvent: TokenEventRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$TokenEvent$_free(ptr)
        }
    }
}
public class TokenEventRefMut: TokenEventRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class TokenEventRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension TokenEvent: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_TokenEvent$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_TokenEvent$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: TokenEvent) {
        __swift_bridge__$Vec_TokenEvent$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_TokenEvent$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (TokenEvent(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TokenEventRef> {
        let pointer = __swift_bridge__$Vec_TokenEvent$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TokenEventRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TokenEventRefMut> {
        let pointer = __swift_bridge__$Vec_TokenEvent$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TokenEventRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<TokenEventRef> {
        UnsafePointer<TokenEventRef>(OpaquePointer(__swift_bridge__$Vec_TokenEvent$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_TokenEvent$len(vecPtr)
    }
}


public class HotOrNotOutcomePayoutEvent: HotOrNotOutcomePayoutEventRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$HotOrNotOutcomePayoutEvent$_free(ptr)
        }
    }
}
public class HotOrNotOutcomePayoutEventRefMut: HotOrNotOutcomePayoutEventRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class HotOrNotOutcomePayoutEventRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension HotOrNotOutcomePayoutEvent: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: HotOrNotOutcomePayoutEvent) {
        __swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (HotOrNotOutcomePayoutEvent(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HotOrNotOutcomePayoutEventRef> {
        let pointer = __swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HotOrNotOutcomePayoutEventRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HotOrNotOutcomePayoutEventRefMut> {
        let pointer = __swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HotOrNotOutcomePayoutEventRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<HotOrNotOutcomePayoutEventRef> {
        UnsafePointer<HotOrNotOutcomePayoutEventRef>(OpaquePointer(__swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_HotOrNotOutcomePayoutEvent$len(vecPtr)
    }
}


public class MintEvent: MintEventRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$MintEvent$_free(ptr)
        }
    }
}
public class MintEventRefMut: MintEventRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class MintEventRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension MintEvent: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_MintEvent$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_MintEvent$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: MintEvent) {
        __swift_bridge__$Vec_MintEvent$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_MintEvent$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (MintEvent(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MintEventRef> {
        let pointer = __swift_bridge__$Vec_MintEvent$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MintEventRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MintEventRefMut> {
        let pointer = __swift_bridge__$Vec_MintEvent$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MintEventRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<MintEventRef> {
        UnsafePointer<MintEventRef>(OpaquePointer(__swift_bridge__$Vec_MintEvent$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_MintEvent$len(vecPtr)
    }
}


public class StakeEvent: StakeEventRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$StakeEvent$_free(ptr)
        }
    }
}
public class StakeEventRefMut: StakeEventRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class StakeEventRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension StakeEvent: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_StakeEvent$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_StakeEvent$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: StakeEvent) {
        __swift_bridge__$Vec_StakeEvent$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_StakeEvent$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (StakeEvent(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<StakeEventRef> {
        let pointer = __swift_bridge__$Vec_StakeEvent$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return StakeEventRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<StakeEventRefMut> {
        let pointer = __swift_bridge__$Vec_StakeEvent$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return StakeEventRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<StakeEventRef> {
        UnsafePointer<StakeEventRef>(OpaquePointer(__swift_bridge__$Vec_StakeEvent$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_StakeEvent$len(vecPtr)
    }
}


public class Result15: Result15RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result15$_free(ptr)
        }
    }
}
public class Result15RefMut: Result15Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result15Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result15: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result15$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result15$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result15) {
        __swift_bridge__$Vec_Result15$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result15$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result15(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result15Ref> {
        let pointer = __swift_bridge__$Vec_Result15$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result15Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result15RefMut> {
        let pointer = __swift_bridge__$Vec_Result15$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result15RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result15Ref> {
        UnsafePointer<Result15Ref>(OpaquePointer(__swift_bridge__$Vec_Result15$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result15$len(vecPtr)
    }
}


public class PaginationError: PaginationErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PaginationError$_free(ptr)
        }
    }
}
public class PaginationErrorRefMut: PaginationErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PaginationErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PaginationError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PaginationError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PaginationError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PaginationError) {
        __swift_bridge__$Vec_PaginationError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PaginationError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PaginationError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PaginationErrorRef> {
        let pointer = __swift_bridge__$Vec_PaginationError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PaginationErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PaginationErrorRefMut> {
        let pointer = __swift_bridge__$Vec_PaginationError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PaginationErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PaginationErrorRef> {
        UnsafePointer<PaginationErrorRef>(OpaquePointer(__swift_bridge__$Vec_PaginationError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PaginationError$len(vecPtr)
    }
}


public class Result14: Result14RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result14$_free(ptr)
        }
    }
}
public class Result14RefMut: Result14Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result14Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result14: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result14$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result14$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result14) {
        __swift_bridge__$Vec_Result14$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result14$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result14(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result14Ref> {
        let pointer = __swift_bridge__$Vec_Result14$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result14Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result14RefMut> {
        let pointer = __swift_bridge__$Vec_Result14$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result14RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result14Ref> {
        UnsafePointer<Result14Ref>(OpaquePointer(__swift_bridge__$Vec_Result14$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result14$len(vecPtr)
    }
}


public class SuccessHistoryItemV1: SuccessHistoryItemV1RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SuccessHistoryItemV1$_free(ptr)
        }
    }
}
public class SuccessHistoryItemV1RefMut: SuccessHistoryItemV1Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SuccessHistoryItemV1Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SuccessHistoryItemV1: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SuccessHistoryItemV1$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SuccessHistoryItemV1$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SuccessHistoryItemV1) {
        __swift_bridge__$Vec_SuccessHistoryItemV1$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SuccessHistoryItemV1$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SuccessHistoryItemV1(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SuccessHistoryItemV1Ref> {
        let pointer = __swift_bridge__$Vec_SuccessHistoryItemV1$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SuccessHistoryItemV1Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SuccessHistoryItemV1RefMut> {
        let pointer = __swift_bridge__$Vec_SuccessHistoryItemV1$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SuccessHistoryItemV1RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SuccessHistoryItemV1Ref> {
        UnsafePointer<SuccessHistoryItemV1Ref>(OpaquePointer(__swift_bridge__$Vec_SuccessHistoryItemV1$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SuccessHistoryItemV1$len(vecPtr)
    }
}


public class Result13: Result13RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result13$_free(ptr)
        }
    }
}
public class Result13RefMut: Result13Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result13Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result13: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result13$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result13$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result13) {
        __swift_bridge__$Vec_Result13$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result13$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result13(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result13Ref> {
        let pointer = __swift_bridge__$Vec_Result13$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result13Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result13RefMut> {
        let pointer = __swift_bridge__$Vec_Result13$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result13RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result13Ref> {
        UnsafePointer<Result13Ref>(OpaquePointer(__swift_bridge__$Vec_Result13$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result13$len(vecPtr)
    }
}


public class SessionType: SessionTypeRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SessionType$_free(ptr)
        }
    }
}
public class SessionTypeRefMut: SessionTypeRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SessionTypeRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SessionType: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SessionType$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SessionType$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SessionType) {
        __swift_bridge__$Vec_SessionType$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SessionType$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SessionType(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SessionTypeRef> {
        let pointer = __swift_bridge__$Vec_SessionType$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SessionTypeRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SessionTypeRefMut> {
        let pointer = __swift_bridge__$Vec_SessionType$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SessionTypeRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SessionTypeRef> {
        UnsafePointer<SessionTypeRef>(OpaquePointer(__swift_bridge__$Vec_SessionType$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SessionType$len(vecPtr)
    }
}


public class UserProfileDetailsForFrontendV2: UserProfileDetailsForFrontendV2RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UserProfileDetailsForFrontendV2$_free(ptr)
        }
    }
}
public class UserProfileDetailsForFrontendV2RefMut: UserProfileDetailsForFrontendV2Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UserProfileDetailsForFrontendV2Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UserProfileDetailsForFrontendV2: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UserProfileDetailsForFrontendV2$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UserProfileDetailsForFrontendV2$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UserProfileDetailsForFrontendV2) {
        __swift_bridge__$Vec_UserProfileDetailsForFrontendV2$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UserProfileDetailsForFrontendV2$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UserProfileDetailsForFrontendV2(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileDetailsForFrontendV2Ref> {
        let pointer = __swift_bridge__$Vec_UserProfileDetailsForFrontendV2$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileDetailsForFrontendV2Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileDetailsForFrontendV2RefMut> {
        let pointer = __swift_bridge__$Vec_UserProfileDetailsForFrontendV2$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileDetailsForFrontendV2RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UserProfileDetailsForFrontendV2Ref> {
        UnsafePointer<UserProfileDetailsForFrontendV2Ref>(OpaquePointer(__swift_bridge__$Vec_UserProfileDetailsForFrontendV2$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UserProfileDetailsForFrontendV2$len(vecPtr)
    }
}


public class MigrationInfo: MigrationInfoRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$MigrationInfo$_free(ptr)
        }
    }
}
public class MigrationInfoRefMut: MigrationInfoRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class MigrationInfoRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension MigrationInfo: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_MigrationInfo$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_MigrationInfo$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: MigrationInfo) {
        __swift_bridge__$Vec_MigrationInfo$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_MigrationInfo$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (MigrationInfo(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MigrationInfoRef> {
        let pointer = __swift_bridge__$Vec_MigrationInfo$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MigrationInfoRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MigrationInfoRefMut> {
        let pointer = __swift_bridge__$Vec_MigrationInfo$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MigrationInfoRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<MigrationInfoRef> {
        UnsafePointer<MigrationInfoRef>(OpaquePointer(__swift_bridge__$Vec_MigrationInfo$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_MigrationInfo$len(vecPtr)
    }
}


public class UserProfileDetailsForFrontend: UserProfileDetailsForFrontendRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UserProfileDetailsForFrontend$_free(ptr)
        }
    }
}
public class UserProfileDetailsForFrontendRefMut: UserProfileDetailsForFrontendRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UserProfileDetailsForFrontendRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UserProfileDetailsForFrontend: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UserProfileDetailsForFrontend$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UserProfileDetailsForFrontend$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UserProfileDetailsForFrontend) {
        __swift_bridge__$Vec_UserProfileDetailsForFrontend$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UserProfileDetailsForFrontend$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UserProfileDetailsForFrontend(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileDetailsForFrontendRef> {
        let pointer = __swift_bridge__$Vec_UserProfileDetailsForFrontend$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileDetailsForFrontendRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileDetailsForFrontendRefMut> {
        let pointer = __swift_bridge__$Vec_UserProfileDetailsForFrontend$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileDetailsForFrontendRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UserProfileDetailsForFrontendRef> {
        UnsafePointer<UserProfileDetailsForFrontendRef>(OpaquePointer(__swift_bridge__$Vec_UserProfileDetailsForFrontend$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UserProfileDetailsForFrontend$len(vecPtr)
    }
}


public class UserCanisterDetails: UserCanisterDetailsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UserCanisterDetails$_free(ptr)
        }
    }
}
public class UserCanisterDetailsRefMut: UserCanisterDetailsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UserCanisterDetailsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UserCanisterDetails: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UserCanisterDetails$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UserCanisterDetails$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UserCanisterDetails) {
        __swift_bridge__$Vec_UserCanisterDetails$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UserCanisterDetails$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UserCanisterDetails(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserCanisterDetailsRef> {
        let pointer = __swift_bridge__$Vec_UserCanisterDetails$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserCanisterDetailsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserCanisterDetailsRefMut> {
        let pointer = __swift_bridge__$Vec_UserCanisterDetails$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserCanisterDetailsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UserCanisterDetailsRef> {
        UnsafePointer<UserCanisterDetailsRef>(OpaquePointer(__swift_bridge__$Vec_UserCanisterDetails$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UserCanisterDetails$len(vecPtr)
    }
}


public class UserProfileGlobalStats: UserProfileGlobalStatsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UserProfileGlobalStats$_free(ptr)
        }
    }
}
public class UserProfileGlobalStatsRefMut: UserProfileGlobalStatsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UserProfileGlobalStatsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UserProfileGlobalStats: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UserProfileGlobalStats$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UserProfileGlobalStats$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UserProfileGlobalStats) {
        __swift_bridge__$Vec_UserProfileGlobalStats$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UserProfileGlobalStats$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UserProfileGlobalStats(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileGlobalStatsRef> {
        let pointer = __swift_bridge__$Vec_UserProfileGlobalStats$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileGlobalStatsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UserProfileGlobalStatsRefMut> {
        let pointer = __swift_bridge__$Vec_UserProfileGlobalStats$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UserProfileGlobalStatsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UserProfileGlobalStatsRef> {
        UnsafePointer<UserProfileGlobalStatsRef>(OpaquePointer(__swift_bridge__$Vec_UserProfileGlobalStats$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UserProfileGlobalStats$len(vecPtr)
    }
}


public class FollowEntry: FollowEntryRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$FollowEntry$_free(ptr)
        }
    }
}
public class FollowEntryRefMut: FollowEntryRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class FollowEntryRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension FollowEntry: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_FollowEntry$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_FollowEntry$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: FollowEntry) {
        __swift_bridge__$Vec_FollowEntry$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_FollowEntry$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (FollowEntry(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowEntryRef> {
        let pointer = __swift_bridge__$Vec_FollowEntry$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowEntryRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowEntryRefMut> {
        let pointer = __swift_bridge__$Vec_FollowEntry$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowEntryRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<FollowEntryRef> {
        UnsafePointer<FollowEntryRef>(OpaquePointer(__swift_bridge__$Vec_FollowEntry$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_FollowEntry$len(vecPtr)
    }
}


public class FollowEntryDetail: FollowEntryDetailRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$FollowEntryDetail$_free(ptr)
        }
    }
}
public class FollowEntryDetailRefMut: FollowEntryDetailRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class FollowEntryDetailRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension FollowEntryDetail: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_FollowEntryDetail$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_FollowEntryDetail$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: FollowEntryDetail) {
        __swift_bridge__$Vec_FollowEntryDetail$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_FollowEntryDetail$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (FollowEntryDetail(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowEntryDetailRef> {
        let pointer = __swift_bridge__$Vec_FollowEntryDetail$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowEntryDetailRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowEntryDetailRefMut> {
        let pointer = __swift_bridge__$Vec_FollowEntryDetail$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowEntryDetailRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<FollowEntryDetailRef> {
        UnsafePointer<FollowEntryDetailRef>(OpaquePointer(__swift_bridge__$Vec_FollowEntryDetail$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_FollowEntryDetail$len(vecPtr)
    }
}


public class Result12: Result12RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result12$_free(ptr)
        }
    }
}
public class Result12RefMut: Result12Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result12Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result12: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result12$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result12$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result12) {
        __swift_bridge__$Vec_Result12$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result12$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result12(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result12Ref> {
        let pointer = __swift_bridge__$Vec_Result12$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result12Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result12RefMut> {
        let pointer = __swift_bridge__$Vec_Result12$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result12RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result12Ref> {
        UnsafePointer<Result12Ref>(OpaquePointer(__swift_bridge__$Vec_Result12$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result12$len(vecPtr)
    }
}


public class GetPostsOfUserProfileError: GetPostsOfUserProfileErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$GetPostsOfUserProfileError$_free(ptr)
        }
    }
}
public class GetPostsOfUserProfileErrorRefMut: GetPostsOfUserProfileErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class GetPostsOfUserProfileErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension GetPostsOfUserProfileError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_GetPostsOfUserProfileError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_GetPostsOfUserProfileError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: GetPostsOfUserProfileError) {
        __swift_bridge__$Vec_GetPostsOfUserProfileError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_GetPostsOfUserProfileError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (GetPostsOfUserProfileError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<GetPostsOfUserProfileErrorRef> {
        let pointer = __swift_bridge__$Vec_GetPostsOfUserProfileError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return GetPostsOfUserProfileErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<GetPostsOfUserProfileErrorRefMut> {
        let pointer = __swift_bridge__$Vec_GetPostsOfUserProfileError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return GetPostsOfUserProfileErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<GetPostsOfUserProfileErrorRef> {
        UnsafePointer<GetPostsOfUserProfileErrorRef>(OpaquePointer(__swift_bridge__$Vec_GetPostsOfUserProfileError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_GetPostsOfUserProfileError$len(vecPtr)
    }
}


public class MlFeedCacheItem: MlFeedCacheItemRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$MlFeedCacheItem$_free(ptr)
        }
    }
}
public class MlFeedCacheItemRefMut: MlFeedCacheItemRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class MlFeedCacheItemRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension MlFeedCacheItem: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_MlFeedCacheItem$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_MlFeedCacheItem$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: MlFeedCacheItem) {
        __swift_bridge__$Vec_MlFeedCacheItem$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_MlFeedCacheItem$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (MlFeedCacheItem(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MlFeedCacheItemRef> {
        let pointer = __swift_bridge__$Vec_MlFeedCacheItem$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MlFeedCacheItemRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<MlFeedCacheItemRefMut> {
        let pointer = __swift_bridge__$Vec_MlFeedCacheItem$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return MlFeedCacheItemRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<MlFeedCacheItemRef> {
        UnsafePointer<MlFeedCacheItemRef>(OpaquePointer(__swift_bridge__$Vec_MlFeedCacheItem$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_MlFeedCacheItem$len(vecPtr)
    }
}


public class Result11: Result11RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result11$_free(ptr)
        }
    }
}
public class Result11RefMut: Result11Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result11Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result11: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result11$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result11$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result11) {
        __swift_bridge__$Vec_Result11$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result11$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result11(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result11Ref> {
        let pointer = __swift_bridge__$Vec_Result11$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result11Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result11RefMut> {
        let pointer = __swift_bridge__$Vec_Result11$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result11RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result11Ref> {
        UnsafePointer<Result11Ref>(OpaquePointer(__swift_bridge__$Vec_Result11$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result11$len(vecPtr)
    }
}


public class PostDetailsForFrontend: PostDetailsForFrontendRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostDetailsForFrontend$_free(ptr)
        }
    }
}
public class PostDetailsForFrontendRefMut: PostDetailsForFrontendRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostDetailsForFrontendRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostDetailsForFrontend: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostDetailsForFrontend$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostDetailsForFrontend$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostDetailsForFrontend) {
        __swift_bridge__$Vec_PostDetailsForFrontend$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostDetailsForFrontend$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostDetailsForFrontend(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostDetailsForFrontendRef> {
        let pointer = __swift_bridge__$Vec_PostDetailsForFrontend$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostDetailsForFrontendRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostDetailsForFrontendRefMut> {
        let pointer = __swift_bridge__$Vec_PostDetailsForFrontend$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostDetailsForFrontendRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostDetailsForFrontendRef> {
        UnsafePointer<PostDetailsForFrontendRef>(OpaquePointer(__swift_bridge__$Vec_PostDetailsForFrontend$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostDetailsForFrontend$len(vecPtr)
    }
}


public class PlacedBetDetail: PlacedBetDetailRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PlacedBetDetail$_free(ptr)
        }
    }
}
public class PlacedBetDetailRefMut: PlacedBetDetailRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PlacedBetDetailRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PlacedBetDetail: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PlacedBetDetail$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PlacedBetDetail$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PlacedBetDetail) {
        __swift_bridge__$Vec_PlacedBetDetail$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PlacedBetDetail$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PlacedBetDetail(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlacedBetDetailRef> {
        let pointer = __swift_bridge__$Vec_PlacedBetDetail$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlacedBetDetailRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlacedBetDetailRefMut> {
        let pointer = __swift_bridge__$Vec_PlacedBetDetail$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlacedBetDetailRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PlacedBetDetailRef> {
        UnsafePointer<PlacedBetDetailRef>(OpaquePointer(__swift_bridge__$Vec_PlacedBetDetail$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PlacedBetDetail$len(vecPtr)
    }
}


public class BetOutcomeForBetMaker: BetOutcomeForBetMakerRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$BetOutcomeForBetMaker$_free(ptr)
        }
    }
}
public class BetOutcomeForBetMakerRefMut: BetOutcomeForBetMakerRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class BetOutcomeForBetMakerRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension BetOutcomeForBetMaker: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_BetOutcomeForBetMaker$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_BetOutcomeForBetMaker$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: BetOutcomeForBetMaker) {
        __swift_bridge__$Vec_BetOutcomeForBetMaker$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_BetOutcomeForBetMaker$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (BetOutcomeForBetMaker(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetOutcomeForBetMakerRef> {
        let pointer = __swift_bridge__$Vec_BetOutcomeForBetMaker$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetOutcomeForBetMakerRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetOutcomeForBetMakerRefMut> {
        let pointer = __swift_bridge__$Vec_BetOutcomeForBetMaker$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetOutcomeForBetMakerRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<BetOutcomeForBetMakerRef> {
        UnsafePointer<BetOutcomeForBetMakerRef>(OpaquePointer(__swift_bridge__$Vec_BetOutcomeForBetMaker$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_BetOutcomeForBetMaker$len(vecPtr)
    }
}


public class Result10: Result10RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result10$_free(ptr)
        }
    }
}
public class Result10RefMut: Result10Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result10Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result10: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result10$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result10$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result10) {
        __swift_bridge__$Vec_Result10$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result10$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result10(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result10Ref> {
        let pointer = __swift_bridge__$Vec_Result10$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result10Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result10RefMut> {
        let pointer = __swift_bridge__$Vec_Result10$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result10RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result10Ref> {
        UnsafePointer<Result10Ref>(OpaquePointer(__swift_bridge__$Vec_Result10$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result10$len(vecPtr)
    }
}


public class Post: PostRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Post$_free(ptr)
        }
    }
}
public class PostRefMut: PostRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Post: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Post$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Post$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Post) {
        __swift_bridge__$Vec_Post$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Post$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Post(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostRef> {
        let pointer = __swift_bridge__$Vec_Post$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostRefMut> {
        let pointer = __swift_bridge__$Vec_Post$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostRef> {
        UnsafePointer<PostRef>(OpaquePointer(__swift_bridge__$Vec_Post$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Post$len(vecPtr)
    }
}


public class HotOrNotDetails: HotOrNotDetailsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$HotOrNotDetails$_free(ptr)
        }
    }
}
public class HotOrNotDetailsRefMut: HotOrNotDetailsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class HotOrNotDetailsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension HotOrNotDetails: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_HotOrNotDetails$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_HotOrNotDetails$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: HotOrNotDetails) {
        __swift_bridge__$Vec_HotOrNotDetails$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_HotOrNotDetails$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (HotOrNotDetails(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HotOrNotDetailsRef> {
        let pointer = __swift_bridge__$Vec_HotOrNotDetails$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HotOrNotDetailsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<HotOrNotDetailsRefMut> {
        let pointer = __swift_bridge__$Vec_HotOrNotDetails$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return HotOrNotDetailsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<HotOrNotDetailsRef> {
        UnsafePointer<HotOrNotDetailsRef>(OpaquePointer(__swift_bridge__$Vec_HotOrNotDetails$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_HotOrNotDetails$len(vecPtr)
    }
}


public class SlotDetails: SlotDetailsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SlotDetails$_free(ptr)
        }
    }
}
public class SlotDetailsRefMut: SlotDetailsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SlotDetailsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SlotDetails: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SlotDetails$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SlotDetails$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SlotDetails) {
        __swift_bridge__$Vec_SlotDetails$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SlotDetails$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SlotDetails(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SlotDetailsRef> {
        let pointer = __swift_bridge__$Vec_SlotDetails$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SlotDetailsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SlotDetailsRefMut> {
        let pointer = __swift_bridge__$Vec_SlotDetails$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SlotDetailsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SlotDetailsRef> {
        UnsafePointer<SlotDetailsRef>(OpaquePointer(__swift_bridge__$Vec_SlotDetails$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SlotDetails$len(vecPtr)
    }
}


public class RoomDetails: RoomDetailsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$RoomDetails$_free(ptr)
        }
    }
}
public class RoomDetailsRefMut: RoomDetailsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class RoomDetailsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension RoomDetails: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_RoomDetails$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_RoomDetails$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: RoomDetails) {
        __swift_bridge__$Vec_RoomDetails$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_RoomDetails$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (RoomDetails(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<RoomDetailsRef> {
        let pointer = __swift_bridge__$Vec_RoomDetails$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return RoomDetailsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<RoomDetailsRefMut> {
        let pointer = __swift_bridge__$Vec_RoomDetails$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return RoomDetailsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<RoomDetailsRef> {
        UnsafePointer<RoomDetailsRef>(OpaquePointer(__swift_bridge__$Vec_RoomDetails$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_RoomDetails$len(vecPtr)
    }
}


public class RoomBetPossibleOutcomes: RoomBetPossibleOutcomesRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$RoomBetPossibleOutcomes$_free(ptr)
        }
    }
}
public class RoomBetPossibleOutcomesRefMut: RoomBetPossibleOutcomesRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class RoomBetPossibleOutcomesRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension RoomBetPossibleOutcomes: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_RoomBetPossibleOutcomes$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_RoomBetPossibleOutcomes$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: RoomBetPossibleOutcomes) {
        __swift_bridge__$Vec_RoomBetPossibleOutcomes$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_RoomBetPossibleOutcomes$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (RoomBetPossibleOutcomes(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<RoomBetPossibleOutcomesRef> {
        let pointer = __swift_bridge__$Vec_RoomBetPossibleOutcomes$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return RoomBetPossibleOutcomesRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<RoomBetPossibleOutcomesRefMut> {
        let pointer = __swift_bridge__$Vec_RoomBetPossibleOutcomes$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return RoomBetPossibleOutcomesRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<RoomBetPossibleOutcomesRef> {
        UnsafePointer<RoomBetPossibleOutcomesRef>(OpaquePointer(__swift_bridge__$Vec_RoomBetPossibleOutcomes$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_RoomBetPossibleOutcomes$len(vecPtr)
    }
}


public class AggregateStats: AggregateStatsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$AggregateStats$_free(ptr)
        }
    }
}
public class AggregateStatsRefMut: AggregateStatsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class AggregateStatsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension AggregateStats: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_AggregateStats$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_AggregateStats$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: AggregateStats) {
        __swift_bridge__$Vec_AggregateStats$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_AggregateStats$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (AggregateStats(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<AggregateStatsRef> {
        let pointer = __swift_bridge__$Vec_AggregateStats$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return AggregateStatsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<AggregateStatsRefMut> {
        let pointer = __swift_bridge__$Vec_AggregateStats$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return AggregateStatsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<AggregateStatsRef> {
        UnsafePointer<AggregateStatsRef>(OpaquePointer(__swift_bridge__$Vec_AggregateStats$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_AggregateStats$len(vecPtr)
    }
}


public class PostViewStatistics: PostViewStatisticsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostViewStatistics$_free(ptr)
        }
    }
}
public class PostViewStatisticsRefMut: PostViewStatisticsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostViewStatisticsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostViewStatistics: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostViewStatistics$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostViewStatistics$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostViewStatistics) {
        __swift_bridge__$Vec_PostViewStatistics$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostViewStatistics$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostViewStatistics(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostViewStatisticsRef> {
        let pointer = __swift_bridge__$Vec_PostViewStatistics$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostViewStatisticsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostViewStatisticsRefMut> {
        let pointer = __swift_bridge__$Vec_PostViewStatistics$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostViewStatisticsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostViewStatisticsRef> {
        UnsafePointer<PostViewStatisticsRef>(OpaquePointer(__swift_bridge__$Vec_PostViewStatistics$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostViewStatistics$len(vecPtr)
    }
}


public class FeedScore: FeedScoreRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$FeedScore$_free(ptr)
        }
    }
}
public class FeedScoreRefMut: FeedScoreRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class FeedScoreRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension FeedScore: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_FeedScore$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_FeedScore$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: FeedScore) {
        __swift_bridge__$Vec_FeedScore$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_FeedScore$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (FeedScore(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FeedScoreRef> {
        let pointer = __swift_bridge__$Vec_FeedScore$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FeedScoreRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FeedScoreRefMut> {
        let pointer = __swift_bridge__$Vec_FeedScore$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FeedScoreRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<FeedScoreRef> {
        UnsafePointer<FeedScoreRef>(OpaquePointer(__swift_bridge__$Vec_FeedScore$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_FeedScore$len(vecPtr)
    }
}


public class PostStatus: PostStatusRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostStatus$_free(ptr)
        }
    }
}
public class PostStatusRefMut: PostStatusRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostStatusRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostStatus: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostStatus$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostStatus$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostStatus) {
        __swift_bridge__$Vec_PostStatus$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostStatus$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostStatus(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostStatusRef> {
        let pointer = __swift_bridge__$Vec_PostStatus$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostStatusRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostStatusRefMut> {
        let pointer = __swift_bridge__$Vec_PostStatus$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostStatusRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostStatusRef> {
        UnsafePointer<PostStatusRef>(OpaquePointer(__swift_bridge__$Vec_PostStatus$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostStatus$len(vecPtr)
    }
}


public class DeviceIdentity: DeviceIdentityRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$DeviceIdentity$_free(ptr)
        }
    }
}
public class DeviceIdentityRefMut: DeviceIdentityRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class DeviceIdentityRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension DeviceIdentity: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_DeviceIdentity$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_DeviceIdentity$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: DeviceIdentity) {
        __swift_bridge__$Vec_DeviceIdentity$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_DeviceIdentity$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (DeviceIdentity(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DeviceIdentityRef> {
        let pointer = __swift_bridge__$Vec_DeviceIdentity$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DeviceIdentityRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DeviceIdentityRefMut> {
        let pointer = __swift_bridge__$Vec_DeviceIdentity$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DeviceIdentityRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<DeviceIdentityRef> {
        UnsafePointer<DeviceIdentityRef>(OpaquePointer(__swift_bridge__$Vec_DeviceIdentity$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_DeviceIdentity$len(vecPtr)
    }
}


public class Result9: Result9RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result9$_free(ptr)
        }
    }
}
public class Result9RefMut: Result9Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result9Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result9: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result9$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result9$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result9) {
        __swift_bridge__$Vec_Result9$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result9$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result9(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result9Ref> {
        let pointer = __swift_bridge__$Vec_Result9$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result9Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result9RefMut> {
        let pointer = __swift_bridge__$Vec_Result9$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result9RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result9Ref> {
        UnsafePointer<Result9Ref>(OpaquePointer(__swift_bridge__$Vec_Result9$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result9$len(vecPtr)
    }
}


public class BetDetails: BetDetailsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$BetDetails$_free(ptr)
        }
    }
}
public class BetDetailsRefMut: BetDetailsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class BetDetailsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension BetDetails: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_BetDetails$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_BetDetails$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: BetDetails) {
        __swift_bridge__$Vec_BetDetails$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_BetDetails$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (BetDetails(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetDetailsRef> {
        let pointer = __swift_bridge__$Vec_BetDetails$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetDetailsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetDetailsRefMut> {
        let pointer = __swift_bridge__$Vec_BetDetails$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetDetailsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<BetDetailsRef> {
        UnsafePointer<BetDetailsRef>(OpaquePointer(__swift_bridge__$Vec_BetDetails$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_BetDetails$len(vecPtr)
    }
}


public class BetPayout: BetPayoutRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$BetPayout$_free(ptr)
        }
    }
}
public class BetPayoutRefMut: BetPayoutRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class BetPayoutRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension BetPayout: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_BetPayout$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_BetPayout$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: BetPayout) {
        __swift_bridge__$Vec_BetPayout$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_BetPayout$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (BetPayout(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetPayoutRef> {
        let pointer = __swift_bridge__$Vec_BetPayout$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetPayoutRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetPayoutRefMut> {
        let pointer = __swift_bridge__$Vec_BetPayout$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetPayoutRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<BetPayoutRef> {
        UnsafePointer<BetPayoutRef>(OpaquePointer(__swift_bridge__$Vec_BetPayout$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_BetPayout$len(vecPtr)
    }
}


public class BetMakerInformedStatus: BetMakerInformedStatusRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$BetMakerInformedStatus$_free(ptr)
        }
    }
}
public class BetMakerInformedStatusRefMut: BetMakerInformedStatusRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class BetMakerInformedStatusRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension BetMakerInformedStatus: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_BetMakerInformedStatus$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_BetMakerInformedStatus$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: BetMakerInformedStatus) {
        __swift_bridge__$Vec_BetMakerInformedStatus$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_BetMakerInformedStatus$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (BetMakerInformedStatus(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetMakerInformedStatusRef> {
        let pointer = __swift_bridge__$Vec_BetMakerInformedStatus$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetMakerInformedStatusRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetMakerInformedStatusRefMut> {
        let pointer = __swift_bridge__$Vec_BetMakerInformedStatus$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetMakerInformedStatusRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<BetMakerInformedStatusRef> {
        UnsafePointer<BetMakerInformedStatusRef>(OpaquePointer(__swift_bridge__$Vec_BetMakerInformedStatus$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_BetMakerInformedStatus$len(vecPtr)
    }
}


public class Result8: Result8RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result8$_free(ptr)
        }
    }
}
public class Result8RefMut: Result8Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result8Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result8: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result8$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result8$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result8) {
        __swift_bridge__$Vec_Result8$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result8$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result8(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result8Ref> {
        let pointer = __swift_bridge__$Vec_Result8$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result8Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result8RefMut> {
        let pointer = __swift_bridge__$Vec_Result8$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result8RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result8Ref> {
        UnsafePointer<Result8Ref>(OpaquePointer(__swift_bridge__$Vec_Result8$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result8$len(vecPtr)
    }
}


public class FollowAnotherUserProfileError: FollowAnotherUserProfileErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$FollowAnotherUserProfileError$_free(ptr)
        }
    }
}
public class FollowAnotherUserProfileErrorRefMut: FollowAnotherUserProfileErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class FollowAnotherUserProfileErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension FollowAnotherUserProfileError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_FollowAnotherUserProfileError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_FollowAnotherUserProfileError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: FollowAnotherUserProfileError) {
        __swift_bridge__$Vec_FollowAnotherUserProfileError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_FollowAnotherUserProfileError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (FollowAnotherUserProfileError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowAnotherUserProfileErrorRef> {
        let pointer = __swift_bridge__$Vec_FollowAnotherUserProfileError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowAnotherUserProfileErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FollowAnotherUserProfileErrorRefMut> {
        let pointer = __swift_bridge__$Vec_FollowAnotherUserProfileError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FollowAnotherUserProfileErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<FollowAnotherUserProfileErrorRef> {
        UnsafePointer<FollowAnotherUserProfileErrorRef>(OpaquePointer(__swift_bridge__$Vec_FollowAnotherUserProfileError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_FollowAnotherUserProfileError$len(vecPtr)
    }
}


public class FolloweeArg: FolloweeArgRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$FolloweeArg$_free(ptr)
        }
    }
}
public class FolloweeArgRefMut: FolloweeArgRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class FolloweeArgRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension FolloweeArg: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_FolloweeArg$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_FolloweeArg$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: FolloweeArg) {
        __swift_bridge__$Vec_FolloweeArg$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_FolloweeArg$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (FolloweeArg(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FolloweeArgRef> {
        let pointer = __swift_bridge__$Vec_FolloweeArg$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FolloweeArgRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FolloweeArgRefMut> {
        let pointer = __swift_bridge__$Vec_FolloweeArg$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FolloweeArgRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<FolloweeArgRef> {
        UnsafePointer<FolloweeArgRef>(OpaquePointer(__swift_bridge__$Vec_FolloweeArg$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_FolloweeArg$len(vecPtr)
    }
}


public class Result7: Result7RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result7$_free(ptr)
        }
    }
}
public class Result7RefMut: Result7Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result7Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result7: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result7$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result7$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result7) {
        __swift_bridge__$Vec_Result7$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result7$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result7(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result7Ref> {
        let pointer = __swift_bridge__$Vec_Result7$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result7Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result7RefMut> {
        let pointer = __swift_bridge__$Vec_Result7$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result7RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result7Ref> {
        UnsafePointer<Result7Ref>(OpaquePointer(__swift_bridge__$Vec_Result7$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result7$len(vecPtr)
    }
}


public class CdaoDeployError: CdaoDeployErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$CdaoDeployError$_free(ptr)
        }
    }
}
public class CdaoDeployErrorRefMut: CdaoDeployErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CdaoDeployErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension CdaoDeployError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_CdaoDeployError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_CdaoDeployError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: CdaoDeployError) {
        __swift_bridge__$Vec_CdaoDeployError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_CdaoDeployError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (CdaoDeployError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CdaoDeployErrorRef> {
        let pointer = __swift_bridge__$Vec_CdaoDeployError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CdaoDeployErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CdaoDeployErrorRefMut> {
        let pointer = __swift_bridge__$Vec_CdaoDeployError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CdaoDeployErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CdaoDeployErrorRef> {
        UnsafePointer<CdaoDeployErrorRef>(OpaquePointer(__swift_bridge__$Vec_CdaoDeployError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_CdaoDeployError$len(vecPtr)
    }
}


public class DeployedCdaoCanisters: DeployedCdaoCanistersRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$DeployedCdaoCanisters$_free(ptr)
        }
    }
}
public class DeployedCdaoCanistersRefMut: DeployedCdaoCanistersRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class DeployedCdaoCanistersRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension DeployedCdaoCanisters: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_DeployedCdaoCanisters$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_DeployedCdaoCanisters$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: DeployedCdaoCanisters) {
        __swift_bridge__$Vec_DeployedCdaoCanisters$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_DeployedCdaoCanisters$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (DeployedCdaoCanisters(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DeployedCdaoCanistersRef> {
        let pointer = __swift_bridge__$Vec_DeployedCdaoCanisters$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DeployedCdaoCanistersRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DeployedCdaoCanistersRefMut> {
        let pointer = __swift_bridge__$Vec_DeployedCdaoCanisters$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DeployedCdaoCanistersRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<DeployedCdaoCanistersRef> {
        UnsafePointer<DeployedCdaoCanistersRef>(OpaquePointer(__swift_bridge__$Vec_DeployedCdaoCanisters$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_DeployedCdaoCanisters$len(vecPtr)
    }
}


public class SnsInitPayload: SnsInitPayloadRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SnsInitPayload$_free(ptr)
        }
    }
}
public class SnsInitPayloadRefMut: SnsInitPayloadRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SnsInitPayloadRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SnsInitPayload: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SnsInitPayload$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SnsInitPayload$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SnsInitPayload) {
        __swift_bridge__$Vec_SnsInitPayload$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SnsInitPayload$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SnsInitPayload(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SnsInitPayloadRef> {
        let pointer = __swift_bridge__$Vec_SnsInitPayload$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SnsInitPayloadRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SnsInitPayloadRefMut> {
        let pointer = __swift_bridge__$Vec_SnsInitPayload$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SnsInitPayloadRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SnsInitPayloadRef> {
        UnsafePointer<SnsInitPayloadRef>(OpaquePointer(__swift_bridge__$Vec_SnsInitPayload$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SnsInitPayload$len(vecPtr)
    }
}


public class Countries: CountriesRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Countries$_free(ptr)
        }
    }
}
public class CountriesRefMut: CountriesRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CountriesRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Countries: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Countries$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Countries$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Countries) {
        __swift_bridge__$Vec_Countries$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Countries$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Countries(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CountriesRef> {
        let pointer = __swift_bridge__$Vec_Countries$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CountriesRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CountriesRefMut> {
        let pointer = __swift_bridge__$Vec_Countries$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CountriesRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CountriesRef> {
        UnsafePointer<CountriesRef>(OpaquePointer(__swift_bridge__$Vec_Countries$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Countries$len(vecPtr)
    }
}


public class InitialTokenDistribution: InitialTokenDistributionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$InitialTokenDistribution$_free(ptr)
        }
    }
}
public class InitialTokenDistributionRefMut: InitialTokenDistributionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class InitialTokenDistributionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension InitialTokenDistribution: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_InitialTokenDistribution$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_InitialTokenDistribution$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: InitialTokenDistribution) {
        __swift_bridge__$Vec_InitialTokenDistribution$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_InitialTokenDistribution$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (InitialTokenDistribution(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<InitialTokenDistributionRef> {
        let pointer = __swift_bridge__$Vec_InitialTokenDistribution$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return InitialTokenDistributionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<InitialTokenDistributionRefMut> {
        let pointer = __swift_bridge__$Vec_InitialTokenDistribution$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return InitialTokenDistributionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<InitialTokenDistributionRef> {
        UnsafePointer<InitialTokenDistributionRef>(OpaquePointer(__swift_bridge__$Vec_InitialTokenDistribution$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_InitialTokenDistribution$len(vecPtr)
    }
}


public class FractionalDeveloperVotingPower: FractionalDeveloperVotingPowerRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$FractionalDeveloperVotingPower$_free(ptr)
        }
    }
}
public class FractionalDeveloperVotingPowerRefMut: FractionalDeveloperVotingPowerRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class FractionalDeveloperVotingPowerRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension FractionalDeveloperVotingPower: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_FractionalDeveloperVotingPower$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_FractionalDeveloperVotingPower$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: FractionalDeveloperVotingPower) {
        __swift_bridge__$Vec_FractionalDeveloperVotingPower$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_FractionalDeveloperVotingPower$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (FractionalDeveloperVotingPower(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FractionalDeveloperVotingPowerRef> {
        let pointer = __swift_bridge__$Vec_FractionalDeveloperVotingPower$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FractionalDeveloperVotingPowerRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<FractionalDeveloperVotingPowerRefMut> {
        let pointer = __swift_bridge__$Vec_FractionalDeveloperVotingPower$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return FractionalDeveloperVotingPowerRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<FractionalDeveloperVotingPowerRef> {
        UnsafePointer<FractionalDeveloperVotingPowerRef>(OpaquePointer(__swift_bridge__$Vec_FractionalDeveloperVotingPower$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_FractionalDeveloperVotingPower$len(vecPtr)
    }
}


public class SwapDistribution: SwapDistributionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SwapDistribution$_free(ptr)
        }
    }
}
public class SwapDistributionRefMut: SwapDistributionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SwapDistributionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SwapDistribution: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SwapDistribution$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SwapDistribution$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SwapDistribution) {
        __swift_bridge__$Vec_SwapDistribution$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SwapDistribution$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SwapDistribution(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SwapDistributionRef> {
        let pointer = __swift_bridge__$Vec_SwapDistribution$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SwapDistributionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SwapDistributionRefMut> {
        let pointer = __swift_bridge__$Vec_SwapDistribution$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SwapDistributionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SwapDistributionRef> {
        UnsafePointer<SwapDistributionRef>(OpaquePointer(__swift_bridge__$Vec_SwapDistribution$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SwapDistribution$len(vecPtr)
    }
}


public class AirdropDistribution: AirdropDistributionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$AirdropDistribution$_free(ptr)
        }
    }
}
public class AirdropDistributionRefMut: AirdropDistributionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class AirdropDistributionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension AirdropDistribution: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_AirdropDistribution$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_AirdropDistribution$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: AirdropDistribution) {
        __swift_bridge__$Vec_AirdropDistribution$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_AirdropDistribution$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (AirdropDistribution(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<AirdropDistributionRef> {
        let pointer = __swift_bridge__$Vec_AirdropDistribution$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return AirdropDistributionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<AirdropDistributionRefMut> {
        let pointer = __swift_bridge__$Vec_AirdropDistribution$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return AirdropDistributionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<AirdropDistributionRef> {
        UnsafePointer<AirdropDistributionRef>(OpaquePointer(__swift_bridge__$Vec_AirdropDistribution$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_AirdropDistribution$len(vecPtr)
    }
}


public class DeveloperDistribution: DeveloperDistributionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$DeveloperDistribution$_free(ptr)
        }
    }
}
public class DeveloperDistributionRefMut: DeveloperDistributionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class DeveloperDistributionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension DeveloperDistribution: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_DeveloperDistribution$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_DeveloperDistribution$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: DeveloperDistribution) {
        __swift_bridge__$Vec_DeveloperDistribution$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_DeveloperDistribution$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (DeveloperDistribution(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DeveloperDistributionRef> {
        let pointer = __swift_bridge__$Vec_DeveloperDistribution$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DeveloperDistributionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DeveloperDistributionRefMut> {
        let pointer = __swift_bridge__$Vec_DeveloperDistribution$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DeveloperDistributionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<DeveloperDistributionRef> {
        UnsafePointer<DeveloperDistributionRef>(OpaquePointer(__swift_bridge__$Vec_DeveloperDistribution$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_DeveloperDistribution$len(vecPtr)
    }
}


public class NeuronDistribution: NeuronDistributionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NeuronDistribution$_free(ptr)
        }
    }
}
public class NeuronDistributionRefMut: NeuronDistributionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NeuronDistributionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NeuronDistribution: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NeuronDistribution$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NeuronDistribution$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NeuronDistribution) {
        __swift_bridge__$Vec_NeuronDistribution$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NeuronDistribution$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NeuronDistribution(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronDistributionRef> {
        let pointer = __swift_bridge__$Vec_NeuronDistribution$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronDistributionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronDistributionRefMut> {
        let pointer = __swift_bridge__$Vec_NeuronDistribution$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronDistributionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NeuronDistributionRef> {
        UnsafePointer<NeuronDistributionRef>(OpaquePointer(__swift_bridge__$Vec_NeuronDistribution$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NeuronDistribution$len(vecPtr)
    }
}


public class TreasuryDistribution: TreasuryDistributionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$TreasuryDistribution$_free(ptr)
        }
    }
}
public class TreasuryDistributionRefMut: TreasuryDistributionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class TreasuryDistributionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension TreasuryDistribution: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_TreasuryDistribution$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_TreasuryDistribution$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: TreasuryDistribution) {
        __swift_bridge__$Vec_TreasuryDistribution$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_TreasuryDistribution$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (TreasuryDistribution(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TreasuryDistributionRef> {
        let pointer = __swift_bridge__$Vec_TreasuryDistribution$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TreasuryDistributionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TreasuryDistributionRefMut> {
        let pointer = __swift_bridge__$Vec_TreasuryDistribution$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TreasuryDistributionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<TreasuryDistributionRef> {
        UnsafePointer<TreasuryDistributionRef>(OpaquePointer(__swift_bridge__$Vec_TreasuryDistribution$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_TreasuryDistribution$len(vecPtr)
    }
}


public class NeuronsFundParticipants: NeuronsFundParticipantsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NeuronsFundParticipants$_free(ptr)
        }
    }
}
public class NeuronsFundParticipantsRefMut: NeuronsFundParticipantsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NeuronsFundParticipantsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NeuronsFundParticipants: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NeuronsFundParticipants$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NeuronsFundParticipants$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NeuronsFundParticipants) {
        __swift_bridge__$Vec_NeuronsFundParticipants$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NeuronsFundParticipants$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NeuronsFundParticipants(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronsFundParticipantsRef> {
        let pointer = __swift_bridge__$Vec_NeuronsFundParticipants$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronsFundParticipantsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronsFundParticipantsRefMut> {
        let pointer = __swift_bridge__$Vec_NeuronsFundParticipants$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronsFundParticipantsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NeuronsFundParticipantsRef> {
        UnsafePointer<NeuronsFundParticipantsRef>(OpaquePointer(__swift_bridge__$Vec_NeuronsFundParticipants$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NeuronsFundParticipants$len(vecPtr)
    }
}


public class CfParticipant: CfParticipantRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$CfParticipant$_free(ptr)
        }
    }
}
public class CfParticipantRefMut: CfParticipantRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CfParticipantRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension CfParticipant: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_CfParticipant$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_CfParticipant$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: CfParticipant) {
        __swift_bridge__$Vec_CfParticipant$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_CfParticipant$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (CfParticipant(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CfParticipantRef> {
        let pointer = __swift_bridge__$Vec_CfParticipant$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CfParticipantRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CfParticipantRefMut> {
        let pointer = __swift_bridge__$Vec_CfParticipant$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CfParticipantRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CfParticipantRef> {
        UnsafePointer<CfParticipantRef>(OpaquePointer(__swift_bridge__$Vec_CfParticipant$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_CfParticipant$len(vecPtr)
    }
}


public class CfNeuron: CfNeuronRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$CfNeuron$_free(ptr)
        }
    }
}
public class CfNeuronRefMut: CfNeuronRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CfNeuronRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension CfNeuron: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_CfNeuron$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_CfNeuron$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: CfNeuron) {
        __swift_bridge__$Vec_CfNeuron$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_CfNeuron$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (CfNeuron(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CfNeuronRef> {
        let pointer = __swift_bridge__$Vec_CfNeuron$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CfNeuronRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CfNeuronRefMut> {
        let pointer = __swift_bridge__$Vec_CfNeuron$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CfNeuronRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CfNeuronRef> {
        UnsafePointer<CfNeuronRef>(OpaquePointer(__swift_bridge__$Vec_CfNeuron$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_CfNeuron$len(vecPtr)
    }
}


public class NeuronsFundParticipationConstraints: NeuronsFundParticipationConstraintsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NeuronsFundParticipationConstraints$_free(ptr)
        }
    }
}
public class NeuronsFundParticipationConstraintsRefMut: NeuronsFundParticipationConstraintsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NeuronsFundParticipationConstraintsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NeuronsFundParticipationConstraints: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NeuronsFundParticipationConstraints$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NeuronsFundParticipationConstraints$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NeuronsFundParticipationConstraints) {
        __swift_bridge__$Vec_NeuronsFundParticipationConstraints$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NeuronsFundParticipationConstraints$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NeuronsFundParticipationConstraints(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronsFundParticipationConstraintsRef> {
        let pointer = __swift_bridge__$Vec_NeuronsFundParticipationConstraints$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronsFundParticipationConstraintsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronsFundParticipationConstraintsRefMut> {
        let pointer = __swift_bridge__$Vec_NeuronsFundParticipationConstraints$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronsFundParticipationConstraintsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NeuronsFundParticipationConstraintsRef> {
        UnsafePointer<NeuronsFundParticipationConstraintsRef>(OpaquePointer(__swift_bridge__$Vec_NeuronsFundParticipationConstraints$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NeuronsFundParticipationConstraints$len(vecPtr)
    }
}


public class IdealMatchedParticipationFunction: IdealMatchedParticipationFunctionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$IdealMatchedParticipationFunction$_free(ptr)
        }
    }
}
public class IdealMatchedParticipationFunctionRefMut: IdealMatchedParticipationFunctionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class IdealMatchedParticipationFunctionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension IdealMatchedParticipationFunction: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_IdealMatchedParticipationFunction$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_IdealMatchedParticipationFunction$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: IdealMatchedParticipationFunction) {
        __swift_bridge__$Vec_IdealMatchedParticipationFunction$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_IdealMatchedParticipationFunction$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (IdealMatchedParticipationFunction(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<IdealMatchedParticipationFunctionRef> {
        let pointer = __swift_bridge__$Vec_IdealMatchedParticipationFunction$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return IdealMatchedParticipationFunctionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<IdealMatchedParticipationFunctionRefMut> {
        let pointer = __swift_bridge__$Vec_IdealMatchedParticipationFunction$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return IdealMatchedParticipationFunctionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<IdealMatchedParticipationFunctionRef> {
        UnsafePointer<IdealMatchedParticipationFunctionRef>(OpaquePointer(__swift_bridge__$Vec_IdealMatchedParticipationFunction$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_IdealMatchedParticipationFunction$len(vecPtr)
    }
}


public class LinearScalingCoefficient: LinearScalingCoefficientRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$LinearScalingCoefficient$_free(ptr)
        }
    }
}
public class LinearScalingCoefficientRefMut: LinearScalingCoefficientRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class LinearScalingCoefficientRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension LinearScalingCoefficient: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_LinearScalingCoefficient$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_LinearScalingCoefficient$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: LinearScalingCoefficient) {
        __swift_bridge__$Vec_LinearScalingCoefficient$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_LinearScalingCoefficient$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (LinearScalingCoefficient(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<LinearScalingCoefficientRef> {
        let pointer = __swift_bridge__$Vec_LinearScalingCoefficient$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return LinearScalingCoefficientRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<LinearScalingCoefficientRefMut> {
        let pointer = __swift_bridge__$Vec_LinearScalingCoefficient$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return LinearScalingCoefficientRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<LinearScalingCoefficientRef> {
        UnsafePointer<LinearScalingCoefficientRef>(OpaquePointer(__swift_bridge__$Vec_LinearScalingCoefficient$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_LinearScalingCoefficient$len(vecPtr)
    }
}


public class DappCanisters: DappCanistersRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$DappCanisters$_free(ptr)
        }
    }
}
public class DappCanistersRefMut: DappCanistersRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class DappCanistersRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension DappCanisters: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_DappCanisters$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_DappCanisters$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: DappCanisters) {
        __swift_bridge__$Vec_DappCanisters$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_DappCanisters$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (DappCanisters(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DappCanistersRef> {
        let pointer = __swift_bridge__$Vec_DappCanisters$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DappCanistersRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DappCanistersRefMut> {
        let pointer = __swift_bridge__$Vec_DappCanisters$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DappCanistersRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<DappCanistersRef> {
        UnsafePointer<DappCanistersRef>(OpaquePointer(__swift_bridge__$Vec_DappCanisters$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_DappCanisters$len(vecPtr)
    }
}


public class Canister: CanisterRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Canister$_free(ptr)
        }
    }
}
public class CanisterRefMut: CanisterRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CanisterRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Canister: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Canister$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Canister$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Canister) {
        __swift_bridge__$Vec_Canister$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Canister$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Canister(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CanisterRef> {
        let pointer = __swift_bridge__$Vec_Canister$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CanisterRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CanisterRefMut> {
        let pointer = __swift_bridge__$Vec_Canister$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CanisterRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CanisterRef> {
        UnsafePointer<CanisterRef>(OpaquePointer(__swift_bridge__$Vec_Canister$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Canister$len(vecPtr)
    }
}


public class NeuronBasketConstructionParameters: NeuronBasketConstructionParametersRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NeuronBasketConstructionParameters$_free(ptr)
        }
    }
}
public class NeuronBasketConstructionParametersRefMut: NeuronBasketConstructionParametersRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NeuronBasketConstructionParametersRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NeuronBasketConstructionParameters: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NeuronBasketConstructionParameters$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NeuronBasketConstructionParameters$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NeuronBasketConstructionParameters) {
        __swift_bridge__$Vec_NeuronBasketConstructionParameters$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NeuronBasketConstructionParameters$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NeuronBasketConstructionParameters(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronBasketConstructionParametersRef> {
        let pointer = __swift_bridge__$Vec_NeuronBasketConstructionParameters$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronBasketConstructionParametersRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NeuronBasketConstructionParametersRefMut> {
        let pointer = __swift_bridge__$Vec_NeuronBasketConstructionParameters$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NeuronBasketConstructionParametersRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NeuronBasketConstructionParametersRef> {
        UnsafePointer<NeuronBasketConstructionParametersRef>(OpaquePointer(__swift_bridge__$Vec_NeuronBasketConstructionParameters$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NeuronBasketConstructionParameters$len(vecPtr)
    }
}


public class Result6: Result6RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result6$_free(ptr)
        }
    }
}
public class Result6RefMut: Result6Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result6Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result6: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result6$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result6$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result6) {
        __swift_bridge__$Vec_Result6$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result6$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result6(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result6Ref> {
        let pointer = __swift_bridge__$Vec_Result6$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result6Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result6RefMut> {
        let pointer = __swift_bridge__$Vec_Result6$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result6RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result6Ref> {
        UnsafePointer<Result6Ref>(OpaquePointer(__swift_bridge__$Vec_Result6$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result6$len(vecPtr)
    }
}


public class Result5: Result5RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result5$_free(ptr)
        }
    }
}
public class Result5RefMut: Result5Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result5Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result5: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result5$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result5$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result5) {
        __swift_bridge__$Vec_Result5$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result5$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result5(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result5Ref> {
        let pointer = __swift_bridge__$Vec_Result5$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result5Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result5RefMut> {
        let pointer = __swift_bridge__$Vec_Result5$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result5RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result5Ref> {
        UnsafePointer<Result5Ref>(OpaquePointer(__swift_bridge__$Vec_Result5$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result5$len(vecPtr)
    }
}


public class Result4: Result4RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result4$_free(ptr)
        }
    }
}
public class Result4RefMut: Result4Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result4Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result4: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result4$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result4$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result4) {
        __swift_bridge__$Vec_Result4$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result4$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result4(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result4Ref> {
        let pointer = __swift_bridge__$Vec_Result4$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result4Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result4RefMut> {
        let pointer = __swift_bridge__$Vec_Result4$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result4RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result4Ref> {
        UnsafePointer<Result4Ref>(OpaquePointer(__swift_bridge__$Vec_Result4$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result4$len(vecPtr)
    }
}


public class NamespaceErrors: NamespaceErrorsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NamespaceErrors$_free(ptr)
        }
    }
}
public class NamespaceErrorsRefMut: NamespaceErrorsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NamespaceErrorsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NamespaceErrors: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NamespaceErrors$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NamespaceErrors$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NamespaceErrors) {
        __swift_bridge__$Vec_NamespaceErrors$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NamespaceErrors$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NamespaceErrors(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NamespaceErrorsRef> {
        let pointer = __swift_bridge__$Vec_NamespaceErrors$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NamespaceErrorsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NamespaceErrorsRefMut> {
        let pointer = __swift_bridge__$Vec_NamespaceErrors$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NamespaceErrorsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NamespaceErrorsRef> {
        UnsafePointer<NamespaceErrorsRef>(OpaquePointer(__swift_bridge__$Vec_NamespaceErrors$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NamespaceErrors$len(vecPtr)
    }
}


public class NamespaceForFrontend: NamespaceForFrontendRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NamespaceForFrontend$_free(ptr)
        }
    }
}
public class NamespaceForFrontendRefMut: NamespaceForFrontendRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NamespaceForFrontendRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NamespaceForFrontend: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NamespaceForFrontend$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NamespaceForFrontend$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NamespaceForFrontend) {
        __swift_bridge__$Vec_NamespaceForFrontend$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NamespaceForFrontend$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NamespaceForFrontend(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NamespaceForFrontendRef> {
        let pointer = __swift_bridge__$Vec_NamespaceForFrontend$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NamespaceForFrontendRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NamespaceForFrontendRefMut> {
        let pointer = __swift_bridge__$Vec_NamespaceForFrontend$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NamespaceForFrontendRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NamespaceForFrontendRef> {
        UnsafePointer<NamespaceForFrontendRef>(OpaquePointer(__swift_bridge__$Vec_NamespaceForFrontend$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NamespaceForFrontend$len(vecPtr)
    }
}


public class Result3: Result3RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result3$_free(ptr)
        }
    }
}
public class Result3RefMut: Result3Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result3Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result3: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result3$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result3$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result3) {
        __swift_bridge__$Vec_Result3$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result3$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result3(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result3Ref> {
        let pointer = __swift_bridge__$Vec_Result3$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result3Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result3RefMut> {
        let pointer = __swift_bridge__$Vec_Result3$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result3RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result3Ref> {
        UnsafePointer<Result3Ref>(OpaquePointer(__swift_bridge__$Vec_Result3$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result3$len(vecPtr)
    }
}


public class BetOnCurrentlyViewingPostError: BetOnCurrentlyViewingPostErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$BetOnCurrentlyViewingPostError$_free(ptr)
        }
    }
}
public class BetOnCurrentlyViewingPostErrorRefMut: BetOnCurrentlyViewingPostErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class BetOnCurrentlyViewingPostErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension BetOnCurrentlyViewingPostError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_BetOnCurrentlyViewingPostError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_BetOnCurrentlyViewingPostError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: BetOnCurrentlyViewingPostError) {
        __swift_bridge__$Vec_BetOnCurrentlyViewingPostError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_BetOnCurrentlyViewingPostError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (BetOnCurrentlyViewingPostError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetOnCurrentlyViewingPostErrorRef> {
        let pointer = __swift_bridge__$Vec_BetOnCurrentlyViewingPostError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetOnCurrentlyViewingPostErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetOnCurrentlyViewingPostErrorRefMut> {
        let pointer = __swift_bridge__$Vec_BetOnCurrentlyViewingPostError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetOnCurrentlyViewingPostErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<BetOnCurrentlyViewingPostErrorRef> {
        UnsafePointer<BetOnCurrentlyViewingPostErrorRef>(OpaquePointer(__swift_bridge__$Vec_BetOnCurrentlyViewingPostError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_BetOnCurrentlyViewingPostError$len(vecPtr)
    }
}


public class BettingStatus: BettingStatusRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$BettingStatus$_free(ptr)
        }
    }
}
public class BettingStatusRefMut: BettingStatusRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class BettingStatusRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension BettingStatus: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_BettingStatus$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_BettingStatus$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: BettingStatus) {
        __swift_bridge__$Vec_BettingStatus$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_BettingStatus$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (BettingStatus(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BettingStatusRef> {
        let pointer = __swift_bridge__$Vec_BettingStatus$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BettingStatusRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BettingStatusRefMut> {
        let pointer = __swift_bridge__$Vec_BettingStatus$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BettingStatusRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<BettingStatusRef> {
        UnsafePointer<BettingStatusRef>(OpaquePointer(__swift_bridge__$Vec_BettingStatus$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_BettingStatus$len(vecPtr)
    }
}


public class SystemTime: SystemTimeRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$SystemTime$_free(ptr)
        }
    }
}
public class SystemTimeRefMut: SystemTimeRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class SystemTimeRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension SystemTime: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_SystemTime$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_SystemTime$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: SystemTime) {
        __swift_bridge__$Vec_SystemTime$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_SystemTime$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (SystemTime(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SystemTimeRef> {
        let pointer = __swift_bridge__$Vec_SystemTime$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SystemTimeRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<SystemTimeRefMut> {
        let pointer = __swift_bridge__$Vec_SystemTime$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return SystemTimeRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<SystemTimeRef> {
        UnsafePointer<SystemTimeRef>(OpaquePointer(__swift_bridge__$Vec_SystemTime$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_SystemTime$len(vecPtr)
    }
}


public class PlaceBetArg: PlaceBetArgRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PlaceBetArg$_free(ptr)
        }
    }
}
public class PlaceBetArgRefMut: PlaceBetArgRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PlaceBetArgRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PlaceBetArg: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PlaceBetArg$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PlaceBetArg$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PlaceBetArg) {
        __swift_bridge__$Vec_PlaceBetArg$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PlaceBetArg$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PlaceBetArg(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlaceBetArgRef> {
        let pointer = __swift_bridge__$Vec_PlaceBetArg$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlaceBetArgRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlaceBetArgRefMut> {
        let pointer = __swift_bridge__$Vec_PlaceBetArg$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlaceBetArgRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PlaceBetArgRef> {
        UnsafePointer<PlaceBetArgRef>(OpaquePointer(__swift_bridge__$Vec_PlaceBetArg$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PlaceBetArg$len(vecPtr)
    }
}


public class BetDirection: BetDirectionRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$BetDirection$_free(ptr)
        }
    }
}
public class BetDirectionRefMut: BetDirectionRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class BetDirectionRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension BetDirection: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_BetDirection$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_BetDirection$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: BetDirection) {
        __swift_bridge__$Vec_BetDirection$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_BetDirection$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (BetDirection(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetDirectionRef> {
        let pointer = __swift_bridge__$Vec_BetDirection$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetDirectionRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<BetDirectionRefMut> {
        let pointer = __swift_bridge__$Vec_BetDirection$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return BetDirectionRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<BetDirectionRef> {
        UnsafePointer<BetDirectionRef>(OpaquePointer(__swift_bridge__$Vec_BetDirection$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_BetDirection$len(vecPtr)
    }
}


public class Result2: Result2RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result2$_free(ptr)
        }
    }
}
public class Result2RefMut: Result2Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result2Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result2: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result2$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result2$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result2) {
        __swift_bridge__$Vec_Result2$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result2$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result2(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result2Ref> {
        let pointer = __swift_bridge__$Vec_Result2$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result2Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result2RefMut> {
        let pointer = __swift_bridge__$Vec_Result2$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result2RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result2Ref> {
        UnsafePointer<Result2Ref>(OpaquePointer(__swift_bridge__$Vec_Result2$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result2$len(vecPtr)
    }
}


public class Result1: Result1RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result1$_free(ptr)
        }
    }
}
public class Result1RefMut: Result1Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result1Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result1: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result1$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result1$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result1) {
        __swift_bridge__$Vec_Result1$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result1$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result1(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result1Ref> {
        let pointer = __swift_bridge__$Vec_Result1$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result1Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result1RefMut> {
        let pointer = __swift_bridge__$Vec_Result1$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result1RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result1Ref> {
        UnsafePointer<Result1Ref>(OpaquePointer(__swift_bridge__$Vec_Result1$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result1$len(vecPtr)
    }
}


public class CdaoTokenError: CdaoTokenErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$CdaoTokenError$_free(ptr)
        }
    }
}
public class CdaoTokenErrorRefMut: CdaoTokenErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CdaoTokenErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension CdaoTokenError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_CdaoTokenError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_CdaoTokenError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: CdaoTokenError) {
        __swift_bridge__$Vec_CdaoTokenError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_CdaoTokenError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (CdaoTokenError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CdaoTokenErrorRef> {
        let pointer = __swift_bridge__$Vec_CdaoTokenError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CdaoTokenErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CdaoTokenErrorRefMut> {
        let pointer = __swift_bridge__$Vec_CdaoTokenError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CdaoTokenErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CdaoTokenErrorRef> {
        UnsafePointer<CdaoTokenErrorRef>(OpaquePointer(__swift_bridge__$Vec_CdaoTokenError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_CdaoTokenError$len(vecPtr)
    }
}


public class TransferError: TransferErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$TransferError$_free(ptr)
        }
    }
}
public class TransferErrorRefMut: TransferErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class TransferErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension TransferError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_TransferError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_TransferError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: TransferError) {
        __swift_bridge__$Vec_TransferError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_TransferError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (TransferError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TransferErrorRef> {
        let pointer = __swift_bridge__$Vec_TransferError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TransferErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TransferErrorRefMut> {
        let pointer = __swift_bridge__$Vec_TransferError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TransferErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<TransferErrorRef> {
        UnsafePointer<TransferErrorRef>(OpaquePointer(__swift_bridge__$Vec_TransferError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_TransferError$len(vecPtr)
    }
}


public class RejectionCode: RejectionCodeRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$RejectionCode$_free(ptr)
        }
    }
}
public class RejectionCodeRefMut: RejectionCodeRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class RejectionCodeRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension RejectionCode: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_RejectionCode$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_RejectionCode$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: RejectionCode) {
        __swift_bridge__$Vec_RejectionCode$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_RejectionCode$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (RejectionCode(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<RejectionCodeRef> {
        let pointer = __swift_bridge__$Vec_RejectionCode$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return RejectionCodeRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<RejectionCodeRefMut> {
        let pointer = __swift_bridge__$Vec_RejectionCode$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return RejectionCodeRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<RejectionCodeRef> {
        UnsafePointer<RejectionCodeRef>(OpaquePointer(__swift_bridge__$Vec_RejectionCode$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_RejectionCode$len(vecPtr)
    }
}


public class PostDetailsFromFrontend: PostDetailsFromFrontendRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostDetailsFromFrontend$_free(ptr)
        }
    }
}
public class PostDetailsFromFrontendRefMut: PostDetailsFromFrontendRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostDetailsFromFrontendRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostDetailsFromFrontend: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostDetailsFromFrontend$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostDetailsFromFrontend$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostDetailsFromFrontend) {
        __swift_bridge__$Vec_PostDetailsFromFrontend$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostDetailsFromFrontend$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostDetailsFromFrontend(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostDetailsFromFrontendRef> {
        let pointer = __swift_bridge__$Vec_PostDetailsFromFrontend$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostDetailsFromFrontendRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostDetailsFromFrontendRefMut> {
        let pointer = __swift_bridge__$Vec_PostDetailsFromFrontend$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostDetailsFromFrontendRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostDetailsFromFrontendRef> {
        UnsafePointer<PostDetailsFromFrontendRef>(OpaquePointer(__swift_bridge__$Vec_PostDetailsFromFrontend$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostDetailsFromFrontend$len(vecPtr)
    }
}


public class Result_: Result_RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Result_$_free(ptr)
        }
    }
}
public class Result_RefMut: Result_Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class Result_Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension Result_: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Result_$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Result_$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Result_) {
        __swift_bridge__$Vec_Result_$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Result_$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Result_(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result_Ref> {
        let pointer = __swift_bridge__$Vec_Result_$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result_Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<Result_RefMut> {
        let pointer = __swift_bridge__$Vec_Result_$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return Result_RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<Result_Ref> {
        UnsafePointer<Result_Ref>(OpaquePointer(__swift_bridge__$Vec_Result_$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Result_$len(vecPtr)
    }
}


public class Service: ServiceRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$Service$_free(ptr)
        }
    }
}
extension Service {
  public convenience init<GenericToRustStr: ToRustStr>(
    _ principal_text: GenericToRustStr,
    _ agent_url: GenericToRustStr
  ) throws {
    var initializationError: Error?
    var servicePtr: UnsafeMutableRawPointer?

    agent_url.toRustStr { agent_urlAsRustStr in
      principal_text.toRustStr { principal_textAsRustStr in
        let val = __swift_bridge__$Service$new(principal_textAsRustStr, agent_urlAsRustStr)
        if val.is_ok {
          servicePtr = val.ok_or_err!
        } else {
          initializationError = PrincipalError(ptr: val.ok_or_err!)
        }
      }
    }
    if let error = initializationError {
      throw error
    }
    guard let ptr = servicePtr else {
      fatalError("Service initialization failed without an error.")
    }
    self.init(ptr: ptr)
  }
}
public class ServiceRefMut: ServiceRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class ServiceRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension ServiceRef {
    public func add_device_id<GenericIntoRustString: IntoRustString>(_ arg0: GenericIntoRustString) async throws -> Result_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$add_device_id>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$add_device_id(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$add_device_id(wrapperPtr, onComplete, ptr, { let rustString = arg0.intoRustString(); rustString.isOwned = false; return rustString.ptr }())
        })
    }
    class CbWrapper$Service$add_device_id {
        var cb: (Result<Result_, Error>) -> ()
    
        public init(cb: @escaping (Result<Result_, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func add_post_v_2(_ arg0: PostDetailsFromFrontend) async throws -> Result1 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$add_post_v_2>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result1(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result1, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$add_post_v_2(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$add_post_v_2(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$add_post_v_2 {
        var cb: (Result<Result1, Error>) -> ()
    
        public init(cb: @escaping (Result<Result1, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func add_token(_ arg0: Principal) async throws -> Result2 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$add_token>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result2(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result2, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$add_token(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$add_token(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$add_token {
        var cb: (Result<Result2, Error>) -> ()
    
        public init(cb: @escaping (Result<Result2, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func bet_on_currently_viewing_post(_ arg0: PlaceBetArg) async throws -> Result3 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$bet_on_currently_viewing_post>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result3(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result3, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$bet_on_currently_viewing_post(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$bet_on_currently_viewing_post(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$bet_on_currently_viewing_post {
        var cb: (Result<Result3, Error>) -> ()
    
        public init(cb: @escaping (Result<Result3, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold(_ arg0: RustVec<UInt64>) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold(wrapperPtr, onComplete, ptr, { let val = arg0; val.isOwned = false; return val.ptr }())
        })
    }
    class CbWrapper$Service$check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func clear_snapshot() async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$clear_snapshot>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$clear_snapshot(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$clear_snapshot(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$clear_snapshot {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func create_a_namespace<GenericIntoRustString: IntoRustString>(_ arg0: GenericIntoRustString) async throws -> Result4 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$create_a_namespace>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result4(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result4, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$create_a_namespace(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$create_a_namespace(wrapperPtr, onComplete, ptr, { let rustString = arg0.intoRustString(); rustString.isOwned = false; return rustString.ptr }())
        })
    }
    class CbWrapper$Service$create_a_namespace {
        var cb: (Result<Result4, Error>) -> ()
    
        public init(cb: @escaping (Result<Result4, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func delete_key_value_pair<GenericIntoRustString: IntoRustString>(_ arg0: UInt64, _ arg1: GenericIntoRustString) async throws -> Result5 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$delete_key_value_pair>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result5(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result5, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$delete_key_value_pair(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$delete_key_value_pair(wrapperPtr, onComplete, ptr, arg0, { let rustString = arg1.intoRustString(); rustString.isOwned = false; return rustString.ptr }())
        })
    }
    class CbWrapper$Service$delete_key_value_pair {
        var cb: (Result<Result5, Error>) -> ()
    
        public init(cb: @escaping (Result<Result5, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func delete_multiple_key_value_pairs<GenericIntoRustString: IntoRustString>(_ arg0: UInt64, _ arg1: RustVec<GenericIntoRustString>) async throws -> Result6 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$delete_multiple_key_value_pairs>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result6(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result6, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$delete_multiple_key_value_pairs(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$delete_multiple_key_value_pairs(wrapperPtr, onComplete, ptr, arg0, { let val = arg1; val.isOwned = false; return val.ptr }())
        })
    }
    class CbWrapper$Service$delete_multiple_key_value_pairs {
        var cb: (Result<Result6, Error>) -> ()
    
        public init(cb: @escaping (Result<Result6, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func deploy_cdao_sns(_ arg0: SnsInitPayload, _ arg1: UInt64) async throws -> Result7 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$deploy_cdao_sns>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result7(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result7, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$deploy_cdao_sns(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$deploy_cdao_sns(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), arg1)
        })
    }
    class CbWrapper$Service$deploy_cdao_sns {
        var cb: (Result<Result7, Error>) -> ()
    
        public init(cb: @escaping (Result<Result7, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func deployed_cdao_canisters() async throws -> RustVec<DeployedCdaoCanisters> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$deployed_cdao_canisters>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<DeployedCdaoCanisters>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<DeployedCdaoCanisters>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$deployed_cdao_canisters(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$deployed_cdao_canisters(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$deployed_cdao_canisters {
        var cb: (Result<RustVec<DeployedCdaoCanisters>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<DeployedCdaoCanisters>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func do_i_follow_this_user(_ arg0: FolloweeArg) async throws -> Result8 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$do_i_follow_this_user>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result8(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result8, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$do_i_follow_this_user(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$do_i_follow_this_user(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$do_i_follow_this_user {
        var cb: (Result<Result8, Error>) -> ()
    
        public init(cb: @escaping (Result<Result8, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_bet_details_for_a_user_on_a_post(_ arg0: Principal, _ arg1: UInt64) async throws -> Result9 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_bet_details_for_a_user_on_a_post>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result9(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result9, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_bet_details_for_a_user_on_a_post(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_bet_details_for_a_user_on_a_post(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), arg1)
        })
    }
    class CbWrapper$Service$get_bet_details_for_a_user_on_a_post {
        var cb: (Result<Result9, Error>) -> ()
    
        public init(cb: @escaping (Result<Result9, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_device_identities() async throws -> RustVec<DeviceIdentity> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_device_identities>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<DeviceIdentity>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<DeviceIdentity>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_device_identities(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_device_identities(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_device_identities {
        var cb: (Result<RustVec<DeviceIdentity>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<DeviceIdentity>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_entire_individual_post_detail_by_id(_ arg0: UInt64) async throws -> Result10 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_entire_individual_post_detail_by_id>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result10(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result10, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_entire_individual_post_detail_by_id(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_entire_individual_post_detail_by_id(wrapperPtr, onComplete, ptr, arg0)
        })
    }
    class CbWrapper$Service$get_entire_individual_post_detail_by_id {
        var cb: (Result<Result10, Error>) -> ()
    
        public init(cb: @escaping (Result<Result10, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_hot_or_not_bet_details_for_this_post(_ arg0: UInt64) async throws -> BettingStatus {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_hot_or_not_bet_details_for_this_post>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(BettingStatus(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<BettingStatus, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_hot_or_not_bet_details_for_this_post(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_hot_or_not_bet_details_for_this_post(wrapperPtr, onComplete, ptr, arg0)
        })
    }
    class CbWrapper$Service$get_hot_or_not_bet_details_for_this_post {
        var cb: (Result<BettingStatus, Error>) -> ()
    
        public init(cb: @escaping (Result<BettingStatus, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_hot_or_not_bets_placed_by_this_profile_with_pagination(_ arg0: UInt64) async throws -> RustVec<PlacedBetDetail> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_hot_or_not_bets_placed_by_this_profile_with_pagination>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<PlacedBetDetail>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<PlacedBetDetail>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_hot_or_not_bets_placed_by_this_profile_with_pagination(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_hot_or_not_bets_placed_by_this_profile_with_pagination(wrapperPtr, onComplete, ptr, arg0)
        })
    }
    class CbWrapper$Service$get_hot_or_not_bets_placed_by_this_profile_with_pagination {
        var cb: (Result<RustVec<PlacedBetDetail>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<PlacedBetDetail>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_individual_post_details_by_id(_ arg0: UInt64) async throws -> PostDetailsForFrontend {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_individual_post_details_by_id>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PostDetailsForFrontend(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PostDetailsForFrontend, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_individual_post_details_by_id(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_individual_post_details_by_id(wrapperPtr, onComplete, ptr, arg0)
        })
    }
    class CbWrapper$Service$get_individual_post_details_by_id {
        var cb: (Result<PostDetailsForFrontend, Error>) -> ()
    
        public init(cb: @escaping (Result<PostDetailsForFrontend, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_last_access_time() async throws -> Result11 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_last_access_time>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result11(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result11, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_last_access_time(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_last_access_time(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_last_access_time {
        var cb: (Result<Result11, Error>) -> ()
    
        public init(cb: @escaping (Result<Result11, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_last_canister_functionality_access_time() async throws -> Result11 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_last_canister_functionality_access_time>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result11(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result11, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_last_canister_functionality_access_time(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_last_canister_functionality_access_time(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_last_canister_functionality_access_time {
        var cb: (Result<Result11, Error>) -> ()
    
        public init(cb: @escaping (Result<Result11, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_ml_feed_cache_paginated(_ arg0: UInt64, _ arg1: UInt64) async throws -> RustVec<MlFeedCacheItem> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_ml_feed_cache_paginated>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<MlFeedCacheItem>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<MlFeedCacheItem>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_ml_feed_cache_paginated(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_ml_feed_cache_paginated(wrapperPtr, onComplete, ptr, arg0, arg1)
        })
    }
    class CbWrapper$Service$get_ml_feed_cache_paginated {
        var cb: (Result<RustVec<MlFeedCacheItem>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<MlFeedCacheItem>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_posts_of_this_user_profile_with_pagination(_ arg0: UInt64, _ arg1: UInt64) async throws -> Result12 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_posts_of_this_user_profile_with_pagination>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result12(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result12, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_posts_of_this_user_profile_with_pagination(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_posts_of_this_user_profile_with_pagination(wrapperPtr, onComplete, ptr, arg0, arg1)
        })
    }
    class CbWrapper$Service$get_posts_of_this_user_profile_with_pagination {
        var cb: (Result<Result12, Error>) -> ()
    
        public init(cb: @escaping (Result<Result12, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_posts_of_this_user_profile_with_pagination_cursor(_ arg0: UInt64, _ arg1: UInt64) async throws -> Result12 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_posts_of_this_user_profile_with_pagination_cursor>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result12(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result12, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_posts_of_this_user_profile_with_pagination_cursor(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_posts_of_this_user_profile_with_pagination_cursor(wrapperPtr, onComplete, ptr, arg0, arg1)
        })
    }
    class CbWrapper$Service$get_posts_of_this_user_profile_with_pagination_cursor {
        var cb: (Result<Result12, Error>) -> ()
    
        public init(cb: @escaping (Result<Result12, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_principals_that_follow_this_profile_paginated(_ arg0: Optional<UInt64>) async throws -> RustVec<FollowEntry> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_principals_that_follow_this_profile_paginated>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<FollowEntry>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<FollowEntry>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_principals_that_follow_this_profile_paginated(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_principals_that_follow_this_profile_paginated(wrapperPtr, onComplete, ptr, arg0.intoFfiRepr())
        })
    }
    class CbWrapper$Service$get_principals_that_follow_this_profile_paginated {
        var cb: (Result<RustVec<FollowEntry>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<FollowEntry>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_principals_this_profile_follows_paginated(_ arg0: Optional<UInt64>) async throws -> RustVec<FollowEntry> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_principals_this_profile_follows_paginated>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<FollowEntry>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<FollowEntry>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_principals_this_profile_follows_paginated(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_principals_this_profile_follows_paginated(wrapperPtr, onComplete, ptr, arg0.intoFfiRepr())
        })
    }
    class CbWrapper$Service$get_principals_this_profile_follows_paginated {
        var cb: (Result<RustVec<FollowEntry>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<FollowEntry>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_profile_details() async throws -> UserProfileDetailsForFrontend {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_profile_details>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(UserProfileDetailsForFrontend(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<UserProfileDetailsForFrontend, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_profile_details(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_profile_details(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_profile_details {
        var cb: (Result<UserProfileDetailsForFrontend, Error>) -> ()
    
        public init(cb: @escaping (Result<UserProfileDetailsForFrontend, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_profile_details_v_2() async throws -> UserProfileDetailsForFrontendV2 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_profile_details_v_2>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(UserProfileDetailsForFrontendV2(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<UserProfileDetailsForFrontendV2, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_profile_details_v_2(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_profile_details_v_2(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_profile_details_v_2 {
        var cb: (Result<UserProfileDetailsForFrontendV2, Error>) -> ()
    
        public init(cb: @escaping (Result<UserProfileDetailsForFrontendV2, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_rewarded_for_referral(_ arg0: Principal, _ arg1: Principal) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$get_rewarded_for_referral>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_rewarded_for_referral(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_rewarded_for_referral(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$get_rewarded_for_referral {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_rewarded_for_signing_up() async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$get_rewarded_for_signing_up>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_rewarded_for_signing_up(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_rewarded_for_signing_up(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_rewarded_for_signing_up {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_session_type() async throws -> Result13 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_session_type>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result13(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result13, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_session_type(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_session_type(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_session_type {
        var cb: (Result<Result13, Error>) -> ()
    
        public init(cb: @escaping (Result<Result13, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_success_history() async throws -> Result14 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_success_history>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result14(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result14, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_success_history(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_success_history(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_success_history {
        var cb: (Result<Result14, Error>) -> ()
    
        public init(cb: @escaping (Result<Result14, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_token_roots_of_this_user_with_pagination_cursor(_ arg0: UInt64, _ arg1: UInt64) async throws -> Result15 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_token_roots_of_this_user_with_pagination_cursor>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result15(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result15, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_token_roots_of_this_user_with_pagination_cursor(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_token_roots_of_this_user_with_pagination_cursor(wrapperPtr, onComplete, ptr, arg0, arg1)
        })
    }
    class CbWrapper$Service$get_token_roots_of_this_user_with_pagination_cursor {
        var cb: (Result<Result15, Error>) -> ()
    
        public init(cb: @escaping (Result<Result15, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_user_caniser_cycle_balance() async throws -> Nat {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_user_caniser_cycle_balance>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Nat(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Nat, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_user_caniser_cycle_balance(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_user_caniser_cycle_balance(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_user_caniser_cycle_balance {
        var cb: (Result<Nat, Error>) -> ()
    
        public init(cb: @escaping (Result<Nat, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_user_utility_token_transaction_history_with_pagination(_ arg0: UInt64, _ arg1: UInt64) async throws -> Result16 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_user_utility_token_transaction_history_with_pagination>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result16(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result16, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_user_utility_token_transaction_history_with_pagination(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_user_utility_token_transaction_history_with_pagination(wrapperPtr, onComplete, ptr, arg0, arg1)
        })
    }
    class CbWrapper$Service$get_user_utility_token_transaction_history_with_pagination {
        var cb: (Result<Result16, Error>) -> ()
    
        public init(cb: @escaping (Result<Result16, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_version() async throws -> RustString {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_version>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustString(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustString, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_version(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_version(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_version {
        var cb: (Result<RustString, Error>) -> ()
    
        public init(cb: @escaping (Result<RustString, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_watch_history() async throws -> Result17 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_watch_history>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result17(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result17, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_watch_history(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_watch_history(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_watch_history {
        var cb: (Result<Result17, Error>) -> ()
    
        public init(cb: @escaping (Result<Result17, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func http_request(_ arg0: HttpRequest) async throws -> HttpResponse {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$http_request>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(HttpResponse(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<HttpResponse, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$http_request(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$http_request(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$http_request {
        var cb: (Result<HttpResponse, Error>) -> ()
    
        public init(cb: @escaping (Result<HttpResponse, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func list_namespace_keys(_ arg0: UInt64) async throws -> Result18 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$list_namespace_keys>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result18(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result18, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$list_namespace_keys(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$list_namespace_keys(wrapperPtr, onComplete, ptr, arg0)
        })
    }
    class CbWrapper$Service$list_namespace_keys {
        var cb: (Result<Result18, Error>) -> ()
    
        public init(cb: @escaping (Result<Result18, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func list_namespaces(_ arg0: UInt64, _ arg1: UInt64) async throws -> RustVec<NamespaceForFrontend> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$list_namespaces>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<NamespaceForFrontend>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<NamespaceForFrontend>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$list_namespaces(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$list_namespaces(wrapperPtr, onComplete, ptr, arg0, arg1)
        })
    }
    class CbWrapper$Service$list_namespaces {
        var cb: (Result<RustVec<NamespaceForFrontend>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<NamespaceForFrontend>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func load_snapshot(_ arg0: UInt64) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$load_snapshot>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$load_snapshot(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$load_snapshot(wrapperPtr, onComplete, ptr, arg0)
        })
    }
    class CbWrapper$Service$load_snapshot {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func once_reenqueue_timers_for_pending_bet_outcomes() async throws -> Result19 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$once_reenqueue_timers_for_pending_bet_outcomes>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result19(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result19, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$once_reenqueue_timers_for_pending_bet_outcomes(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$once_reenqueue_timers_for_pending_bet_outcomes(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$once_reenqueue_timers_for_pending_bet_outcomes {
        var cb: (Result<Result19, Error>) -> ()
    
        public init(cb: @escaping (Result<Result19, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func read_key_value_pair<GenericIntoRustString: IntoRustString>(_ arg0: UInt64, _ arg1: GenericIntoRustString) async throws -> Result5 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$read_key_value_pair>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result5(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result5, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$read_key_value_pair(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$read_key_value_pair(wrapperPtr, onComplete, ptr, arg0, { let rustString = arg1.intoRustString(); rustString.isOwned = false; return rustString.ptr }())
        })
    }
    class CbWrapper$Service$read_key_value_pair {
        var cb: (Result<Result5, Error>) -> ()
    
        public init(cb: @escaping (Result<Result5, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func receive_and_save_snaphot(_ arg0: UInt64, _ arg1: ByteBuf) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$receive_and_save_snaphot>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$receive_and_save_snaphot(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$receive_and_save_snaphot(wrapperPtr, onComplete, ptr, arg0, {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$receive_and_save_snaphot {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func receive_bet_from_bet_makers_canister(_ arg0: PlaceBetArg, _ arg1: Principal) async throws -> Result3 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$receive_bet_from_bet_makers_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result3(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result3, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$receive_bet_from_bet_makers_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$receive_bet_from_bet_makers_canister(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$receive_bet_from_bet_makers_canister {
        var cb: (Result<Result3, Error>) -> ()
    
        public init(cb: @escaping (Result<Result3, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func receive_bet_winnings_when_distributed(_ arg0: UInt64, _ arg1: BetOutcomeForBetMaker) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$receive_bet_winnings_when_distributed>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$receive_bet_winnings_when_distributed(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$receive_bet_winnings_when_distributed(wrapperPtr, onComplete, ptr, arg0, {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$receive_bet_winnings_when_distributed {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func receive_data_from_hotornot(_ arg0: Principal, _ arg1: UInt64, _ arg2: RustVec<Post>) async throws -> Result20 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$receive_data_from_hotornot>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result20(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result20, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$receive_data_from_hotornot(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$receive_data_from_hotornot(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), arg1, { let val = arg2; val.isOwned = false; return val.ptr }())
        })
    }
    class CbWrapper$Service$receive_data_from_hotornot {
        var cb: (Result<Result20, Error>) -> ()
    
        public init(cb: @escaping (Result<Result20, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func return_cycles_to_user_index_canister(_ arg0: Optional<Nat>) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$return_cycles_to_user_index_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$return_cycles_to_user_index_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$return_cycles_to_user_index_canister(wrapperPtr, onComplete, ptr, { if let val = arg0 { val.isOwned = false; return val.ptr } else { return nil } }())
        })
    }
    class CbWrapper$Service$return_cycles_to_user_index_canister {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func save_snapshot_json() async throws -> UInt32 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __swift_bridge__$ResultU32AndAgentError) {
            let wrapper = Unmanaged<CbWrapper$Service$save_snapshot_json>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            switch rustFnRetVal.tag { case __swift_bridge__$ResultU32AndAgentError$ResultOk: wrapper.cb(.success(rustFnRetVal.payload.ok)) case __swift_bridge__$ResultU32AndAgentError$ResultErr: wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.payload.err))) default: fatalError() }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<UInt32, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$save_snapshot_json(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$save_snapshot_json(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$save_snapshot_json {
        var cb: (Result<UInt32, Error>) -> ()
    
        public init(cb: @escaping (Result<UInt32, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func settle_neurons_fund_participation(_ arg0: SettleNeuronsFundParticipationRequest) async throws -> SettleNeuronsFundParticipationResponse {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$settle_neurons_fund_participation>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(SettleNeuronsFundParticipationResponse(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<SettleNeuronsFundParticipationResponse, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$settle_neurons_fund_participation(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$settle_neurons_fund_participation(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$settle_neurons_fund_participation {
        var cb: (Result<SettleNeuronsFundParticipationResponse, Error>) -> ()
    
        public init(cb: @escaping (Result<SettleNeuronsFundParticipationResponse, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func transfer_token_to_user_canister(_ arg0: Principal, _ arg1: Principal, _ arg2: Optional<ByteBuf>, _ arg3: Nat) async throws -> Result23 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$transfer_token_to_user_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result23(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result23, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$transfer_token_to_user_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$transfer_token_to_user_canister(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), {arg1.isOwned = false; return arg1.ptr;}(), { if let val = arg2 { val.isOwned = false; return val.ptr } else { return nil } }(), {arg3.isOwned = false; return arg3.ptr;}())
        })
    }
    class CbWrapper$Service$transfer_token_to_user_canister {
        var cb: (Result<Result23, Error>) -> ()
    
        public init(cb: @escaping (Result<Result23, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func transfer_tokens_and_posts(_ arg0: Principal, _ arg1: Principal) async throws -> Result20 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$transfer_tokens_and_posts>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result20(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result20, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$transfer_tokens_and_posts(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$transfer_tokens_and_posts(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$transfer_tokens_and_posts {
        var cb: (Result<Result20, Error>) -> ()
    
        public init(cb: @escaping (Result<Result20, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_last_access_time() async throws -> Result24 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_last_access_time>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result24(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result24, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_last_access_time(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_last_access_time(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$update_last_access_time {
        var cb: (Result<Result24, Error>) -> ()
    
        public init(cb: @escaping (Result<Result24, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_last_canister_functionality_access_time() async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_last_canister_functionality_access_time>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_last_canister_functionality_access_time(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_last_canister_functionality_access_time(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$update_last_canister_functionality_access_time {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_ml_feed_cache(_ arg0: RustVec<MlFeedCacheItem>) async throws -> Result24 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_ml_feed_cache>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result24(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result24, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_ml_feed_cache(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_ml_feed_cache(wrapperPtr, onComplete, ptr, { let val = arg0; val.isOwned = false; return val.ptr }())
        })
    }
    class CbWrapper$Service$update_ml_feed_cache {
        var cb: (Result<Result24, Error>) -> ()
    
        public init(cb: @escaping (Result<Result24, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_post_add_view_details(_ arg0: UInt64, _ arg1: PostViewDetailsFromFrontend) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_post_add_view_details>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_post_add_view_details(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_post_add_view_details(wrapperPtr, onComplete, ptr, arg0, {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$update_post_add_view_details {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_post_as_ready_to_view(_ arg0: UInt64) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_post_as_ready_to_view>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_post_as_ready_to_view(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_post_as_ready_to_view(wrapperPtr, onComplete, ptr, arg0)
        })
    }
    class CbWrapper$Service$update_post_as_ready_to_view {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_post_status(_ arg0: UInt64, _ arg1: PostStatus) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_post_status>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_post_status(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_post_status(wrapperPtr, onComplete, ptr, arg0, {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$update_post_status {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_profile_display_details(_ arg0: UserProfileUpdateDetailsFromFrontend) async throws -> Result25 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_profile_display_details>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result25(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result25, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_profile_display_details(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_profile_display_details(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_profile_display_details {
        var cb: (Result<Result25, Error>) -> ()
    
        public init(cb: @escaping (Result<Result25, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_profile_owner(_ arg0: Optional<Principal>) async throws -> Result26 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_profile_owner>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result26(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result26, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_profile_owner(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_profile_owner(wrapperPtr, onComplete, ptr, { if let val = arg0 { val.isOwned = false; return val.ptr } else { return nil } }())
        })
    }
    class CbWrapper$Service$update_profile_owner {
        var cb: (Result<Result26, Error>) -> ()
    
        public init(cb: @escaping (Result<Result26, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_profile_set_unique_username_once<GenericIntoRustString: IntoRustString>(_ arg0: GenericIntoRustString) async throws -> Result27 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_profile_set_unique_username_once>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result27(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result27, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_profile_set_unique_username_once(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_profile_set_unique_username_once(wrapperPtr, onComplete, ptr, { let rustString = arg0.intoRustString(); rustString.isOwned = false; return rustString.ptr }())
        })
    }
    class CbWrapper$Service$update_profile_set_unique_username_once {
        var cb: (Result<Result27, Error>) -> ()
    
        public init(cb: @escaping (Result<Result27, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_profiles_i_follow_toggle_list_with_specified_profile(_ arg0: FolloweeArg) async throws -> Result8 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_profiles_i_follow_toggle_list_with_specified_profile>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result8(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result8, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_profiles_i_follow_toggle_list_with_specified_profile(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_profiles_i_follow_toggle_list_with_specified_profile(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_profiles_i_follow_toggle_list_with_specified_profile {
        var cb: (Result<Result8, Error>) -> ()
    
        public init(cb: @escaping (Result<Result8, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_profiles_that_follow_me_toggle_list_with_specified_profile(_ arg0: FollowerArg) async throws -> Result8 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_profiles_that_follow_me_toggle_list_with_specified_profile>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result8(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result8, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_profiles_that_follow_me_toggle_list_with_specified_profile(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_profiles_that_follow_me_toggle_list_with_specified_profile(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_profiles_that_follow_me_toggle_list_with_specified_profile {
        var cb: (Result<Result8, Error>) -> ()
    
        public init(cb: @escaping (Result<Result8, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_referrer_details(_ arg0: UserCanisterDetails) async throws -> Result24 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_referrer_details>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result24(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result24, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_referrer_details(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_referrer_details(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_referrer_details {
        var cb: (Result<Result24, Error>) -> ()
    
        public init(cb: @escaping (Result<Result24, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_session_type(_ arg0: SessionType) async throws -> Result24 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_session_type>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result24(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result24, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_session_type(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_session_type(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_session_type {
        var cb: (Result<Result24, Error>) -> ()
    
        public init(cb: @escaping (Result<Result24, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_success_history(_ arg0: SuccessHistoryItemV1) async throws -> Result24 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_success_history>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result24(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result24, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_success_history(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_success_history(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_success_history {
        var cb: (Result<Result24, Error>) -> ()
    
        public init(cb: @escaping (Result<Result24, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_watch_history(_ arg0: WatchHistoryItem) async throws -> Result24 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$update_watch_history>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result24(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result24, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_watch_history(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_watch_history(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_watch_history {
        var cb: (Result<Result24, Error>) -> ()
    
        public init(cb: @escaping (Result<Result24, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_well_known_principal(_ arg0: KnownPrincipalType, _ arg1: Principal) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_well_known_principal>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_well_known_principal(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_well_known_principal(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$update_well_known_principal {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func write_key_value_pair<GenericIntoRustString: IntoRustString>(_ arg0: UInt64, _ arg1: GenericIntoRustString, _ arg2: GenericIntoRustString) async throws -> Result5 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$write_key_value_pair>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(Result5(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<Result5, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$write_key_value_pair(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$write_key_value_pair(wrapperPtr, onComplete, ptr, arg0, { let rustString = arg1.intoRustString(); rustString.isOwned = false; return rustString.ptr }(), { let rustString = arg2.intoRustString(); rustString.isOwned = false; return rustString.ptr }())
        })
    }
    class CbWrapper$Service$write_key_value_pair {
        var cb: (Result<Result5, Error>) -> ()
    
        public init(cb: @escaping (Result<Result5, Error>) -> ()) {
            self.cb = cb
        }
    }
}
extension Service: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_Service$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_Service$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: Service) {
        __swift_bridge__$Vec_Service$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_Service$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (Service(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<ServiceRef> {
        let pointer = __swift_bridge__$Vec_Service$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return ServiceRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<ServiceRefMut> {
        let pointer = __swift_bridge__$Vec_Service$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return ServiceRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<ServiceRef> {
        UnsafePointer<ServiceRef>(OpaquePointer(__swift_bridge__$Vec_Service$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_Service$len(vecPtr)
    }
}


public class DelegatedIdentityWire: DelegatedIdentityWireRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$DelegatedIdentityWire$_free(ptr)
        }
    }
}
public class DelegatedIdentityWireRefMut: DelegatedIdentityWireRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class DelegatedIdentityWireRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension DelegatedIdentityWire: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_DelegatedIdentityWire$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_DelegatedIdentityWire$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: DelegatedIdentityWire) {
        __swift_bridge__$Vec_DelegatedIdentityWire$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_DelegatedIdentityWire$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (DelegatedIdentityWire(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DelegatedIdentityWireRef> {
        let pointer = __swift_bridge__$Vec_DelegatedIdentityWire$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DelegatedIdentityWireRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<DelegatedIdentityWireRefMut> {
        let pointer = __swift_bridge__$Vec_DelegatedIdentityWire$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return DelegatedIdentityWireRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<DelegatedIdentityWireRef> {
        UnsafePointer<DelegatedIdentityWireRef>(OpaquePointer(__swift_bridge__$Vec_DelegatedIdentityWire$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_DelegatedIdentityWire$len(vecPtr)
    }
}














public class PlatformOrchestratorResult1: PlatformOrchestratorResult1RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PlatformOrchestratorResult1$_free(ptr)
        }
    }
}
public class PlatformOrchestratorResult1RefMut: PlatformOrchestratorResult1Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PlatformOrchestratorResult1Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PlatformOrchestratorResult1: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PlatformOrchestratorResult1$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PlatformOrchestratorResult1$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PlatformOrchestratorResult1) {
        __swift_bridge__$Vec_PlatformOrchestratorResult1$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorResult1$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PlatformOrchestratorResult1(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlatformOrchestratorResult1Ref> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorResult1$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlatformOrchestratorResult1Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlatformOrchestratorResult1RefMut> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorResult1$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlatformOrchestratorResult1RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PlatformOrchestratorResult1Ref> {
        UnsafePointer<PlatformOrchestratorResult1Ref>(OpaquePointer(__swift_bridge__$Vec_PlatformOrchestratorResult1$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PlatformOrchestratorResult1$len(vecPtr)
    }
}






public class CanisterUpgradeStatus: CanisterUpgradeStatusRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$CanisterUpgradeStatus$_free(ptr)
        }
    }
}
public class CanisterUpgradeStatusRefMut: CanisterUpgradeStatusRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class CanisterUpgradeStatusRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension CanisterUpgradeStatus: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_CanisterUpgradeStatus$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_CanisterUpgradeStatus$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: CanisterUpgradeStatus) {
        __swift_bridge__$Vec_CanisterUpgradeStatus$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_CanisterUpgradeStatus$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (CanisterUpgradeStatus(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CanisterUpgradeStatusRef> {
        let pointer = __swift_bridge__$Vec_CanisterUpgradeStatus$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CanisterUpgradeStatusRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<CanisterUpgradeStatusRefMut> {
        let pointer = __swift_bridge__$Vec_CanisterUpgradeStatus$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return CanisterUpgradeStatusRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<CanisterUpgradeStatusRef> {
        UnsafePointer<CanisterUpgradeStatusRef>(OpaquePointer(__swift_bridge__$Vec_CanisterUpgradeStatus$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_CanisterUpgradeStatus$len(vecPtr)
    }
}


public class UpgradeCanisterArg: UpgradeCanisterArgRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$UpgradeCanisterArg$_free(ptr)
        }
    }
}
public class UpgradeCanisterArgRefMut: UpgradeCanisterArgRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class UpgradeCanisterArgRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension UpgradeCanisterArg: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_UpgradeCanisterArg$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_UpgradeCanisterArg$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: UpgradeCanisterArg) {
        __swift_bridge__$Vec_UpgradeCanisterArg$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_UpgradeCanisterArg$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (UpgradeCanisterArg(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UpgradeCanisterArgRef> {
        let pointer = __swift_bridge__$Vec_UpgradeCanisterArg$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UpgradeCanisterArgRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<UpgradeCanisterArgRefMut> {
        let pointer = __swift_bridge__$Vec_UpgradeCanisterArg$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return UpgradeCanisterArgRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<UpgradeCanisterArgRef> {
        UnsafePointer<UpgradeCanisterArgRef>(OpaquePointer(__swift_bridge__$Vec_UpgradeCanisterArg$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_UpgradeCanisterArg$len(vecPtr)
    }
}


public class WasmType: WasmTypeRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$WasmType$_free(ptr)
        }
    }
}
public class WasmTypeRefMut: WasmTypeRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class WasmTypeRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension WasmType: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_WasmType$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_WasmType$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: WasmType) {
        __swift_bridge__$Vec_WasmType$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_WasmType$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (WasmType(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<WasmTypeRef> {
        let pointer = __swift_bridge__$Vec_WasmType$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return WasmTypeRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<WasmTypeRefMut> {
        let pointer = __swift_bridge__$Vec_WasmType$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return WasmTypeRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<WasmTypeRef> {
        UnsafePointer<WasmTypeRef>(OpaquePointer(__swift_bridge__$Vec_WasmType$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_WasmType$len(vecPtr)
    }
}


public class PlatformOrchestratorResult_: PlatformOrchestratorResult_RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PlatformOrchestratorResult_$_free(ptr)
        }
    }
}
public class PlatformOrchestratorResult_RefMut: PlatformOrchestratorResult_Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PlatformOrchestratorResult_Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PlatformOrchestratorResult_: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PlatformOrchestratorResult_$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PlatformOrchestratorResult_$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PlatformOrchestratorResult_) {
        __swift_bridge__$Vec_PlatformOrchestratorResult_$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorResult_$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PlatformOrchestratorResult_(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlatformOrchestratorResult_Ref> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorResult_$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlatformOrchestratorResult_Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlatformOrchestratorResult_RefMut> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorResult_$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlatformOrchestratorResult_RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PlatformOrchestratorResult_Ref> {
        UnsafePointer<PlatformOrchestratorResult_Ref>(OpaquePointer(__swift_bridge__$Vec_PlatformOrchestratorResult_$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PlatformOrchestratorResult_$len(vecPtr)
    }
}


public class PlatformOrchestratorInitArgs: PlatformOrchestratorInitArgsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PlatformOrchestratorInitArgs$_free(ptr)
        }
    }
}
public class PlatformOrchestratorInitArgsRefMut: PlatformOrchestratorInitArgsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PlatformOrchestratorInitArgsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PlatformOrchestratorInitArgs: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PlatformOrchestratorInitArgs$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PlatformOrchestratorInitArgs$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PlatformOrchestratorInitArgs) {
        __swift_bridge__$Vec_PlatformOrchestratorInitArgs$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorInitArgs$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PlatformOrchestratorInitArgs(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlatformOrchestratorInitArgsRef> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorInitArgs$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlatformOrchestratorInitArgsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PlatformOrchestratorInitArgsRefMut> {
        let pointer = __swift_bridge__$Vec_PlatformOrchestratorInitArgs$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PlatformOrchestratorInitArgsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PlatformOrchestratorInitArgsRef> {
        UnsafePointer<PlatformOrchestratorInitArgsRef>(OpaquePointer(__swift_bridge__$Vec_PlatformOrchestratorInitArgs$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PlatformOrchestratorInitArgs$len(vecPtr)
    }
}



extension ServiceRef {
    public func deposit_cycles_to_canister(_ arg0: Principal, _ arg1: Nat) async throws -> PlatformOrchestratorResult_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$deposit_cycles_to_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PlatformOrchestratorResult_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PlatformOrchestratorResult_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$deposit_cycles_to_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$deposit_cycles_to_canister(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$deposit_cycles_to_canister {
        var cb: (Result<PlatformOrchestratorResult_, Error>) -> ()
    
        public init(cb: @escaping (Result<PlatformOrchestratorResult_, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_all_available_subnet_orchestrators() async throws -> RustVec<Principal> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_all_available_subnet_orchestrators>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<Principal>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<Principal>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_all_available_subnet_orchestrators(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_all_available_subnet_orchestrators(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_all_available_subnet_orchestrators {
        var cb: (Result<RustVec<Principal>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<Principal>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_all_subnet_orchestrators() async throws -> RustVec<Principal> {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_all_subnet_orchestrators>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustVec<Principal>(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustVec<Principal>, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_all_subnet_orchestrators(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_all_subnet_orchestrators(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_all_subnet_orchestrators {
        var cb: (Result<RustVec<Principal>, Error>) -> ()
    
        public init(cb: @escaping (Result<RustVec<Principal>, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_subnet_last_upgrade_status() async throws -> CanisterUpgradeStatus {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_subnet_last_upgrade_status>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(CanisterUpgradeStatus(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<CanisterUpgradeStatus, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_subnet_last_upgrade_status(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_subnet_last_upgrade_status(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$get_subnet_last_upgrade_status {
        var cb: (Result<CanisterUpgradeStatus, Error>) -> ()
    
        public init(cb: @escaping (Result<CanisterUpgradeStatus, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func provision_subnet_orchestrator_canister(_ arg0: Principal) async throws -> PlatformOrchestratorResult1 {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$provision_subnet_orchestrator_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PlatformOrchestratorResult1(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PlatformOrchestratorResult1, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$provision_subnet_orchestrator_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$provision_subnet_orchestrator_canister(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$provision_subnet_orchestrator_canister {
        var cb: (Result<PlatformOrchestratorResult1, Error>) -> ()
    
        public init(cb: @escaping (Result<PlatformOrchestratorResult1, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func start_reclaiming_cycles_from_individual_canisters() async throws -> PlatformOrchestratorResult_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$start_reclaiming_cycles_from_individual_canisters>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PlatformOrchestratorResult_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PlatformOrchestratorResult_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$start_reclaiming_cycles_from_individual_canisters(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$start_reclaiming_cycles_from_individual_canisters(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$start_reclaiming_cycles_from_individual_canisters {
        var cb: (Result<PlatformOrchestratorResult_, Error>) -> ()
    
        public init(cb: @escaping (Result<PlatformOrchestratorResult_, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func start_reclaiming_cycles_from_subnet_orchestrator_canister() async throws -> RustString {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$start_reclaiming_cycles_from_subnet_orchestrator_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(RustString(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<RustString, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$start_reclaiming_cycles_from_subnet_orchestrator_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$start_reclaiming_cycles_from_subnet_orchestrator_canister(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$start_reclaiming_cycles_from_subnet_orchestrator_canister {
        var cb: (Result<RustString, Error>) -> ()
    
        public init(cb: @escaping (Result<RustString, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func stop_upgrades_for_individual_user_canisters() async throws -> PlatformOrchestratorResult_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$stop_upgrades_for_individual_user_canisters>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PlatformOrchestratorResult_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PlatformOrchestratorResult_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$stop_upgrades_for_individual_user_canisters(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$stop_upgrades_for_individual_user_canisters(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$stop_upgrades_for_individual_user_canisters {
        var cb: (Result<PlatformOrchestratorResult_, Error>) -> ()
    
        public init(cb: @escaping (Result<PlatformOrchestratorResult_, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func subnet_orchestrator_maxed_out() async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$subnet_orchestrator_maxed_out>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$subnet_orchestrator_maxed_out(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$subnet_orchestrator_maxed_out(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$subnet_orchestrator_maxed_out {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_profile_owner_for_individual_canisters() async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_profile_owner_for_individual_canisters>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_profile_owner_for_individual_canisters(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_profile_owner_for_individual_canisters(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$update_profile_owner_for_individual_canisters {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func upgrade_canister(_ arg0: UpgradeCanisterArg) async throws -> PlatformOrchestratorResult_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$upgrade_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PlatformOrchestratorResult_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PlatformOrchestratorResult_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$upgrade_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$upgrade_canister(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$upgrade_canister {
        var cb: (Result<PlatformOrchestratorResult_, Error>) -> ()
    
        public init(cb: @escaping (Result<PlatformOrchestratorResult_, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func upgrade_specific_individual_canister(_ arg0: Principal) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$upgrade_specific_individual_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$upgrade_specific_individual_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$upgrade_specific_individual_canister(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$upgrade_specific_individual_canister {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func upload_wasms(_ arg0: WasmType, _ arg1: ByteBuf) async throws -> PlatformOrchestratorResult_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$upload_wasms>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PlatformOrchestratorResult_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PlatformOrchestratorResult_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$upload_wasms(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$upload_wasms(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}(), {arg1.isOwned = false; return arg1.ptr;}())
        })
    }
    class CbWrapper$Service$upload_wasms {
        var cb: (Result<PlatformOrchestratorResult_, Error>) -> ()
    
        public init(cb: @escaping (Result<PlatformOrchestratorResult_, Error>) -> ()) {
            self.cb = cb
        }
    }
}













public class PostCacheResult_: PostCacheResult_RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostCacheResult_$_free(ptr)
        }
    }
}
public class PostCacheResult_RefMut: PostCacheResult_Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostCacheResult_Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostCacheResult_: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostCacheResult_$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostCacheResult_$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostCacheResult_) {
        __swift_bridge__$Vec_PostCacheResult_$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostCacheResult_$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostCacheResult_(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostCacheResult_Ref> {
        let pointer = __swift_bridge__$Vec_PostCacheResult_$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostCacheResult_Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostCacheResult_RefMut> {
        let pointer = __swift_bridge__$Vec_PostCacheResult_$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostCacheResult_RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostCacheResult_Ref> {
        UnsafePointer<PostCacheResult_Ref>(OpaquePointer(__swift_bridge__$Vec_PostCacheResult_$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostCacheResult_$len(vecPtr)
    }
}


public class TopPostsFetchError: TopPostsFetchErrorRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$TopPostsFetchError$_free(ptr)
        }
    }
}
public class TopPostsFetchErrorRefMut: TopPostsFetchErrorRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class TopPostsFetchErrorRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension TopPostsFetchError: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_TopPostsFetchError$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_TopPostsFetchError$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: TopPostsFetchError) {
        __swift_bridge__$Vec_TopPostsFetchError$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_TopPostsFetchError$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (TopPostsFetchError(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TopPostsFetchErrorRef> {
        let pointer = __swift_bridge__$Vec_TopPostsFetchError$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TopPostsFetchErrorRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<TopPostsFetchErrorRefMut> {
        let pointer = __swift_bridge__$Vec_TopPostsFetchError$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return TopPostsFetchErrorRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<TopPostsFetchErrorRef> {
        UnsafePointer<TopPostsFetchErrorRef>(OpaquePointer(__swift_bridge__$Vec_TopPostsFetchError$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_TopPostsFetchError$len(vecPtr)
    }
}


public class PostScoreIndexItemV1: PostScoreIndexItemV1RefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostScoreIndexItemV1$_free(ptr)
        }
    }
}
public class PostScoreIndexItemV1RefMut: PostScoreIndexItemV1Ref {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostScoreIndexItemV1Ref {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostScoreIndexItemV1: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostScoreIndexItemV1$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostScoreIndexItemV1$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostScoreIndexItemV1) {
        __swift_bridge__$Vec_PostScoreIndexItemV1$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostScoreIndexItemV1$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostScoreIndexItemV1(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostScoreIndexItemV1Ref> {
        let pointer = __swift_bridge__$Vec_PostScoreIndexItemV1$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostScoreIndexItemV1Ref(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostScoreIndexItemV1RefMut> {
        let pointer = __swift_bridge__$Vec_PostScoreIndexItemV1$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostScoreIndexItemV1RefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostScoreIndexItemV1Ref> {
        UnsafePointer<PostScoreIndexItemV1Ref>(OpaquePointer(__swift_bridge__$Vec_PostScoreIndexItemV1$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostScoreIndexItemV1$len(vecPtr)
    }
}




public class NsfwFilter: NsfwFilterRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$NsfwFilter$_free(ptr)
        }
    }
}
public class NsfwFilterRefMut: NsfwFilterRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class NsfwFilterRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension NsfwFilter: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_NsfwFilter$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_NsfwFilter$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: NsfwFilter) {
        __swift_bridge__$Vec_NsfwFilter$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_NsfwFilter$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (NsfwFilter(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NsfwFilterRef> {
        let pointer = __swift_bridge__$Vec_NsfwFilter$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NsfwFilterRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<NsfwFilterRefMut> {
        let pointer = __swift_bridge__$Vec_NsfwFilter$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return NsfwFilterRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<NsfwFilterRef> {
        UnsafePointer<NsfwFilterRef>(OpaquePointer(__swift_bridge__$Vec_NsfwFilter$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_NsfwFilter$len(vecPtr)
    }
}




public class PostCacheInitArgs: PostCacheInitArgsRefMut {
    var isOwned: Bool = true

    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }

    deinit {
        if isOwned {
            __swift_bridge__$PostCacheInitArgs$_free(ptr)
        }
    }
}
public class PostCacheInitArgsRefMut: PostCacheInitArgsRef {
    public override init(ptr: UnsafeMutableRawPointer) {
        super.init(ptr: ptr)
    }
}
public class PostCacheInitArgsRef {
    var ptr: UnsafeMutableRawPointer

    public init(ptr: UnsafeMutableRawPointer) {
        self.ptr = ptr
    }
}
extension PostCacheInitArgs: Vectorizable {
    public static func vecOfSelfNew() -> UnsafeMutableRawPointer {
        __swift_bridge__$Vec_PostCacheInitArgs$new()
    }

    public static func vecOfSelfFree(vecPtr: UnsafeMutableRawPointer) {
        __swift_bridge__$Vec_PostCacheInitArgs$drop(vecPtr)
    }

    public static func vecOfSelfPush(vecPtr: UnsafeMutableRawPointer, value: PostCacheInitArgs) {
        __swift_bridge__$Vec_PostCacheInitArgs$push(vecPtr, {value.isOwned = false; return value.ptr;}())
    }

    public static func vecOfSelfPop(vecPtr: UnsafeMutableRawPointer) -> Optional<Self> {
        let pointer = __swift_bridge__$Vec_PostCacheInitArgs$pop(vecPtr)
        if pointer == nil {
            return nil
        } else {
            return (PostCacheInitArgs(ptr: pointer!) as! Self)
        }
    }

    public static func vecOfSelfGet(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostCacheInitArgsRef> {
        let pointer = __swift_bridge__$Vec_PostCacheInitArgs$get(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostCacheInitArgsRef(ptr: pointer!)
        }
    }

    public static func vecOfSelfGetMut(vecPtr: UnsafeMutableRawPointer, index: UInt) -> Optional<PostCacheInitArgsRefMut> {
        let pointer = __swift_bridge__$Vec_PostCacheInitArgs$get_mut(vecPtr, index)
        if pointer == nil {
            return nil
        } else {
            return PostCacheInitArgsRefMut(ptr: pointer!)
        }
    }

    public static func vecOfSelfAsPtr(vecPtr: UnsafeMutableRawPointer) -> UnsafePointer<PostCacheInitArgsRef> {
        UnsafePointer<PostCacheInitArgsRef>(OpaquePointer(__swift_bridge__$Vec_PostCacheInitArgs$as_ptr(vecPtr)))
    }

    public static func vecOfSelfLen(vecPtr: UnsafeMutableRawPointer) -> UInt {
        __swift_bridge__$Vec_PostCacheInitArgs$len(vecPtr)
    }
}





extension ServiceRef {
    public func get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor(_ arg0: UInt64, _ arg1: UInt64, _ arg2: Optional<Bool>, _ arg3: Optional<PostStatus>, _ arg4: Optional<NsfwFilter>) async throws -> PostCacheResult_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PostCacheResult_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PostCacheResult_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor(wrapperPtr, onComplete, ptr, arg0, arg1, arg2.intoFfiRepr(), { if let val = arg3 { val.isOwned = false; return val.ptr } else { return nil } }(), { if let val = arg4 { val.isOwned = false; return val.ptr } else { return nil } }())
        })
    }
    class CbWrapper$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor {
        var cb: (Result<PostCacheResult_, Error>) -> ()
    
        public init(cb: @escaping (Result<PostCacheResult_, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor(_ arg0: UInt64, _ arg1: UInt64, _ arg2: Optional<Bool>, _ arg3: Optional<PostStatus>, _ arg4: Optional<NsfwFilter>) async throws -> PostCacheResult_ {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: __private__ResultPtrAndPtr) {
            let wrapper = Unmanaged<CbWrapper$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal.is_ok {
                wrapper.cb(.success(PostCacheResult_(ptr: rustFnRetVal.ok_or_err!)))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal.ok_or_err!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<PostCacheResult_, Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor(wrapperPtr, onComplete, ptr, arg0, arg1, arg2.intoFfiRepr(), { if let val = arg3 { val.isOwned = false; return val.ptr } else { return nil } }(), { if let val = arg4 { val.isOwned = false; return val.ptr } else { return nil } }())
        })
    }
    class CbWrapper$Service$get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor {
        var cb: (Result<PostCacheResult_, Error>) -> ()
    
        public init(cb: @escaping (Result<PostCacheResult_, Error>) -> ()) {
            self.cb = cb
        }
    }

    public func receive_top_home_feed_posts_from_publishing_canister(_ arg0: RustVec<PostScoreIndexItemV1>) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$receive_top_home_feed_posts_from_publishing_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$receive_top_home_feed_posts_from_publishing_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$receive_top_home_feed_posts_from_publishing_canister(wrapperPtr, onComplete, ptr, { let val = arg0; val.isOwned = false; return val.ptr }())
        })
    }
    class CbWrapper$Service$receive_top_home_feed_posts_from_publishing_canister {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func receive_top_hot_or_not_feed_posts_from_publishing_canister(_ arg0: RustVec<PostScoreIndexItemV1>) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$receive_top_hot_or_not_feed_posts_from_publishing_canister>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$receive_top_hot_or_not_feed_posts_from_publishing_canister(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$receive_top_hot_or_not_feed_posts_from_publishing_canister(wrapperPtr, onComplete, ptr, { let val = arg0; val.isOwned = false; return val.ptr }())
        })
    }
    class CbWrapper$Service$receive_top_hot_or_not_feed_posts_from_publishing_canister {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func remove_all_feed_entries() async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$remove_all_feed_entries>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$remove_all_feed_entries(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$remove_all_feed_entries(wrapperPtr, onComplete, ptr)
        })
    }
    class CbWrapper$Service$remove_all_feed_entries {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_post_home_feed(_ arg0: PostScoreIndexItemV1) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_post_home_feed>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_post_home_feed(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_post_home_feed(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_post_home_feed {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }

    public func update_post_hot_or_not_feed(_ arg0: PostScoreIndexItemV1) async throws -> () {
        func onComplete(cbWrapperPtr: UnsafeMutableRawPointer?, rustFnRetVal: UnsafeMutableRawPointer?) {
            let wrapper = Unmanaged<CbWrapper$Service$update_post_hot_or_not_feed>.fromOpaque(cbWrapperPtr!).takeRetainedValue()
            if rustFnRetVal == nil {
                wrapper.cb(.success(()))
            } else {
                wrapper.cb(.failure(AgentError(ptr: rustFnRetVal!)))
            }
        }

        return try await withCheckedThrowingContinuation({ (continuation: CheckedContinuation<(), Error>) in
            let callback = { rustFnRetVal in
                continuation.resume(with: rustFnRetVal)
            }

            let wrapper = CbWrapper$Service$update_post_hot_or_not_feed(cb: callback)
            let wrapperPtr = Unmanaged.passRetained(wrapper).toOpaque()

            __swift_bridge__$Service$update_post_hot_or_not_feed(wrapperPtr, onComplete, ptr, {arg0.isOwned = false; return arg0.ptr;}())
        })
    }
    class CbWrapper$Service$update_post_hot_or_not_feed {
        var cb: (Result<(), Error>) -> ()
    
        public init(cb: @escaping (Result<(), Error>) -> ()) {
            self.cb = cb
        }
    }
}


