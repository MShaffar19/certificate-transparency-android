/*
 * Copyright 2018 Babylon Healthcare Services Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.certificatetransparency.ctlog.internal.logclient.model.network

import com.google.gson.annotations.SerializedName
import org.certificatetransparency.ctlog.exceptions.CertificateTransparencyException
import org.certificatetransparency.ctlog.internal.logclient.model.SignedTreeHead
import org.certificatetransparency.ctlog.internal.serialization.Deserializer
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.logclient.model.Version

/**
 * @property treeSize The size of the tree, in entries, in decimal.
 * @property timestamp The timestamp, in decimal.
 * @property sha256RootHash The Merkle Tree Hash of the tree, in base64.
 * @property treeHeadSignature A TreeHeadSignature for the above data.
 */
internal data class GetSthResponse(
    @SerializedName("tree_size") val treeSize: Long,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("sha256_root_hash") val sha256RootHash: String,
    @SerializedName("tree_head_signature") val treeHeadSignature: String
) {

    /**
     * Parses CT log's response for "get-sth" into a signed tree head object.
     *
     * @return A SignedTreeHead object.
     */
    fun toSignedTreeHead(): SignedTreeHead {
        requireNotNull(this) { "Sign Tree Head response from a CT log should not be null" }

        val treeSize = treeSize
        val timestamp = timestamp
        if (treeSize < 0 || timestamp < 0) {
            throw CertificateTransparencyException(
                "Bad response. Size of tree or timestamp cannot be a negative value. Log Tree size: $treeSize Timestamp: $timestamp"
            )
        }
        val base64Signature = treeHeadSignature
        val sha256RootHash = try {
            Base64.decode(sha256RootHash)
        } catch (e: Exception) {
            throw CertificateTransparencyException("Bad response. The root hash of the Merkle Hash Tree is invalid.")
        }

        if (sha256RootHash.size != 32) {
            throw CertificateTransparencyException(
                "Bad response. The root hash of the Merkle Hash Tree must be 32 bytes. The size of the root hash is ${sha256RootHash.size}"
            )
        }

        return SignedTreeHead(
            version = Version.V1,
            treeSize = treeSize,
            timestamp = timestamp,
            sha256RootHash = sha256RootHash,
            signature = Deserializer.parseDigitallySignedFromBinary(Base64.decode(base64Signature).inputStream())
        )
    }
}
