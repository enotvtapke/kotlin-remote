/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object RemoteClassId {
    val remoteAnnotation = ClassId(FqName("kotlinx.remote"), Name.identifier("Remote"))
    val remoteSerializableAnnotation = ClassId(FqName("kotlinx.remote.classes"), Name.identifier("RemoteSerializable"))

    val remoteSerializer = ClassId(FqName("kotlinx.remote.classes"), Name.identifier("RemoteSerializer"))
    val stubInterface = ClassId(FqName("kotlinx.remote.classes"), Name.identifier("Stub"))
    val remoteContext = ClassId(FqName("kotlinx.remote"), Name.identifier("RemoteContext"))
}

object RemoteNames {
    val REMOTE_CLASS_STUB_NAME: Name = Name.identifier("RemoteClassStub")
    val REMOTE_CLASS_SERIALIZER_NAME: Name = Name.identifier("RemoteClassSerializer")
    val REMOTE_SERIALIZER_STUB_NAME: Name = Name.identifier("createStub")
}
