package org.example

import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.bouncycastle.util.encoders.Hex
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun String.formatKey(): String = this.padStart(64, '0')

fun generatePublic(privateKeyHex: String): String? {
    return try {
        val publicKey = ECKey.fromPrivate(Hex.decode(privateKeyHex.formatKey())).publicKeyAsHex
        val pubKeyHash = Utils.sha256hash160(Hex.decode(publicKey))
        LegacyAddress.fromPubKeyHash(MainNetParams.get(), pubKeyHash).toString()
    } catch (_: Exception) {
        null
    }
}

fun percentage(size: BigInteger, percentage: String): BigInteger {
    val percent = BigDecimal(size).multiply(BigDecimal(percentage))
    return percent.toBigInteger()
}

// target Test
val target = "13zb1hQbWVsc2S7ZTZnP2G4undNNpdh5so"
val walletNum = 66
val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

fun main() {
    println(Runtime.getRuntime().availableProcessors()) // 8 processadores

    var min = BigInteger(wallets[walletNum].first, 16)
    var max = BigInteger(wallets[walletNum].second, 16)
    val size = max - min

    println("indexing...")
    executor.execute(Right(min)) // 0
    executor.execute(Left(max)) // 100

    executor.execute(Right(min + (percentage(size, "0.166666666666"))))
    executor.execute(Left(min + (percentage(size,  "0.833333333335"))))

    executor.execute(Right(min + (percentage(size, "0.333333333333"))))
    executor.execute(Left(min + (percentage(size,  "0.666666666668"))))

    executor.execute(Right(min + (percentage(size, "0.5"))))
    executor.execute(Left(min + (percentage(size, "0.500000000001"))))
    println("\nindexing done")

    thread {
        val z = BigInteger.ZERO
        var tp = BigInteger.ZERO
        var pc = BigDecimal.ZERO
        while (!executor.isShutdown && !find) {
            tp = tp + pesquisas
            pc = (pc * 100.toBigDecimal()) / size.toBigDecimal()
            print("\rM: ${Runtime.getRuntime().freeMemory()}, P/s: $pesquisas, TP: $tp, PC: $pc%")
            pesquisas = z
            Thread.sleep(1000)
        }
        println("\n shutdown")
        exitProcess(0)
    }
}

var pesquisas = BigInteger("0")
@Volatile var find = false

class Right(private var number: BigInteger) : Runnable {
    override fun run() {
        var pk: String?
        var num: String?
        while (true){
            pesquisas++
            num = number.toString(16)
            pk = generatePublic(num)
            if (pk != target) number++
            else break
        }
        find = true
        println("\nFind in number: $num")
        println("╚═> $pk")
        println("╚═> ${num!!.formatKey()}")
        executor.shutdownNow()
    }
}

class Left(private var number: BigInteger) : Runnable {
    override fun run() {
        var pk: String?
        var num: String?
        while (true){
            pesquisas++
            num = number.toString(16)
            pk = generatePublic(num)
            if (pk != target) number--
            else break
        }
        find = true
        println("\nFind in number: $num")
        println("╚═> $pk")
        println("╚═> ${num!!.formatKey()}")
        executor.shutdownNow()
    }
}
