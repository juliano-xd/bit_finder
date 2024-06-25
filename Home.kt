package org.example

import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.bouncycastle.util.encoders.Hex
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.security.SecureRandom
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.system.exitProcess

// config
private val target = "13zb1hQbWVsc2S7ZTZnP2G4undNNpdh5so"
private val min = BigInteger("20000000000000000", 16)
private val max = BigInteger("3ffffffffffffffff", 16)
private val workersQuant = 16 // base 2
private val progress = 2450000000L // total quant

fun main() {
    val size = (max - min)

    val percent = (1f / workersQuant.toFloat())
    print(percent)  // "0.015625"

    val workers = mutableListOf<Worker>()

    workers.add(Right(min, progress)) // needed
    for (index in 1..(workersQuant - 1)){// O, N, N, N, N, N, N, N, O
        val percentage = (percent * index).toString()
        workers.add(Left ((min + (percentage(size, percentage))), (progress/workersQuant))) // L +
        workers.add(Right((min + (percentage(size, percentage))), (progress/workersQuant))) // R +
    }
    workers.add(Left(max, progress)) // needed

    for (index in 0..(workersQuant-1)){
        workers.add(Rand(workers[index], workers[index+1]))
    }
    println("indexing total ${workers.size} workers...")
    for (worker in workers) executor.execute(worker)
    println("\nindexing done")

    thread{
        var seconds = 1L
        var max = 0L
        while (!executor.isShutdown && !find){
            val mps = quantity.toLong() / seconds++
            if (max < mps) max = mps
            print("\rMP/s: $mps, Max: $max, TP: ${quantity+progress.toBigInteger()}")
            Thread.sleep(1000)
        }
        println("\n shutdown")
        exitProcess(0)
    }
}
val executor = Executors.newFixedThreadPool(2 + ((workersQuant - 1 ) * 2) + (workersQuant - 1))

fun generatePublic(privateKeyHex: String): String? {
    val publicKey = ECKey.fromPrivate(Hex.decode(privateKeyHex.formatKey())).publicKeyAsHex
    val pubKeyHash = Utils.sha256hash160(Hex.decode(publicKey))
    return LegacyAddress.fromPubKeyHash(MainNetParams.get(), pubKeyHash).toString()
}

fun percentage(size: BigInteger, percentage: String): BigInteger {
    val percent = BigDecimal(size).multiply(BigDecimal(percentage))
    return percent.toBigInteger()
}

@Volatile var find = false

var quantity = BigInteger.ZERO

interface Worker: Runnable {
    var publicKey: String?
    var number: BigInteger
}
class Rand(val right: Worker,val left: Worker): Worker{
    override var publicKey: String? = null
    override var number = BigInteger.ZERO
    override fun run() {
        while (publicKey != target){
            number = randomBigInteger(right.number, left.number)
            publicKey = generatePublic(number.toString(16))
            quantity++
        }
        find = true
        println("\nRand Find in number: $number")
        println("╚═> $publicKey")
        println("╚═> ${number.formatKey()}")
        executor.shutdownNow()
    }

    fun randomBigInteger(min: BigInteger, max: BigInteger): BigInteger {
        val random = SecureRandom()
        val range = max.subtract(min)
        val randomBigInt = BigInteger(range.bitLength(), random)
        return randomBigInt.mod(range).add(min)
    }
}
class Right(num: BigInteger, progress: Long) : Worker {
    override var number = num + progress.toBigInteger()
    override var publicKey: String? = null
    override fun run() {
        while (publicKey != target) {
            publicKey = generatePublic(number++.toString(16))
            quantity++
        }
        find = true
        --number
        println("\nFind in number: $number")
        println("╚═> $publicKey")
        println("╚═> ${number.formatKey()}")
        executor.shutdownNow()
    }
}

class Left(num: BigInteger, progress: Long) : Worker {
    override var number = num - progress.toBigInteger()
    override var publicKey: String? = null
    override fun run() {
        while (publicKey != target) {
            publicKey = generatePublic(number--.toString(16))
            quantity++
        }
        find = true
        ++number
        println("\nFind in number: $number")
        println("╚═> $publicKey")
        println("╚═> ${number.formatKey()}")
        executor.shutdownNow()
    }
}


fun String.formatKey(): String = this.padStart(64, '0')

fun BigInteger.formatKey(): String = this.toString(16).padStart(64, '0')