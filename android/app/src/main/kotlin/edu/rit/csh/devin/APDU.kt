package edu.rit.csh.devin

class APDU(val classId: Byte, val instructionId: Byte, val p1: Byte, val p2: Byte, val data: ByteArray, val responseLength: UShort) {
    override fun equals(other: Any?): Boolean {
        if (other !is APDU) {
            return false
        }
        return classId == other.classId &&
                instructionId == other.instructionId &&
                p1 == other.p1 &&
                p2 == other.p2 &&
                data.contentEquals(
                    other.data
                ) &&
                responseLength == other.responseLength
    }

    override fun hashCode(): Int {
        var result: Int = this.classId.toInt()
        result = 31 * result + instructionId
        result = 31 * result + p1
        result = 31 * result + p2
        result = 31 * result + data.contentHashCode()
        result = 31 * result + responseLength.toInt()
        return result
    }

    constructor(data: ByteArray) : this(data[0], data[1], data[2], data[3], parseData(data).first, parseData(data).second)

    companion object {
        private fun parseData(data: ByteArray): Pair<ByteArray, UShort> {
            var index = 0
            var numBytes = 0.toUShort()
            if (data.size == 4) return Pair(byteArrayOf(), 0U)
            if (data.size == 5) return Pair(byteArrayOf(), data[4].toUShort())
            // [4] should be LC! :)
            if (data[4] > 1) {
                numBytes = data[4].toUShort()
                index = 5
            } else if (data[4] == 0.toByte()) {
                // Cringe as heck. Big(?) endian
                numBytes = data[5].toUShort().rotateLeft(8) or data[6].toUShort()
                index = 7
            }
            var responseLength = 0.toUShort()
            val responseLengthIndex = index + numBytes.toInt()
            if (responseLengthIndex < (data.size)-1) {
                responseLength = data[responseLengthIndex].toUShort().rotateLeft(8) or
                        data[responseLengthIndex+1].toUShort()
            } else if (responseLengthIndex < data.size) {
                responseLength = data[responseLengthIndex].toUShort()
            }
            return Pair(data.sliceArray(IntRange(index, responseLengthIndex - 1)), responseLength)
        }
    }
}
