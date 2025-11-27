/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.kremote.codegen.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object RpcClassId {
    val remoteAnnotation = ClassId(FqName("kotlinx.remote"), Name.identifier("Remote"))
    val remoteSerializableAnnotation = ClassId(FqName("kotlinx.remote.classes"), Name.identifier("RemoteSerializable"))
    val checkedTypeAnnotation = ClassId(FqName("kotlinx.rpc.annotations"), Name.identifier("CheckedTypeAnnotation"))

    val serializableAnnotation = ClassId(FqName("kotlinx.serialization"), Name.identifier("Serializable"))
    val kSerializer = ClassId(FqName("kotlinx.serialization"), Name.identifier("KSerializer"))
    val remoteSerializer = ClassId(FqName("kotlinx.remote.classes"), Name.identifier("RemoteSerializer"))
    val stubInterface = ClassId(FqName("kotlinx.remote.classes"), Name.identifier("Stub"))
    val serializationTransient = ClassId.topLevel(FqName("kotlinx.serialization.Transient"))

    val flow = ClassId(FqName("kotlinx.coroutines.flow"), Name.identifier("Flow"))
    val sharedFlow = ClassId(FqName("kotlinx.coroutines.flow"), Name.identifier("SharedFlow"))
    val stateFlow = ClassId(FqName("kotlinx.coroutines.flow"), Name.identifier("StateFlow"))
    val remoteContext = ClassId(FqName("kotlinx.rpc"), Name.identifier("RemoteContext"))
    val remoteInterface = ClassId(FqName("kotlinx.rpc"), Name.identifier("Remote"))
}

object RpcNames {
    val SERVICE_STUB_NAME: Name = Name.identifier("\$rpcServiceStub")
    val REMOTE_CLASS_STUB_NAME: Name = Name.identifier("RemoteClassStub")
    val REMOTE_CLASS_SERIALIZER_NAME: Name = Name.identifier("RemoteClassSerializer")
    val REMOTE_CLOSE_NAME: Name = Name.identifier("close")
    val REMOTE_SERIALIZER_STUB_NAME: Name = Name.identifier("createStub")
    val KSERIALIZER_SERIALIZE_NAME: Name = Name.identifier("serialize")
    val KSERIALIZER_DESERIALIZE_NAME: Name = Name.identifier("deserialize")
    val KSERIALIZER_DESCRIPTOR_NAME: Name = Name.identifier("descriptor")
}
