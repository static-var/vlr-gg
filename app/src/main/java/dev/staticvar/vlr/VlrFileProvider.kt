/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr

import androidx.annotation.XmlRes
import androidx.core.content.FileProvider

class VlrFileProvider(@XmlRes val resourceId: Int = R.xml.file_path) : FileProvider(resourceId)
