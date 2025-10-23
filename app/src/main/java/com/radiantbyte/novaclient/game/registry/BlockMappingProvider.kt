package com.radiantbyte.novaclient.game.registry

import android.content.Context

class BlockMappingProvider(context: Context) : MappingProvider<BlockMapping>(context) {
    override val assetPath: String = "mcpedata/blocks"

    override fun readMapping(version: Short): BlockMapping {
        return BlockMapping.read(context, version)
    }
}