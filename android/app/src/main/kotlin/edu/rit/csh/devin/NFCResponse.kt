package edu.rit.csh.devin

class NFCResponse(val payload: ByteArray, val status: UShort) {
    fun toBytes(): ByteArray {
        val response: ByteArray = payload.copyOf(payload.size + 2)
        response[payload.size] = this.status.rotateRight(8).toByte()
        response[payload.size + 1] = this.status.and(0xffU).toByte()
        return response
    }
}